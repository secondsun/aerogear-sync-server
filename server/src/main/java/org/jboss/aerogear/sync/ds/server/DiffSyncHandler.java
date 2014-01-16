/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.sync.ds.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.ds.DefaultDocument;
import org.jboss.aerogear.sync.ds.Edits;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class DiffSyncHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final ConcurrentHashMap<String, Set<ChannelHandlerContext>> clients =
            new ConcurrentHashMap<String, Set<ChannelHandlerContext>>();
    private final ServerSyncEngine<String> syncEngine;

    public DiffSyncHandler(final ServerSyncEngine<String> syncEngine) {
        this.syncEngine = syncEngine;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        final JsonNode json = JsonMapper.asJsonNode(frame.text());
        final String msgType = json.get("msgType").asText();
        if (msgType.equals("add")) {
            syncEngine.addDocument(new DefaultDocument<String>(json.get("docId").asText(), json.get("content").asText()));
            respond(ctx, "ADDED");
        } else if (msgType.equals("shadow")) {
            final String documentId = json.get("docId").asText();
            syncEngine.addShadow(documentId, json.get("clientId").asText());
            addListener(documentId, ctx);
            respond(ctx, "ADDED");
        } else if (msgType.equals("edits")) {
            final Edits clientEdits = JsonMapper.fromJson(json.toString(), Edits.class);
            final Edits edits = syncEngine.patch(clientEdits);
            respond(ctx, "PATCHED");
            notifyListeners(edits);
        } else if (msgType.equals("detach")) {
            // detach the client from a specific document.
        } else {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"result\": \"Unknown msgType '" + msgType + "'\"}"));
        }
    }

    private static void addListener(final String documentId, final ChannelHandlerContext ctx) {
        if (!clients.containsKey(documentId)) {
            final Set<ChannelHandlerContext> contexts = new HashSet<ChannelHandlerContext>();
            contexts.add(ctx);
            clients.put(documentId, contexts);
        } else {
            synchronized (clients) {
                final Set<ChannelHandlerContext> contexts = clients.get(documentId);
                contexts.add(ctx);
            }
        }
    }

    private void notifyListeners(final Edits edits) {
        final Set<ChannelHandlerContext> contexts = clients.get(edits.documentId());
        for (ChannelHandlerContext ctx : contexts) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(JsonMapper.toJson(edits)));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"result\": \"" + cause.getMessage() + "\"}"));
    }

    private static void respond(final ChannelHandlerContext ctx, final String msg) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"result\": \"" + msg + "\"}"));
    }
}
