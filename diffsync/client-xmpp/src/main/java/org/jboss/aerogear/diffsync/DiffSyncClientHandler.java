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
package org.jboss.aerogear.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.diffsync.server.MessageType;


public class DiffSyncClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiffSyncClientHandler.class);

    private final ClientSyncEngine<?> syncEngine;

    public DiffSyncClientHandler(final ClientSyncEngine<?> syncEngine) {
        this.syncEngine = syncEngine;
    }

    protected void messageReceived(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            logger.debug("Received closeFrame");
            ctx.close();
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            logger.info("TextWebSocketFrame: " + ((TextWebSocketFrame)frame).text());
            final JsonNode json = JsonMapper.asJsonNode(((TextWebSocketFrame) frame).text());
            logger.info("json: " + json);
            switch (MessageType.from(json.get("msgType").asText())) {
            case PATCH:
                final PatchMessage serverPatchMessage = JsonMapper.fromJson(json.toString(), DefaultPatchMessage.class);
                logger.info("Edits: " + serverPatchMessage);
                patch(serverPatchMessage);
                break;
            case UNKNOWN:
                unknownMessageType(ctx, json);
                break;
            }
        } else {
            ctx.fireChannelRead(frame);
        }
    }

    private void patch(final PatchMessage clientEdit) {
        syncEngine.patch(clientEdit);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Caught exception", cause);
    }

    private static void unknownMessageType(final ChannelHandlerContext ctx, final JsonNode json) {
        ctx.channel().writeAndFlush(textFrame("{\"result\": \"Unknown msgType '" + json.get("msgType").asText() + "'\"}"));
    }

    private static TextWebSocketFrame textFrame(final String text) {
        return new TextWebSocketFrame(text);
    }

}
