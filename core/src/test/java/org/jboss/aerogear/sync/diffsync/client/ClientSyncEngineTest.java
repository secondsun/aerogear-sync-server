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

import org.jboss.aerogear.sync.common.DiffMatchPatch;
import org.jboss.aerogear.sync.diffsync.BackupShadowDocument;
import org.jboss.aerogear.sync.diffsync.ClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultDocument;
import org.jboss.aerogear.sync.diffsync.Diff;
import org.jboss.aerogear.sync.diffsync.Document;
import org.jboss.aerogear.sync.diffsync.Edit;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;
import org.jboss.aerogear.sync.diffsync.server.DefaultServerSynchronizer;
import org.jboss.aerogear.sync.diffsync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.server.ServerSyncEngine;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.*;

public class ClientSyncEngineTest {

    private ClientDataStore<String> dataStore;
    private ClientSyncEngine<String> clientSyncEngine;
    private ServerSyncEngine<String> serverSyncEngine;

    @Before
    public void setup() {
        dataStore = new ClientInMemoryDataStore();
        clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(), dataStore);
        serverSyncEngine = new ServerSyncEngine<String>(new DefaultServerSynchronizer(), new ServerInMemoryDataStore());
    }

    @Test
    public void addDocument() {
        final String docId = "123456";
        final String clientId = "client1";
        final String originalVersion = "Do or do not, there is no try.";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientId));

        final ClientDocument<String> clientDocument = dataStore.getClientDocument(docId, clientId);
        assertThat(clientDocument.clientId(), equalTo(clientId));
        assertThat(clientDocument.id(), equalTo(docId));
        assertThat(clientDocument.content(), equalTo(originalVersion));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(docId, clientId);
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.document(), equalTo(clientDocument));

        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(docId, clientId);
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(backupShadowDocument.shadow(), equalTo(shadowDocument));
    }

    @Test
    public void diff() {
        final String docId = "123456";
        final String clientOne = "client1";
        final String originalVersion = "Do or do not, there is no try.";
        final String secondVersion = "Do or do not, there is no try!";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientOne));

        final Queue<Edit> edits = clientSyncEngine.diff(newClientDoc(docId, secondVersion, clientOne));
        assertThat(edits.size(), is(1));
        final Edit edit = edits.iterator().next();
        assertThat(edit.documentId(), is(docId));
        // client version is only incremented after the diff is taken. See shadowDocument asserts below.
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));

        final List<Diff> diffs = edit.diffs();
        assertThat(edit.diffs().size(), is(3));
        assertThat(diffs.get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Diff.Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Diff.Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        final ClientDocument<String> document = dataStore.getShadowDocument(edit.documentId(), edit.clientId()).document();

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(edit.documentId(), edit.clientId());
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.document(), equalTo(document));
    }

    @Test
    public void patch() {
        final String docId = "123456";
        final String clientOne = "client1";
        final String clientTwo = "client2";
        final String originalVersion = "Do or do not, there is no try.";
        final String secondVersion = "Do or do not, there is no try!";

        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientOne));
        clientSyncEngine.addDocument(newClientDoc(docId, originalVersion, clientTwo));
        serverSyncEngine.addDocument(newDoc(docId, originalVersion), clientOne);
        serverSyncEngine.addDocument(newDoc(docId, originalVersion), clientTwo);

        final Queue<Edit> edits = clientSyncEngine.diff(newClientDoc(docId, secondVersion, clientOne));
        assertThat(edits.size(), is(1));
        final Edit edit = edits.peek();
        assertThat(edit.clientId(), equalTo(clientOne));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        final List<Diff> diffs = edit.diffs();
        assertThat(diffs.size(), is(3));
        assertThat(diffs.get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Diff.Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Diff.Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));

        serverSyncEngine.patch(edits);

        final Edit serverEdit = serverSyncEngine.diff(clientTwo, docId);
        assertThat(serverEdit.clientId(), equalTo(clientTwo));
        assertThat(serverEdit.clientVersion(), is(0L));
        assertThat(serverEdit.serverVersion(), is(0L));
        assertThat(serverEdit.diffs().size(), is(3));
        final List<Diff> serverDiffs = edit.diffs();
        assertThat(serverDiffs.size(), is(3));
        assertThat(serverDiffs.get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(serverDiffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(serverDiffs.get(1).operation(), is(Diff.Operation.DELETE));
        assertThat(serverDiffs.get(1).text(), equalTo("."));
        assertThat(serverDiffs.get(2).operation(), is(Diff.Operation.ADD));
        assertThat(serverDiffs.get(2).text(), equalTo("!"));

        clientSyncEngine.patch(asList(serverEdit));
        final Queue<Edit> clientOneEdits = dataStore.getEdits(clientTwo, docId);
        assertThat(clientOneEdits.isEmpty(), is(true));
        final Queue<Edit> clientTwoEdits = dataStore.getEdits(clientTwo, docId);
        assertThat(clientTwoEdits.isEmpty(), is(true));

        final Edit serverEdit1 = serverSyncEngine.diff(clientOne, docId);
        assertThat(serverEdit1.diffs().size(), is(1));
        assertThat(serverEdit1.diffs().get(0).operation(), is(Diff.Operation.UNCHANGED));

        final Edit serverEdit2 = serverSyncEngine.diff(clientTwo, docId);
        assertThat(serverEdit2.diffs().size(), is(1));
        assertThat(serverEdit2.diffs().get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(serverEdit2.diffs().get(0).text(), equalTo("Do or do not, there is no try!"));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(docId, clientTwo);
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        assertThat(shadowDocument.document().content(), equalTo(secondVersion));

        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(docId, clientTwo);
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(backupShadowDocument.shadow().document().content(), equalTo(originalVersion));
    }

    private static Queue<Edit> asList(final Edit edit) {
        return new ConcurrentLinkedQueue<Edit>(Arrays.asList(edit));
    }

    private static ClientDocument<String> newClientDoc(final String documentId, final String content, final String clientId) {
        return new DefaultClientDocument<String>(documentId, content, clientId);
    }

    private static Document<String> newDoc(final String documentId, final String content) {
        return new DefaultDocument<String>(documentId, content);
    }
}
