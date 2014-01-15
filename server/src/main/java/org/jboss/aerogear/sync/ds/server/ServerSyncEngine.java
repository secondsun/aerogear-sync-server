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
package org.jboss.aerogear.sync.ds.server;

import org.jboss.aerogear.sync.ds.ClientDocument;
import org.jboss.aerogear.sync.ds.DefaultBackupShadowDocument;
import org.jboss.aerogear.sync.ds.DefaultClientDocument;
import org.jboss.aerogear.sync.ds.DefaultShadowDocument;
import org.jboss.aerogear.sync.ds.Document;
import org.jboss.aerogear.sync.ds.Edits;
import org.jboss.aerogear.sync.ds.ShadowDocument;

/**
 * The server side of the differential synchronization implementation.
 *
 * @param <T> The type of document that this implementation can handle.
 */
public class ServerSyncEngine<T> {

    private final ServerSynchronizer<T> synchronizer;
    private final ServerDataStore<T> dataStore;

    public ServerSyncEngine(final ServerSynchronizer<T> synchronizer, final ServerDataStore<T> dataStore) {
        this.synchronizer = synchronizer;
        this.dataStore = dataStore;
    }

    /**
     * Adds a new document which is "synchonrizable".
     *
     * A server does not create a new document itself, this would be create by a client
     * and a first revision is added to this synchronization engine by this method call.
     *
     * @param document the document to add.
     */
    public void addDocument(final Document<T> document) {
        dataStore.saveDocument(document);
    }

    /**
     * Adds a client side shadow document to the synchronization engine.
     *
     * @param documentId the id of the document for which a client shadow should be created.
     * @param clientId the client's id which will be used to identify the client's server side shadow document
     */
    public void addShadow(final String documentId, final String clientId) {
        final Document<T> document = getDocument(documentId);
        final ClientDocument<T> clientDocument = newClientDocument(clientId, documentId, document.content());
        // A clients shadow always begins with server version 0, and client version 0. Even if the server document
        // has existed for days and had been updated many time, the server version on the shadow is specific to this
        // client. The server version represents the latest version of the server document.
        saveShadow(shadowDoc(0, 0, clientDocument));
    }

    /**
     * Performs a diff of the server document aginst the client's shadow document.
     *
     * @param document the updated document.
     * @param clientId the client's id which is used to look up the client server side shadow document
     * @return {@link Edits} containing the edits for the changes in the document.
     */
    public Edits diff(final Document<T> document, final String clientId) {
        final ShadowDocument<T> shadow = getShadowDocument(clientId, document.id());
        final Edits newEdits = diff(document, shadow);
        final Edits pendingEdits = getPendingEdits(clientId, document.id());
        final Edits mergedEdits = merge(pendingEdits, newEdits);
        saveEdits(mergedEdits, document);
        saveShadow(incrementServerVersion(shadow));
        return mergedEdits;
    }

    /**
     * Patches the client side shadow with updates from the server.
     *
     * @param edits the updates from the server.
     */
    public ShadowDocument<T> patchShadow(final Edits edits) {
        final ShadowDocument<T> patchedShadow = synchronizer.patchShadow(edits, getShadowDocument(edits.clientId(), edits.documentId()));
        final ShadowDocument<T> incShadow = saveShadow(incrementClientVersion(patchedShadow));
        saveBackupShadow(incShadow);
        clearPendingEdits(edits);
        return incShadow;
    }

    /**
     * Patch the server document.
     *
     * @param edits the edits containing the changes to patch the server documnet
     * @return {@code Document} the patched document.
     */
    public Document<T> patchDocument(final Edits edits) {
        final Document<T> document = dataStore.getDocument(edits.documentId());
        final Document<T> patched = synchronizer.patchDocument(edits, document);
        final ShadowDocument<T> shadowDocument = getShadowDocument(edits.clientId(), document.id());
        saveDocument(patched);
        saveShadow(incrementServerVersion(shadowDocument));
        return patched;
    }

    private Document<T> getDocument(final String documentId) {
        return dataStore.getDocument(documentId);
    }

    private ClientDocument<T> newClientDocument(final String clientId, final String documentId, final T content) {
        return new DefaultClientDocument<T>(documentId, content, clientId);
    }

    private ShadowDocument<T> getShadowDocument(final String clientId, final String documentId) {
        return dataStore.getShadowDocument(clientId, documentId);
    }

    private Edits getPendingEdits(final String clientId, final String documentId) {
        return dataStore.getEdit(clientId, documentId);
    }

    private Edits diff(final Document<T> doc, final ShadowDocument<T> shadow) {
        return synchronizer.diff(doc, shadow);
    }

    private static Edits merge(final Edits pendingEdits, final Edits newEdits) {
        if (pendingEdits == null) {
            return newEdits;
        }
        pendingEdits.diffs().addAll(newEdits.diffs());
        return pendingEdits;
    }

    private void saveEdits(final Edits edits, final Document<T> document) {
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


    private ShadowDocument<T> incrementServerVersion(final ShadowDocument<T> shadow) {
        final long serverVersion = shadow.serverVersion() + 1;
        return shadowDoc(serverVersion, shadow.clientVersion(), shadow.document());
    }

    private void saveBackupShadow(final ShadowDocument<T> newShadow) {
        dataStore.saveBackupShadowDocument(new DefaultBackupShadowDocument<T>(newShadow.clientVersion(), newShadow));
    }

    private void saveDocument(final Document<T> document) {
        dataStore.saveDocument(document);
    }

    private void clearPendingEdits(final Edits edits) {
        //TODO: clear out pending edits.
    }

}
