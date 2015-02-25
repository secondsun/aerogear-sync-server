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
package org.jboss.aerogear.sync.client;

import org.jboss.aerogear.sync.ClientDocument;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.Edit;

/**
 *
 * @param <T> The type of the Document that this client can handle
 * @param <S> The type of {@link Edit}s that this client can handle
 */
public interface SyncClient<T, S extends Edit<? extends Diff>> {

    /**
     * Connects this client to a Sync Server.
     *
     * @return {@code SyncClient} to allow method chaining.
     * @throws Exception if an occurs while connecting to the sync server
     */
    SyncClient<T, S> connect() throws Exception;

    /**
     * Determines whether the client is currently connected to the sync server.
     *
     * @return {@code true} if the client is currently connected.
     */
    boolean isConnected();

    /**
     * Disconnect from the sync server.
     */
    void disconnect();

    /**
     * Returns the client identifier.
     *
     * @return {@code String} the client identifier
     */
    String clientId();

    /**
     * Adds a {@link ClientDocument} to this SyncClient.
     * @param document the document to add to the SyncClient
     */
    void addDocument(ClientDocument<T> document);

    /**
     * Diff the specified {@link ClientDocument} and send all edits to the sync server
     * @param document the updates made by the client
     */
    void diffAndSend(ClientDocument<T> document);

    /**
     * Add an {@link PatchListener} that will be notified when a patch has been perfomed.
     *
     * @param patchListener the listener to add.
     */
    void addPatchListener(PatchListener<T> patchListener);

    /**
     * Removes the specified {@link PatchListener} from the list of listeners.
     *
     * @param patchListener the listener to delete from the list of listeners.
     */
    void deletePatchListener(PatchListener<T> patchListener);

    /**
     * Deletes all {@link PatchListener}s.
     */
    void deletePatchListeners();

    /**
     * Returns the number of currently registered {@link PatchListener}s
     *
     * @return {@code int} the number of listeners currently registered.
     */
    int countPatchListeners();

}
