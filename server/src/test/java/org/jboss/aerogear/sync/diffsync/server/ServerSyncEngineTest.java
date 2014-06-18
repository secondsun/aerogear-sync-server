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
package org.jboss.aerogear.sync.diffsync.server;

import org.jboss.aerogear.sync.diffsync.DefaultClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultDocument;
import org.jboss.aerogear.sync.diffsync.Diff;
import org.jboss.aerogear.sync.diffsync.Document;
import org.jboss.aerogear.sync.diffsync.Edits;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;
import org.jboss.aerogear.sync.diffsync.client.ClientDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffsync.client.DefaultClientSynchronizer;
import org.junit.Before;
import org.junit.Test;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerSyncEngineTest {

    private ServerDataStore<String> dataStore;
    private ServerSyncEngine<String> syncEngine;

    @Before
    public void setup() {
        dataStore = new ServerInMemoryDataStore();
        syncEngine = new ServerSyncEngine<String>(new DefaultServerSynchronizer(), dataStore);
    }

    @Test
    public void addDocument() {
        final String documentId = UUID.randomUUID().toString();
        syncEngine.addDocument(newDoc(documentId, "What!"), "test");
        final Document<String> document = dataStore.getDocument(documentId);
        assertThat(document.id(), equalTo(documentId));
        assertThat(document.content(), equalTo("What!"));
    }

    @Test
    public void containsDocument() {
        final String documentId = UUID.randomUUID().toString();
        syncEngine.addDocument(newDoc(documentId, "What!"), "test");
        assertThat(syncEngine.contains(documentId), is(true));
    }

    @Test
    public void containsDocumentNonExistent() {
        assertThat(syncEngine.contains("bogusId"), is(false));
    }

    @Test
    public void addShadow() {
        final String documentId = UUID.randomUUID().toString();
        final String clientId = "shadowTest";
        syncEngine.addDocument(newDoc(documentId, "What!"), clientId);
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.document().id(), equalTo(documentId));
    }

    @Test
    public void patch() {
        final String documentId = UUID.randomUUID().toString();
        final String clientId = "patchTest";
        final String originalText = "Do or do not, there is no try.";
        final String updatedText = "Do or do not, there is no try!";
        final DefaultDocument<String> serverDocument = newDoc(documentId, originalText);
        syncEngine.addDocument(serverDocument, clientId);

        final ShadowDocument<String> shadowBefore = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowBefore.clientVersion(), is(0L));
        assertThat(shadowBefore.serverVersion(), is(0L));

        final Edits clientEdits = generateClientSideEdits(documentId, originalText, clientId, updatedText);
        assertThat(clientEdits.version(), is(0L));
        assertThat(clientEdits.diffs().size(), is(3));

        final Edits edits = syncEngine.patch(clientEdits);
        assertThat(edits.version(), is(1L));
        assertThat(edits.clientId(), equalTo(clientId));
        assertThat(edits.diffs().size(), is(1));
        assertThat(edits.diffs().get(0).text(), equalTo(updatedText));
        assertThat(edits.diffs().get(0).operation(), is(Diff.Operation.UNCHANGED));

        final ShadowDocument<String> shadowAfter = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowAfter.clientVersion(), is(1L));
        assertThat(shadowAfter.serverVersion(), is(1L));
    }

    private static Edits generateClientSideEdits(final String documentId,
                                          final String originalContent,
                                          final String clientId,
                                          final String updatedContent) {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(), clientDataStore);
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId));
    }

    private static DefaultDocument<String> newDoc(final String documentId, String content) {
        return new DefaultDocument<String>(documentId, content);
    }

}
