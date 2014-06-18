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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsConfig;
import org.jboss.aerogear.sync.datastore.CouchDBSyncDataStore;

public final class RestServer {

    public static void main(final String args[]) throws InterruptedException {
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            final CorsConfig corsConfig = CorsConfig.withOrigin("http://localhost")
                    .allowNullOrigin()
                    .allowCredentials()
                    .allowedRequestMethods(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE)
                    .allowedRequestHeaders("Content-Type")
                    .build();
            b.childHandler(new HttpServerInitializer(corsConfig, new CouchDBSyncDataStore("http://127.0.0.1:5984", "sync-test")));

            System.out.println("Binding to localhost [8080]");
            b.bind("localhost", 8080).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
