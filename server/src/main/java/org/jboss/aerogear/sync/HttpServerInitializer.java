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
package org.jboss.aerogear.sync;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eclipse.jetty.npn.NextProtoNego;
import org.jboss.aerogear.sync.rest.DefaultRestProcessor;
import org.jboss.aerogear.sync.rest.RestChannelHandler;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final CorsConfig corsConfig;
    private final SyncManager syncManager;

    public HttpServerInitializer(final CorsConfig corsConfig, final SyncManager syncManager) {
        this.corsConfig = corsConfig;
        this.syncManager = syncManager;
    }

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        final SSLEngine engine = SslServerContext.sslContext("/sync.keystore", "syncstore").createSSLEngine();
        /*
        for (String c : engine.getEnabledCipherSuites()) {
            System.out.println(c);
        }
        */
        engine.setUseClientMode(false);
        //engine.setEnabledCipherSuites(new String[] {"SSL_RSA_WITH_RC4_128_MD5"});
        //engine.setWantClientAuth(false);
        //engine.setNeedClientAuth(false);
        final SslHandler sslHandler = new SslHandler(engine);
        sslHandler.setHandshakeTimeout(1, TimeUnit.HOURS);
        sslHandler.setCloseNotifyTimeout(1, TimeUnit.HOURS);
        //sslHandler.setSingleDecode(true);
        pipeline.addLast("ssl", sslHandler);
        sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
                System.out.println("----------> Handshake outcome: " + future.isSuccess());
            }
        });

        sslHandler.sslCloseFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
            System.out.println("----------> SslClose Cause: " + future.cause());

            }
        });
        sslHandler.sslCloseFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
                System.out.println("----------> Close Cause: " + future.cause());
            }
        });

        // Setup NextProtoNego with our server provider
        NextProtoNego.put(engine, new SpdyServerProvider());
        NextProtoNego.debug = true;


        // Negotiates with the browser if SPDY or HTTP is going to be used
        pipeline.addLast("handler", new SpdyOrHttpHandler(new DefaultRestProcessor(syncManager), new CorsHandler(corsConfig)));
    }
}
