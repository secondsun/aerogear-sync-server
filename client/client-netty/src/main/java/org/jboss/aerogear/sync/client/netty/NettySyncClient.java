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
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Edit;
import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.client.PatchListener;
import org.jboss.aerogear.sync.client.SyncClient;

import java.net.URI;
import java.net.URISyntaxException;

import static org.jboss.aerogear.sync.util.Arguments.checkNotNull;

/**
 * A Netty based WebSocket client for AeroGear Diff Sync Server.
 *
 * @param <T> The type of the Document that this client can handle
 * @param <S> The type of {@link Edit}s that this client can handle
 */
public final class NettySyncClient<T, S extends Edit<? extends Diff>> implements SyncClient<T, S> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettySyncClient.class);

    private final String host;
    private final int port;
    private final String path;
    private final URI uri;
    private final ClientSyncEngine<T, S> syncEngine;
    private final String subprotocols;
    private EventLoopGroup group;
    private Channel channel;

    private NettySyncClient(final Builder<T, S> builder) {
        host = checkNotNull(builder.host, "host must not be null");
        path = checkNotNull(builder.path, "path must not be null");
        syncEngine = checkNotNull(builder.engine, "engine must not be null");
        port = builder.port;
        uri = parseUri(builder.wss, host, port, path);
        subprotocols = builder.subprotocols;
        if (builder.listener != null) {
            syncEngine.addPatchListener(builder.listener);
        }
    }

    @Override
    public NettySyncClient<T, S> connect() throws InterruptedException {
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
                        handler,
                        syncClientHandler);
            }
        });

        channel = b.connect(host, port).sync().channel();
        handler.handshakeFuture().sync();
        logger.info("SyncClient connected to {}:{}", host, port);
        return this;
    }

    @Override
    public void addDocument(final ClientDocument<T> document) {
        syncEngine.addDocument(document);
        if (channel.isOpen()) {
            final String json = syncEngine.documentToJson(document);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    @Override
    public void diffAndSend(final ClientDocument<T> document) {
        final PatchMessage<S> patchMessage = syncEngine.diff(document);
        if (channel.isOpen()) {
            channel.writeAndFlush(new TextWebSocketFrame(patchMessage.asJson()));
        }
    }

    @Override
    public void disconnect() {
        channel.close();
        group.shutdownGracefully();
        logger.info("SyncClient disconnected");
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public void addPatchListener(final PatchListener<T> listener) {
        syncEngine.addPatchListener(listener);
    }

    @Override
    public void deletePatchListener(final PatchListener<T> listener) {
        syncEngine.removePatchListener(listener);
    }

    @Override
    public void deletePatchListeners() {
        syncEngine.removePatchListeners();
    }

    @Override
    public int countPatchListeners() {
        return syncEngine.countPatchListeners();
    }

    @Override
    public String clientId() {
        return "NettySyncClient";
    }

    private WebSocketClientHandler newWebSocketClientHandler() {
        return new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                subprotocols,
                false,
                new DefaultHttpHeaders()));
    }

    private static URI parseUri(final boolean wss, final String host, final int port, final String path) {
        try {
            return new URI(wss ? "wss" : "ws" + "://" + host + ':' + port + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static <T, S extends Edit<? extends Diff>> Builder<T, S> forHost(final String host) {
        return new Builder<T, S>(host);
    }
    
    public static class Builder<T, S extends Edit<? extends Diff>> {

        private final String host;
        private int port;
        private String path;
        private boolean wss;
        private String subprotocols;
        private ClientSyncEngine<T, S> engine;
        private PatchListener<T> listener;

        public Builder(final String host) {
            this.host = host;
        }

        public Builder<T, S> port(final int port) {
            this.port = port;
            return this;
        }

        public Builder<T, S> path(final String path) {
            this.path = path;
            return this;
        }

        public Builder<T, S> wss(final boolean wss) {
            this.wss = wss;
            return this;
        }

        public Builder<T, S> subprotocols(final String subprotocols) {
            this.subprotocols = subprotocols;
            return this;
        }

        public Builder<T, S> syncEngine(final ClientSyncEngine<T, S> engine) {
            this.engine = engine;
            return this;
        }

        public Builder<T, S> patchListener(final PatchListener<T> listener) {
            this.listener = listener;
            return this;
        }

        public NettySyncClient<T, S> build() {
            return new NettySyncClient<T, S>(this);
        }
    }

}
