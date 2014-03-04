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
package org.jboss.aerogear.sync.rest;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

/**
 * Handler that handles RESTful HTTP requests and to the RestProcessor
 */
public class RestChannelHandler extends ChannelHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(RestChannelHandler.class);

    private final RestProcessor restProcessor;

    public RestChannelHandler(final RestProcessor restProcessor) {
        if (restProcessor == null) {
            throw new NullPointerException("'restProcessor' must not be null.");
        }
        this.restProcessor = restProcessor;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final HttpRequest request = (HttpRequest) msg;
            final HttpMethod method = request.getMethod();
            final HttpResponse response;
            logger.info("HTTP Method: " + method);
            if (method.equals(GET)) {
                response = restProcessor.processGet(request, ctx);
            } else if (method.equals(PUT)) {
                response = restProcessor.processPut(request, ctx);
            } else if (method.equals(POST)) {
                response = restProcessor.processPost(request, ctx);
            } else if (method.equals(DELETE)) {
                response = restProcessor.processDelete(request, ctx);
            } else {
                throw new IllegalStateException("Method not supported");
            }

            boolean keepAlive = isKeepAlive(request);
            if (!keepAlive) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /*
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }
    */

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();;
    }
}
