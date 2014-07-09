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
package org.jboss.aerogear.sync.diffsync;

import org.jboss.aerogear.sync.diffsync.Diff.Operation;
import org.jboss.aerogear.sync.diffsync.client.ClientDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffsync.client.DefaultClientSynchronizer;
import org.jboss.aerogear.sync.diffsync.server.DefaultServerSynchronizer;
import org.jboss.aerogear.sync.diffsync.server.ServerDataStore;
import org.jboss.aerogear.sync.diffsync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.server.ServerSyncEngine;
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
        final String client1Id = "client1";
        final String client2Id = "client2";
        final String originalText = "Do or do not, there is no try.";
        final String updatedText = "Do or do not, there is no try!";
        final DefaultDocument<String> serverDocument = newDoc(documentId, originalText);
        syncEngine.addDocument(serverDocument, client1Id);
        syncEngine.addDocument(serverDocument, client2Id);

        final ShadowDocument<String> shadowBefore = dataStore.getShadowDocument(documentId, client1Id);
        assertThat(shadowBefore.clientVersion(), is(0L));
        assertThat(shadowBefore.serverVersion(), is(0L));

        final Edits clientEdits = generateClientSideEdits(documentId, originalText, client1Id, updatedText);
        assertThat(clientEdits.clientVersion(), is(0L));
        assertThat(clientEdits.serverVersion(), is(0L));
        assertThat(clientEdits.diffs().size(), is(3));

        syncEngine.patch(clientEdits);

        final Edits edits = syncEngine.diff(client2Id, documentId);
        assertThat(edits.clientVersion(), is(0L));
        assertThat(edits.serverVersion(), is(0L));
        assertThat(edits.clientId(), equalTo(client2Id));
        assertThat(edits.diffs().size(), is(3));
        assertThat(edits.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edits.diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(edits.diffs().get(1).text(), equalTo("."));
        assertThat(edits.diffs().get(2).operation(), is(Operation.ADD));
        assertThat(edits.diffs().get(2).text(), equalTo("!"));

        final ShadowDocument<String> shadowAfter = dataStore.getShadowDocument(documentId, client1Id);
        assertThat(shadowAfter.clientVersion(), is(1L));
        assertThat(shadowAfter.serverVersion(), is(0L));
    }

    @Test
    public void edits() {
        final String documentId = UUID.randomUUID().toString();
        final String client1Id = "client1";
        final String client2Id = "client2";
        final String version1 = "Do or do not, there is no try.";
        final String version2 = "Do or do not, there is no try!";
        final String version3 = "Do or do nothing, there is no try!";

        // inject the client document into the client engine.
        final ClientSyncEngine<String> clientSyncEngine = clientSyncEngine();
        final ClientDocument<String> clientDoc1 = newClientDoc(documentId, version1, client1Id);
        final ClientDocument<String> clientDoc2 = newClientDoc(documentId, version1, client2Id);
        clientSyncEngine.addDocument(clientDoc1);
        clientSyncEngine.addDocument(clientDoc2);

        // inject the server documents into the server engine.
        final DefaultDocument<String> serverDocument = newDoc(documentId, version1);
        syncEngine.addDocument(serverDocument, client1Id);
        syncEngine.addDocument(serverDocument, client2Id);

        final Edits clientEdits = clientSyncEngine.diff(newClientDoc(documentId, version2, client1Id)).iterator().next();
        assertThat(clientEdits.clientVersion(), is(0L));
        assertThat(clientEdits.serverVersion(), is(0L));
        assertThat(clientEdits.diffs().size(), is(3));

        syncEngine.patch(clientEdits);

        final Edits client1Edits = syncEngine.diff(client1Id, documentId);
        assertThat(client1Edits.clientId(), equalTo(client1Id));
        assertThat(client1Edits.clientVersion(), is(1L));
        assertThat(client1Edits.serverVersion(), is(0L));
        assertThat(client1Edits.diffs().size(), is(1));
        assertThat(client1Edits.diffs().get(0).operation(), is(Operation.UNCHANGED));

        final Edits client2Edits = syncEngine.diff(client2Id, documentId);
        assertThat(client2Edits.clientId(), equalTo(client2Id));
        assertThat(client2Edits.clientVersion(), is(0L));
        assertThat(client2Edits.serverVersion(), is(0L));
        assertThat(client2Edits.diffs().size(), is(3));
        assertThat(client2Edits.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(client2Edits.diffs().get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(client2Edits.diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(client2Edits.diffs().get(1).text(), equalTo("."));
        assertThat(client2Edits.diffs().get(2).operation(), is(Operation.ADD));
        assertThat(client2Edits.diffs().get(2).text(), equalTo("!"));

        clientSyncEngine.patch(clientEdits);

        final Edits clientEdits2 = clientSyncEngine.diff(newClientDoc(documentId, version3, client1Id)).iterator().next();
        assertThat(clientEdits2.clientVersion(), is(0L));
        assertThat(clientEdits2.serverVersion(), is(1L));
        assertThat(clientEdits2.diffs().size(), is(3));
        assertThat(clientEdits2.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(clientEdits2.diffs().get(1).operation(), is(Operation.ADD));
        assertThat(clientEdits2.diffs().get(1).text(), equalTo("hing"));
        assertThat(clientEdits2.diffs().get(2).operation(), is(Operation.UNCHANGED));

    }

    private static ClientDocument<String> newClientDoc(final String documentId, final String content, final String clientId) {
        return new DefaultClientDocument<String>(documentId, content, clientId);
    }

    private static Edits generateClientSideEdits(final String documentId,
                                          final String originalContent,
                                          final String clientId,
                                          final String updatedContent) {
        final ClientSyncEngine<String> clientSyncEngine = clientSyncEngine();
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId)).iterator().next();
    }

    private static ClientSyncEngine<String> clientSyncEngine() {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        return new ClientSyncEngine<String>(new DefaultClientSynchronizer(), clientDataStore);
    }

    private static DefaultDocument<String> newDoc(final String documentId, String content) {
        return new DefaultDocument<String>(documentId, content);
    }

}
