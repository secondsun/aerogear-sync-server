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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerSyncEngineTest {

    private ServerDataStore<String> dataStore;
    private ServerSyncEngine<String> serverSyncEngine;

    @Before
    public void setup() {
        dataStore = new ServerInMemoryDataStore();
        serverSyncEngine = new ServerSyncEngine<String>(new DefaultServerSynchronizer(), dataStore);
    }

    @Test
    public void addDocument() {
        final String documentId = UUID.randomUUID().toString();
        serverSyncEngine.addDocument(newDoc(documentId, "What!"), "client1");
        final Document<String> document = dataStore.getDocument(documentId);
        assertThat(document.id(), equalTo(documentId));
        assertThat(document.content(), equalTo("What!"));
    }

    @Test
    public void containsDocument() {
        final String documentId = UUID.randomUUID().toString();
        serverSyncEngine.addDocument(newDoc(documentId, "What!"), "client1");
        assertThat(serverSyncEngine.contains(documentId), is(true));
    }

    @Test
    public void containsDocumentNonExistent() {
        assertThat(serverSyncEngine.contains("bogusId"), is(false));
    }

    @Test
    public void addShadow() {
        final String documentId = UUID.randomUUID().toString();
        final String clientId = "shadowTest";
        serverSyncEngine.addDocument(newDoc(documentId, "What!"), clientId);
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
        serverSyncEngine.addDocument(serverDocument, clientOne);
        serverSyncEngine.addDocument(serverDocument, clientTwo);
        serverSyncEngine.patch(clientSideEdits(documentId, originalVersion, clientOne, versionOne));

        final Edit edit = serverSyncEngine.diff(clientTwo, documentId);
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.clientId(), equalTo(clientTwo));
        final LinkedList<Diff> diffs = edit.diffs();
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

        final ClientSyncEngine<String> clientOneSyncEngine = clientSyncEngine();
        final ClientSyncEngine<String> clientTwoSyncEngine = clientSyncEngine();
        // inject the client document into the client engine.
        clientOneSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientOne));
        clientTwoSyncEngine.addDocument(newClientDoc(documentId, originalVersion, clientTwo));

        // add the server documents into the server engine for both clients.
        final DefaultDocument<String> serverDocument = newDoc(documentId, originalVersion);
        serverSyncEngine.addDocument(serverDocument, clientOne);
        serverSyncEngine.addDocument(serverDocument, clientTwo);

        // create an update originating from client1.
        serverSyncEngine.patch(clientOneSyncEngine.diff(newClientDoc(documentId, versionTwo, clientOne)));
        final Edit clientOneServerEdit = serverSyncEngine.diff(clientOne, documentId);
        assertThat(clientOneServerEdit.clientId(), equalTo(clientOne));
        assertThat(clientOneServerEdit.clientVersion(), is(1L));
        assertThat(clientOneServerEdit.serverVersion(), is(0L));
        assertThat(clientOneServerEdit.diffs().size(), is(1));
        assertThat(clientOneServerEdit.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(clientOneServerEdit.diffs().get(0).text(), equalTo(versionTwo));
        // no patch required for clientOneSyncEngine as this was performed after the diff was taken.
        clientOneSyncEngine.patch(asList(clientOneServerEdit));

        final Edit clientTwoServerEdit = serverSyncEngine.diff(clientTwo, documentId);
        assertThat(clientTwoServerEdit.clientId(), equalTo(clientTwo));
        assertThat(clientTwoServerEdit.clientVersion(), is(0L));
        assertThat(clientTwoServerEdit.serverVersion(), is(0L));
        final LinkedList<Diff> clientTwoServerDiffs = clientTwoServerEdit.diffs();
        assertThat(clientTwoServerDiffs.size(), is(3));
        assertThat(clientTwoServerDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(clientTwoServerDiffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(clientTwoServerDiffs.get(1).operation(), is(Operation.DELETE));
        assertThat(clientTwoServerDiffs.get(1).text(), equalTo("."));
        assertThat(clientTwoServerDiffs.get(2).operation(), is(Operation.ADD));
        assertThat(clientTwoServerDiffs.get(2).text(), equalTo("!"));
        clientTwoSyncEngine.patch(asList(clientTwoServerEdit));

        serverSyncEngine.patch(clientOneSyncEngine.diff(newClientDoc(documentId, versionThree, clientOne)));
        final Edit thirdEdit = serverSyncEngine.diff(clientTwo, documentId);
        assertThat(thirdEdit.clientVersion(), is(0L));
        assertThat(thirdEdit.serverVersion(), is(1L));
        final LinkedList<Diff> thirdDiffs = thirdEdit.diffs();
        assertThat(thirdDiffs.size(), is(3));
        assertThat(thirdDiffs.get(0).operation(), is(Operation.UNCHANGED));
        assertThat(thirdDiffs.get(1).operation(), is(Operation.ADD));
        assertThat(thirdDiffs.get(1).text(), equalTo("hing"));
        assertThat(thirdDiffs.get(2).operation(), is(Operation.UNCHANGED));
    }

    private static Queue<Edit> asList(final Edit edit) {
        return new ConcurrentLinkedQueue<Edit>(Arrays.asList(edit));
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
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId));
    }

    private static ClientSyncEngine<String> clientSyncEngine() {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        return new ClientSyncEngine<String>(new DefaultClientSynchronizer(), clientDataStore);
    }

    private static DefaultDocument<String> newDoc(final String documentId, String content) {
        return new DefaultDocument<String>(documentId, content);
    }

}
