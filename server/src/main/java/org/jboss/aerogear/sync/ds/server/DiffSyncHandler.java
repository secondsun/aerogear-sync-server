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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.ds.DefaultDocument;
import org.jboss.aerogear.sync.ds.Edits;

public class DiffSyncHandler  extends SimpleChannelInboundHandler<TextWebSocketFrame> {

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
            responseCreated(ctx, "CREATED");
        } else if (msgType.equals("shadow")) {
            syncEngine.addShadow(json.get("docId").asText(), json.get("clientId").asText());
            responseCreated(ctx, "CREATED");
        } else if (msgType.equals("edits")) {
            final Edits edits = JsonMapper.fromJson(json.toString(), Edits.class);
            syncEngine.patchDocument(edits);
            responseCreated(ctx, "PATCHED");
        } else {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"result\": \"Unknown msgType '" + msgType + "'\"}"));
        }
    }

    private static final void responseCreated(final ChannelHandlerContext ctx, final String msg) {
        ctx.channel().writeAndFlush(new TextWebSocketFrame("{\"result\": \"" + msg + "\"}"));
    }
}
