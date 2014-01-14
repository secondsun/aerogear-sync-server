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
package org.jboss.aerogear.sync.ds;

/**
 * The client side of the differential synchronization implementation.
 *
 * @param <T> The type of document that this implementation can handle.
 */
public class ClientSyncEngine<T> {

    private final Synchronizer<T> synchronizer;
    private final DataStore<T> dataStore;

    public ClientSyncEngine(final Synchronizer<T> synchronizer, final DataStore<T> dataStore) {
        this.synchronizer = synchronizer;
        this.dataStore = dataStore;
    }

    /**
     * Adds a new document to this sync engine.
     *
     * @param document the document to add.
     */
    public void newSyncDocument(final ClientDocument<T> document) {
        saveDocument(document);
        saveBackupShadow(saveShadow(new DefaultShadowDocument<T>(0, 0, document)));
    }

    /**
     * Performs the client side of a differential sync.
     * <p>
     * When a client makes an update to it's document, it is first diffed against the shadow
     * document. The result of this is an {@link Edits} instance representing the changes.
     * There might be pending edits that represent edits that have not made it to the server
     * for some reason (for example packet drop). If a pending edit exits the contents (the diffs)
     * of the pending edit will be included in the returned Edits from this method.
     *
     * @param document the updated document.
     * @return {@link Edits} containing the edits for the changes in the document.
     */
    public Edits diff(final ClientDocument<T> document) {
        final ShadowDocument<T> shadow = getShadowDocument(document);
        final Edits newEdits = diff(document, shadow);
        final Edits pendingEdits = getPendingEdits(document.clientId(), document.id());
        final Edits mergedEdits = merge(pendingEdits, newEdits);
        saveEdits(mergedEdits, document);
        saveShadow(incrementClientVersion(shadow));
        return mergedEdits;
    }

    private ShadowDocument<T> getShadowDocument(final ClientDocument<T> clientDoc) {
        return dataStore.getShadowDocument(clientDoc.clientId(), clientDoc.id());
    }

    private Edits getPendingEdits(final String clientId, final String documentId) {
        return dataStore.getEdit(clientId, documentId);
    }

    private Edits diff(final ClientDocument<T> doc, final ShadowDocument<T> shadow) {
        return synchronizer.diff(doc, shadow);
    }

    private static Edits merge(final Edits pendingEdits, final Edits newEdits) {
        if (pendingEdits == null) {
            return newEdits;
        }
        pendingEdits.diffs().addAll(newEdits.diffs());
        return pendingEdits;
    }

    private void saveEdits(final Edits edits, final ClientDocument<T> document) {
        dataStore.saveEdits(edits, document);
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

    /**
     * Patches the client side shadow with updates from the server.
     *
     * @param edits the updates from the server.
     */
    public ShadowDocument<T> patchShadow(final Edits edits) {
        final ShadowDocument<T> patched = synchronizer.patchShadow(edits, dataStore.getShadowDocument(edits.clientId(), edits.documentId()));
        saveShadow(incrementServerVersion(patched));
        saveBackupShadow(patched);
        clearPendingEdits(edits);
        return patched;
    }

    private ShadowDocument<T> incrementServerVersion(final ShadowDocument<T> shadow) {
        final long serverVersion = shadow.serverVersion() + 1;
        return shadowDoc(serverVersion, shadow.clientVersion(), shadow.document());
    }

    private void saveBackupShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveBackupShadowDocument(new DefaultBackupShadowDocument<T>(newShadow.clientVersion(), newShadow));
    }

    public ClientDocument<T> patchDocument(final Edits edits) {
        final ClientDocument<T> document = dataStore.getDocument(edits.clientId(), edits.documentId());
        final ClientDocument<T> patched = synchronizer.patchDocument(edits, document);
        saveDocument(patched);
        return patched;
    }

    private void saveDocument(final ClientDocument<T> document) {
        dataStore.saveDocument(document);
    }

    private void clearPendingEdits(final Edits edits) {
        //TODO: clear out pending edits.
    }

}
