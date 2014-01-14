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
    public void newSyncDocument(final Document<T> document) {
        saveDocument(document);
        saveBackupShadow(saveShadow(new DefaultShadowDocument<T>(0, 0, document)));
    }

    /**
     * Performs the client side of a differential sync.
     *
     * @param document the updated document.
     * @return {@link Edits} containing the edits for the changes in the document.
     */
    public Edits clientDiff(final Document<T> document) {
        final ShadowDocument<T> shadow = getShadowDocument(document.id());
        final Edits edits = diff(document, shadow);
        saveEdits(edits, document.id());
        saveShadow(incrementClientVersion(shadow));
        return edits;
    }

    private ShadowDocument<T> getShadowDocument(final String documentId) {
        return dataStore.getShadowDocument(documentId);
    }

    private Edits diff(final Document<T> doc, final ShadowDocument<T> shadow) {
        final Edits edits = synchronizer.diff(doc, shadow);
        final Edits pendingEdits = dataStore.getEdit(doc.id());
        if (pendingEdits != null) {
            pendingEdits.diffs().addAll(edits.diffs());
            return pendingEdits;
        }
        return edits;
    }

    private void saveEdits(final Edits edits, final String documentId) {
        dataStore.saveEdits(edits, documentId);
    }

    private ShadowDocument<T> incrementClientVersion(final ShadowDocument<T> shadow) {
        final long clientVersion = shadow.clientVersion() + 1;
        return shadowDoc(shadow.serverVersion(), clientVersion, shadow.document());
    }

    private ShadowDocument<T> saveShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveShadowDocument(newShadow);
        return newShadow;
    }

    private ShadowDocument<T> shadowDoc(final long serverVersion, final long clientVersion, final Document<T> doc) {
        return new DefaultShadowDocument<T>(serverVersion, clientVersion, doc);
    }

    /**
     * Patches the client side shadow with updates from the server.
     *
     * @param edits the updates from the server.
     */
    public ShadowDocument<T> patchShadow(final Edits edits) {
        final ShadowDocument<T> patched = synchronizer.patchShadow(edits, dataStore.getShadowDocument(edits.id()));
        saveShadow(incrementServerVersion(patched));
        saveBackupShadow(patched);
        return patched;
    }

    private ShadowDocument<T> incrementServerVersion(final ShadowDocument<T> shadow) {
        final long serverVersion = shadow.serverVersion() + 1;
        return shadowDoc(serverVersion, shadow.clientVersion(), shadow.document());
    }

    private void saveBackupShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveBackupShadowDocument(new DefaultBackupShadowDocument<T>(newShadow.clientVersion(), newShadow));
    }

    public Document<T> patchDocument(final Edits edits) {
        final Document<T> document = dataStore.getDocument(edits.id());
        final Document<T> patched = synchronizer.patchDocument(edits, document);
        saveDocument(patched);
        return patched;
    }

    private void saveDocument(final Document<T> document) {
        dataStore.saveDocument(document);
    }

}
