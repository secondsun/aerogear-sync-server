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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslHandler;
import org.eclipse.jetty.npn.NextProtoNego;
import org.jboss.aerogear.sync.SyncManager;

import javax.net.ssl.SSLEngine;

public class TestHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final CorsConfig corsConfig;
    private final SyncManager syncManager;
    private static final int MAX_CONTENT_LENGTH = 1024 * 100;

    public TestHttpServerInitializer(final CorsConfig corsConfig, final SyncManager syncManager) {
        this.corsConfig = corsConfig;
        this.syncManager = syncManager;
    }

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("httpRquestDecoder", new HttpRequestDecoder());
        pipeline.addLast("httpResponseEncoder", new HttpResponseEncoder());
        pipeline.addLast("httpChunkAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        pipeline.addLast("corsHandler", new CorsHandler(corsConfig));
        pipeline.addLast("httpRequestHandler", new RestChannelHandler(new DefaultRestProcessor(syncManager)));
    }
}
