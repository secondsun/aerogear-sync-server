/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.aerogear.sync;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.spdy.SpdyFrameCodec;
import io.netty.handler.codec.spdy.SpdyHttpDecoder;
import io.netty.handler.codec.spdy.SpdyHttpEncoder;
import io.netty.handler.codec.spdy.SpdyHttpResponseStreamIdHandler;
import io.netty.handler.codec.spdy.SpdyOrHttpChooser;
import io.netty.handler.codec.spdy.SpdySessionHandler;
import io.netty.handler.codec.spdy.SpdyVersion;
import org.eclipse.jetty.npn.NextProtoNego;

import javax.net.ssl.SSLEngine;
import java.util.logging.Logger;

/**
 * Negotiates with the browser if SPDY or HTTP is going to be used. Once decided, the Netty pipeline is setup with
 * the correct handlers for the selected protocol.
 */
public class SpdyOrHttpHandler extends SpdyOrHttpChooser {

    private static final Logger logger = Logger.getLogger( SpdyOrHttpHandler.class.getName());
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final RestProcessor restProcessor;
    private final CorsHandler corsHandler;
    private final int maxSpdyContentLength;
    private final int maxHttpContentLength;

    public SpdyOrHttpHandler(final RestProcessor restProcessor, final CorsHandler corsHandler) {
        this(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH, restProcessor, corsHandler);
    }

    public SpdyOrHttpHandler(int maxSpdyContentLength, int maxHttpContentLength,
                             final RestProcessor restProcessor,
                             final CorsHandler corsHandler) {
        super(maxSpdyContentLength, maxHttpContentLength);
        this.maxSpdyContentLength = maxSpdyContentLength;
        this.maxHttpContentLength = maxHttpContentLength;
        this.restProcessor = restProcessor;
        this.corsHandler = corsHandler;
    }

    @Override
    protected SelectedProtocol getProtocol(SSLEngine engine) {
        SpdyServerProvider provider = (SpdyServerProvider) NextProtoNego.get(engine);
        SelectedProtocol selectedProtocol = provider.getSelectedProtocol();

        logger.info("Selected Protocol is " + selectedProtocol);
        return selectedProtocol;
    }

    /**
     * Add all {@link ChannelHandler}'s that are needed for SPDY with the given version.
     */
    protected void addSpdyHandlers(ChannelHandlerContext ctx, SpdyVersion version) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast("spdyFrameCodec", new SpdyFrameCodec(version));
        pipeline.addLast("spdySessionHandler", new SpdySessionHandler(version, true));
        pipeline.addLast("spdyHttpEncoder", new SpdyHttpEncoder(version));
        pipeline.addLast("spdyHttpDecoder", new SpdyHttpDecoder(version, maxSpdyContentLength));
        pipeline.addLast("spdyStreamIdHandler", new SpdyHttpResponseStreamIdHandler());
        pipeline.addLast("corsHandler", corsHandler);
        pipeline.addLast("httpRequestHandler", createHttpRequestHandlerForSpdy());
    }

    /**
     * Add all {@link ChannelHandler}'s that are needed for HTTP.
     */
    protected void addHttpHandlers(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast("httpRquestDecoder", new HttpRequestDecoder());
        pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
        pipeline.addLast("httpChunkAggregator", new HttpObjectAggregator(maxHttpContentLength));
        pipeline.addLast("corsHandler", corsHandler);
        pipeline.addLast("httpRequestHandler", new RestChannelHandler(restProcessor));
    }

    @Override
    protected ChannelHandler createHttpRequestHandlerForHttp() {
        return new RestChannelHandler(restProcessor);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Caught exception in SpdyOrHttpHandler:");
        cause.printStackTrace();;
    }
}
