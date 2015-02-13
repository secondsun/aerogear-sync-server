/*
 * Copyright 2015 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.sync.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import java.net.URI;
import java.util.Observable;
import java.util.Observer;
import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Edit;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.client.ClientSyncEngine;

/**
 * A Netty based WebSocket client for AeroGear Diff Sync Server.
 *
 * @param <T> The type of the Document that this client can handle
 * @param <S> The type of {@link Edit}s that this client can handle
 */
public abstract class AbstractSyncClient<T, S extends Edit<? extends Diff>> extends Observable { 

    protected final String host;
    protected final int port;
    protected final String path;
    protected final URI uri;
    protected final ClientSyncEngine<T, S> syncEngine;
    protected final String subprotocols;
    protected EventLoopGroup group;
    protected Channel channel;

    protected AbstractSyncClient(final Builder<T, S> builder) {
        host = builder.host;
        port = builder.port;
        path = builder.path;
        uri = builder.uri;
        subprotocols = builder.subprotocols;
        syncEngine = builder.engine;
        if (builder.observer != null) {
            syncEngine.addObserver(builder.observer);
        }
    }
    
    public AbstractSyncClient<T, S> connect() throws InterruptedException {
        final SyncClientHandler<T, S> syncClientHandler = new SyncClientHandler<T, S>(syncEngine);
        final WebSocketClientHandler handler = newWebSocketClientHandler();
        final Bootstrap b = new Bootstrap();
        group = new NioEventLoopGroup();
        b.group(group).channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                p.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        new WebSocketClientCompressionHandler(),
                        handler,
                        syncClientHandler);
            }
        });

        channel = b.connect(host, port).sync().channel();
        handler.handshakeFuture().sync();
        System.out.println("SyncClient connected to " + host + ':' + port);
        return this;
    }

    private WebSocketClientHandler newWebSocketClientHandler() {
        return new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(
                uri, 
                WebSocketVersion.V13, 
                subprotocols, 
                false, 
                new DefaultHttpHeaders()));
    }

    public void addDocument(final ClientDocument<T> document) {
        syncEngine.addDocument(document);
        if (channel.isOpen()) {
            final String json = syncEngine.documentToJson(document);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        } else {
            //TODO: store the messages in a queue. 
        }
    }
    
    public void diffAndSend(final ClientDocument<T> document) {
        final PatchMessage<S> patchMessage = syncEngine.diff(document);
        if (channel.isOpen()) {
            channel.writeAndFlush(new TextWebSocketFrame(patchMessage.asJson()));
        } else {
            //TODO: store edits in a queue. 
        }
    }
    
    public void disconnect() {
        channel.close();
        group.shutdownGracefully();
        System.out.println("SyncClient disconnected");
    }
    
    
    public abstract static class Builder<T, S extends Edit<? extends Diff>> {
        
        protected final String host;
        protected int port;
        protected String path;
        protected boolean wss;
        protected URI uri;
        protected String subprotocols;
        protected ClientSyncEngine<T, S> engine;
        protected Observer observer;
        
        public Builder(final String host) {
            this.host = host;
        }
                
    }
    
}
