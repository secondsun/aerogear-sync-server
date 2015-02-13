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

import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Edit;
import org.jboss.aerogear.sync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffmatchpatch.client.DiffMatchPatchClientSynchronizer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Observer;

/**
 * A Netty based WebSocket client for AeroGear Diff Sync Server.
 *
 * @param <T> The type of the Document that this client can handle
 * @param <S> The type of {@link Edit}s that this client can handle
 */
public final class SyncClient<T, S extends Edit<? extends Diff>> extends AbstractSyncClient {

    private SyncClient(final Builder<T, S> builder) {
        super(builder);
    }
    
    public static <T, S extends Edit<? extends Diff>> Builder<T, S> forHost(final String host) {
        return new Builder<T, S>(host);
    }
    
    public static class Builder<T, S extends Edit<? extends Diff>> extends AbstractSyncClient.Builder {
        
        public Builder(final String host) {
            super(host);
        }
        
        public SyncClient<T, S> build() {
            if (engine == null) {
                engine = new ClientSyncEngine(new DiffMatchPatchClientSynchronizer(), new ClientInMemoryDataStore());
            }
            uri = parseUri(this);
            return new SyncClient<T, S>(this);
        }
    
        private URI parseUri(final Builder<T, S> b) {
            try {
                return new URI(b.wss ? "wss" : "ws" + "://" + b.host + ':' + b.port + b.path);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
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
        
        public Builder<T, S> observer(final Observer observer) {
            this.observer = observer;
            return this;
        }
        
    }
    
}
