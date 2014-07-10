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
package org.jboss.aerogear.sync.diffsync.client;

import org.jboss.aerogear.sync.diffsync.ClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultBackupShadowDocument;
import org.jboss.aerogear.sync.diffsync.DefaultShadowDocument;
import org.jboss.aerogear.sync.diffsync.Edit;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;

import java.util.Set;

/**
 * The client side of the differential synchronization implementation.
 *
 * @param <T> The type of document that this implementation can handle.
 */
public class ClientSyncEngine<T> {

    private final ClientSynchronizer<T> clientSynchronizer;
    private final ClientDataStore<T> dataStore;

    public ClientSyncEngine(final ClientSynchronizer<T> clientSynchronizer, final ClientDataStore<T> dataStore) {
        this.clientSynchronizer = clientSynchronizer;
        this.dataStore = dataStore;
    }

    /**
     * Adds a new document to this sync engine.
     *
     * @param document the document to add.
     */
    public void addDocument(final ClientDocument<T> document) {
        saveDocument(document);
        saveBackupShadow(saveShadow(new DefaultShadowDocument<T>(0, 0, document)));
    }

    /**
     * Performs the client side of a differential sync.
     * <p>
     * When a client makes an update to it's document, it is first diffed against the shadow
     * document. The result of this is an {@link Edit} instance representing the changes.
     * There might be pending edits that represent edits that have not made it to the server
     * for some reason (for example packet drop). If a pending edit exits the contents (the diffs)
     * of the pending edit will be included in the returned Edits from this method.
     *
     * @param document the updated document.
     * @return {@link Edit} containing the edits for the changes in the document.
     */
    public Set<Edit> diff(final ClientDocument<T> document) {
        final ShadowDocument<T> shadow = getShadowDocument(document);
        final Edit newEdit = diff(document, shadow);
        //final Set<Edits> pendingEdits = getPendingEdits(document.clientId(), document.id());
        //final Edits mergedEdits = merge(pendingEdits, newEdits);
        saveEdits(newEdit);
        saveShadow(incrementClientVersion(shadow));
        return getPendingEdits(document.clientId(), document.id());
    }

    /**
     * Patches the client side shadow with updates from the server.
     *
     * @param edit the updates from the server.
     */
    public ClientDocument<T> patch(final Edit edit) {
        final ShadowDocument<T> patchedShadow = clientSynchronizer.patchShadow(edit,
                dataStore.getShadowDocument(edit.documentId(), edit.clientId()));
        saveShadow(incrementServerVersion(patchedShadow));
        saveBackupShadow(patchedShadow);
        clearPendingEdits(edit);
        return patchDocument(edit);
    }

    /*
     * Patches the clients document with the edits from the server.
     *
     * @param edits edits containing changes from the server.
     * @return {@code ClientDocument} the patched client document.
     */
    private ClientDocument<T> patchDocument(final Edit edit) {
        final ClientDocument<T> document = dataStore.getClientDocument(edit.clientId(), edit.documentId());
        final ClientDocument<T> patched = clientSynchronizer.patchDocument(edit, document);
        saveDocument(patched);
        return patched;
    }

    private ShadowDocument<T> getShadowDocument(final ClientDocument<T> clientDoc) {
        return dataStore.getShadowDocument(clientDoc.id(), clientDoc.clientId());
    }

    private Set<Edit> getPendingEdits(final String clientId, final String documentId) {
        return dataStore.getEdits(clientId, documentId);
    }

    private Edit diff(final ClientDocument<T> doc, final ShadowDocument<T> shadow) {
        return clientSynchronizer.diff(doc, shadow);
    }

    private void saveEdits(final Edit edit) {
        dataStore.saveEdits(edit);
    }

    private ShadowDocument<T> incrementClientVersion(final ShadowDocument<T> shadow) {
        final long clientVersion = shadow.clientVersion() + 1;
        return shadowDoc(shadow.serverVersion(), clientVersion, shadow.document());
    }

    private ShadowDocument<T> saveShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveShadowDocument(newShadow);
        return newShadow;
    }

    private ShadowDocument<T> shadowDoc(final long serverVersion, final long clientVersion, final ClientDocument<T> doc) {
        return new DefaultShadowDocument<T>(serverVersion, clientVersion, doc);
    }


    private ShadowDocument<T> incrementServerVersion(final ShadowDocument<T> shadow) {
        final long serverVersion = shadow.serverVersion() + 1;
        return shadowDoc(serverVersion, shadow.clientVersion(), shadow.document());
    }

    private void saveBackupShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveBackupShadowDocument(new DefaultBackupShadowDocument<T>(newShadow.clientVersion(), newShadow));
    }


    private void saveDocument(final ClientDocument<T> document) {
        dataStore.saveClientDocument(document);
    }

    private void clearPendingEdits(final Edit edit) {
        dataStore.removeEdits(edit);
    }

}
