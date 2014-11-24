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

import android.content.Context;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.jboss.aerogear.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.diffsync.client.DefaultClientSynchronizer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Observable;
import java.util.Observer;

/**
 * A Netty based WebSocket client that is able to handle differential synchronization edits.
 */
public final class DiffSyncClient<T> extends Observable {

    private final String host;
    private final int port;
    private final String path;
    private final URI uri;
    private final ClientSyncEngine<T> syncEngine;
    private final String subprotocols;
    private GoogleCloudMessaging gcm;
    
    private DiffSyncClient(final Builder builder) {
        host = builder.host;
        port = builder.port;
        path = builder.path;
        uri = builder.uri;
        subprotocols = builder.subprotocols;
        syncEngine = builder.engine;
        if (builder.observer != null) {
            syncEngine.addObserver(builder.observer);
        }
        gcm = GoogleCloudMessaging.getInstance((Context)null);
    }
    
    public DiffSyncClient<T> connect() throws InterruptedException {
        final DiffSyncClientHandler diffSyncClientHandler = new DiffSyncClientHandler(syncEngine);
        final GCMClientHandler handler = newWebSocketClientHandler();
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
                        diffSyncClientHandler);
            }
        });

        channel = b.connect(host, port).sync().channel();
        handler.handshakeFuture().sync();
        System.out.println("SyncClient connected to " + host + ':' + port);
        return this;
    }

    public void addDocument(final ClientDocument<T> document) {
        syncEngine.addDocument(document);
        if (channel.isOpen()) {
            final ObjectNode docMsg = message("add");
            docMsg.put("msgType", "add");
            docMsg.put("id", document.id());
            docMsg.put("clientId", document.clientId());
            docMsg.put("content", document.content().toString());
            channel.writeAndFlush(new TextWebSocketFrame(docMsg.toString()));
        } else {
            //TODO: store the messages in a queue. 
        }
    }
    
    public void diffAndSend(final ClientDocument<T> document) {
        final PatchMessage patchMessage = syncEngine.diff(document);
        if (channel.isOpen()) {
            channel.writeAndFlush(new TextWebSocketFrame(JsonMapper.toJson(patchMessage)));
        } else {
            //TODO: store edits in a queue. 
        }
    }
    
    private static ObjectNode message(final String type) {
        final ObjectNode jsonNode = JsonMapper.newObjectNode();
        jsonNode.put("msgType", type);
        return jsonNode;
    }
    
    public static <T> Builder<T> forHost(final String host) {
        return new Builder<T>(host);
    }
    
    public static class Builder<T> {
        
        private final String host;
        private int port;
        private String path;
        private boolean wss;
        private URI uri;
        private String subprotocols;
        private ClientSyncEngine<T> engine;
        private Observer observer;
        
        public Builder(final String host) {
            this.host = host;
        }
        
        public Builder<T> port(final int port) {
            this.port = port;
            return this;
        }
        
        public Builder<T> path(final String path) {
            this.path = path;
            return this;
        }
        
        public Builder<T> wss(final boolean wss) {
            this.wss = wss;
            return this;
        }
        
        public Builder<T> subprotocols(final String subprotocols) {
            this.subprotocols = subprotocols;
            return this;
        }
        
        public Builder<T> syncEngine(final ClientSyncEngine<T> engine) {
            this.engine = engine;
            return this;
        }
        
        public Builder<T> observer(final Observer observer) {
            this.observer = observer;
            return this;
        }
        
        public DiffSyncClient<T> build() {
            if (engine == null) {
                engine = new ClientSyncEngine(new DefaultClientSynchronizer(), new ClientInMemoryDataStore());
            }
            uri = parseUri(this);
            return new DiffSyncClient<T>(this);
        }
    
        private URI parseUri(final Builder<T> b) {
            try {
                return new URI(b.wss ? "wss" : "ws" + "://" + b.host + ':' + b.port + b.path);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        
    }
    
}
