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

import java.util.LinkedList;
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
        syncEngine.addDocument(newDoc(documentId, "What!"), "client1");
        final Document<String> document = dataStore.getDocument(documentId);
        assertThat(document.id(), equalTo(documentId));
        assertThat(document.content(), equalTo("What!"));
    }

    @Test
    public void containsDocument() {
        final String documentId = UUID.randomUUID().toString();
        syncEngine.addDocument(newDoc(documentId, "What!"), "client1");
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
        assertThat(shadowDocument.document().clientId(), is("shadowTest"));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.document().id(), equalTo(documentId));
    }

    @Test
    public void patchOneVersion() {
        final String documentId = UUID.randomUUID().toString();
        final String clientOne = "client1";
        final String clientTwo = "client2";
        final String originalVersion = "Do or do not, there is no try.";
        final String versionOne = "Do or do not, there is no try!";

        final DefaultDocument<String> serverDocument = newDoc(documentId, originalVersion);
        syncEngine.addDocument(serverDocument, clientOne);
        syncEngine.addDocument(serverDocument, clientTwo);
        syncEngine.patch(clientSideEdits(documentId, originalVersion, clientOne, versionOne));

        final Edits edits = syncEngine.diff(clientTwo, documentId);
        assertThat(edits.clientVersion(), is(0L));
        assertThat(edits.serverVersion(), is(0L));
        assertThat(edits.clientId(), equalTo(clientTwo));
        final LinkedList<Diff> diffs = edits.diffs();
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(diffs.get(1).operation(), is(Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        final ShadowDocument<String> shadowAfter = dataStore.getShadowDocument(documentId, clientOne);
        assertThat(shadowAfter.clientVersion(), is(1L));
        assertThat(shadowAfter.serverVersion(), is(0L));
    }

    @Test
    public void patchTwoVersions() {
        final String documentId = UUID.randomUUID().toString();
        final String clientOne = "client1";
        final String clientTwo = "client2";
        final String originalVersion = "Do or do not, there is no try.";
        final String versionTwo = "Do or do not, there is no try!";
        final String versionThree = "Do or do nothing, there is no try!";

        // inject the client document into the client engine.
        final ClientSyncEngine<String> clientSyncEngine = clientSyncEngine();
        clientSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientOne));
        clientSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientTwo));

        // inject the server documents into the server engine.
        final DefaultDocument<String> serverDocument = newDoc(documentId, originalVersion);
        syncEngine.addDocument(serverDocument, clientOne);
        syncEngine.addDocument(serverDocument, clientTwo);

        final Edits clientEdits = clientSyncEngine.diff(newClientDoc(documentId, versionTwo, clientOne)).iterator().next();
        assertThat(clientEdits.clientVersion(), is(0L));
        assertThat(clientEdits.serverVersion(), is(0L));
        assertThat(clientEdits.diffs().size(), is(3));

        syncEngine.patch(clientEdits);

        final Edits firstEdits = syncEngine.diff(clientOne, documentId);
        assertThat(firstEdits.clientId(), equalTo(clientOne));
        assertThat(firstEdits.clientVersion(), is(1L));
        assertThat(firstEdits.serverVersion(), is(0L));
        assertThat(firstEdits.diffs().size(), is(1));
        assertThat(firstEdits.diffs().get(0).operation(), is(Operation.UNCHANGED));

        final Edits secondEdits = syncEngine.diff(clientTwo, documentId);
        assertThat(secondEdits.clientId(), equalTo(clientTwo));
        assertThat(secondEdits.clientVersion(), is(0L));
        assertThat(secondEdits.serverVersion(), is(0L));
        final LinkedList<Diff> secondDiffs = secondEdits.diffs();
        assertThat(secondDiffs.size(), is(3));
        assertThat(secondDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(secondDiffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(secondDiffs.get(1).operation(), is(Operation.DELETE));
        assertThat(secondDiffs.get(1).text(), equalTo("."));
        assertThat(secondDiffs.get(2).operation(), is(Operation.ADD));
        assertThat(secondDiffs.get(2).text(), equalTo("!"));

        clientSyncEngine.patch(clientEdits);

        final Edits thirdEdits = clientSyncEngine.diff(newClientDoc(documentId, versionThree, clientOne)).iterator().next();
        assertThat(thirdEdits.clientVersion(), is(0L));
        assertThat(thirdEdits.serverVersion(), is(1L));
        final LinkedList<Diff> thirdDiffs = thirdEdits.diffs();
        assertThat(thirdDiffs.size(), is(3));
        assertThat(thirdDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(thirdDiffs.get(1).operation(), is(Operation.ADD));
        assertThat(thirdDiffs.get(1).text(), equalTo("hing"));
        assertThat(thirdDiffs.get(2).operation(), is(Operation.UNCHANGED));
    }

    private static ClientDocument<String> newClientDoc(final String documentId, final String content, final String clientId) {
        return new DefaultClientDocument<String>(documentId, content, clientId);
    }

    private static Edits clientSideEdits(final String documentId,
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
