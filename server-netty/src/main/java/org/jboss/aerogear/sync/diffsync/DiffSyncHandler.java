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
package org.jboss.aerogear.sync.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.diffsync.server.MessageType;
import org.jboss.aerogear.sync.diffsync.server.ServerSyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class DiffSyncHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(DiffSyncHandler.class);

    private static final ConcurrentHashMap<String, Set<Client>> clients =
            new ConcurrentHashMap<String, Set<Client>>();
    private final ServerSyncEngine<String> syncEngine;

    public DiffSyncHandler(final ServerSyncEngine<String> syncEngine) {
        this.syncEngine = syncEngine;
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            logger.debug("Received closeFrame");
            ctx.close();
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            final JsonNode json = JsonMapper.asJsonNode(((TextWebSocketFrame) frame).text());
            logger.debug(json.toString());
            switch (MessageType.from(json.get("msgType").asText())) {
            case ADD:
                final Document<String> doc = documentFromJson(json);
                final String clientId = json.get("clientId").asText();
                addDocument(doc, clientId);
                addClientListener(doc.id(), clientId, ctx);
                respond(ctx, "ADDED");
                break;
            case PATCH:
                final Edits clientEdits = JsonMapper.fromJson(json.toString(), Edits.class);
                patch(clientEdits);
                respond(ctx, "PATCHED");
                notifyClientListeners(clientEdits.clientId(), clientEdits.documentId());
                break;
            case DETACH:
                // detach the client from a specific document.
                break;
            case UNKNOWN:
                unknownMessageType(ctx, json);
                break;
            }
        } else {
            ctx.fireChannelRead(frame);
        }
    }

    private void addDocument(final Document<String> document, final String clientId) {
        syncEngine.addDocument(document, clientId);
    }

    private void patch(final Edits clientEdits) {
        syncEngine.patch(clientEdits);
    }

    private static Document<String> documentFromJson(final JsonNode json) {
        return new DefaultDocument<String>(json.get("id").asText(), json.get("content").asText());
    }

    private Edits diff(final String clientId, final String documentId) {
        return syncEngine.diff(clientId, documentId);
    }

    private static void addClientListener(final String documentId, final String clientId, final ChannelHandlerContext ctx) {
        final Set<Client> newClient = Collections.newSetFromMap(new ConcurrentHashMap<Client, Boolean>());
        newClient.add(new Client(clientId, ctx));
        while(true) {
            final Set<Client> currentClients = clients.get(documentId);
            if (currentClients == null) {
                final Set<Client> previous = clients.putIfAbsent(documentId, newClient);
                if (previous != null) {
                    newClient.addAll(previous);
                    if (clients.replace(documentId, previous, newClient)) {
                        break;
                    }
                }
            } else {
                newClient.addAll(currentClients);
                if (clients.replace(documentId, currentClients, newClient)) {
                    break;
                }
            }
        }
    }

    private void notifyClientListeners(final String clientId, final String documentId) {
        for (Client client : clients.get(documentId)) {
            if (!client.id().equals(clientId)) {
                //TODO: this should be done async and not block the io thread!
                final Edits edits  = diff(client.id(), documentId);
                client.ctx().channel().writeAndFlush(textFrame(JsonMapper.toJson(edits)));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Caught exception", cause);
        ctx.channel().writeAndFlush(textFrame("{\"result\": \"" + cause.getMessage() + "\"}"));
    }

    private static void respond(final ChannelHandlerContext ctx, final String msg) {
        final TextWebSocketFrame textWebSocketFrame = textFrame("{\"result\": \"" + msg + "\"}");
        logger.info("Responding: " + textWebSocketFrame.text());
        ctx.channel().writeAndFlush(textWebSocketFrame);
    }

    private static void unknownMessageType(final ChannelHandlerContext ctx, final JsonNode json) {
        ctx.channel().writeAndFlush(textFrame("{\"result\": \"Unknown msgType '" + json.get("msgType").asText() + "'\"}"));
    }

    private static TextWebSocketFrame textFrame(final String text) {
        return new TextWebSocketFrame(text);
    }

    private static class Client {

        private final String id;
        private final ChannelHandlerContext ctx;

        Client(final String clientId, final ChannelHandlerContext ctx) {
            id = clientId;
            this.ctx = ctx;
        }

        public String id() {
            return id;
        }

        public ChannelHandlerContext ctx() {
            return ctx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Client client = (Client) o;

            if (!id.equals(client.id)) {
                return false;
            }

            return !ctx.equals(client.ctx);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + ctx.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Client[id=" + id + ", ctx=" + ctx + ']';
        }
    }
}
