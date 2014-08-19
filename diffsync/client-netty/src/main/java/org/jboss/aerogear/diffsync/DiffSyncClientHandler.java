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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.aerogear.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.diffsync.server.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiffSyncClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(DiffSyncClientHandler.class);

    private final ClientSyncEngine<?> syncEngine;

    public DiffSyncClientHandler(final ClientSyncEngine<?> syncEngine) {
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
            logger.info("TextWebSocketFrame: " + ((TextWebSocketFrame)frame).text());
            final JsonNode json = JsonMapper.asJsonNode(((TextWebSocketFrame) frame).text());
            logger.info("json: " + json);
            switch (MessageType.from(json.get("msgType").asText())) {
            case PATCH:
                final Edits serverEdits = JsonMapper.fromJson(json.toString(), DefaultEdits.class);
                logger.info("Edits: " + serverEdits);
                patch(serverEdits);
                break;
            case UNKNOWN:
                unknownMessageType(ctx, json);
                break;
            }
        } else {
            ctx.fireChannelRead(frame);
        }
    }

    private void patch(final Edits clientEdit) {
        syncEngine.patch(clientEdit);
    }

    @Override
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
