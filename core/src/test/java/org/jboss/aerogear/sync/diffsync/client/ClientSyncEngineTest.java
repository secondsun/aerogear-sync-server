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
import org.jboss.aerogear.sync.diffsync.Diff;
import org.jboss.aerogear.sync.diffsync.Edits;
import org.jboss.aerogear.sync.diffsync.ShadowDocument;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.*;

public class ClientSyncEngineTest {

    private static final String ORGINAL_TEXT = "Do or do not, there is no try.";
    private static final String UPDATED_TEXT = "Do or do not, there is no try!";
    private static final String DOC_ID = "123456";
    private static final String CLIENT_ID = "clientA";
    private ClientDocument<String> clientDoc;
    private ClientDataStore<String> dataStore;
    private ClientSyncEngine<String> syncEngine;

    @Before
    public void setup() {
        dataStore = new ClientInMemoryDataStore();
        syncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(), dataStore);
        clientDoc = new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, CLIENT_ID);
        syncEngine.addDocument(clientDoc);
    }

    @Test
    public void addDocument() {
        assertThat(dataStore.getClientDocument(clientDoc.clientId(), clientDoc.id()), is(notNullValue()));
        assertThat(dataStore.getShadowDocument(clientDoc.id(), clientDoc.clientId()), is(notNullValue()));
        assertThat(dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id()), is(notNullValue()));
    }

    @Test
    public void addDocumentWithMultipleClients() {
        final String clientId1 = "client1";
        final String clientId2 = "client2";
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId1));
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId2));

        assertThat(dataStore.getClientDocument(clientId1, DOC_ID).id(), equalTo(DOC_ID));
        assertThat(dataStore.getShadowDocument(DOC_ID, clientId1).document().clientId(), equalTo(clientId1));
        assertThat(dataStore.getBackupShadowDocument(clientId1, DOC_ID), is(notNullValue()));

        assertThat(dataStore.getClientDocument(clientId2, DOC_ID).id(), equalTo(DOC_ID));
        assertThat(dataStore.getShadowDocument(DOC_ID, clientId2).document().clientId(), equalTo(clientId2));
        assertThat(dataStore.getBackupShadowDocument(clientId2, DOC_ID), is(notNullValue()));
    }

    @Test
    public void diff() {
        final Set<Edits> edits = syncEngine.diff(new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID));
        assertThat(edits.size(), is(1));
        final Edits edit = edits.iterator().next();
        assertEdits(edit);
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(edit.documentId(), edit.clientId());
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(0L));
    }

    @Test
    public void patch() {
        final ClientDocument<String> clientDoc = new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID);
        final ClientDocument<String> patched = syncEngine.patch(syncEngine.diff(clientDoc).iterator().next());
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(clientDoc.id(), clientDoc.clientId());
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id());
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(patched.content(), equalTo(UPDATED_TEXT));
    }

    private void assertEdits(final Edits edits) {
        assertThat(edits.documentId(), is(DOC_ID));
        assertThat(edits.clientVersion(), is(0L));
        final ClientDocument<String> document = dataStore.getShadowDocument(edits.documentId(), edits.clientId()).document();
        assertThat(edits.checksum(), equalTo(DiffMatchPatch.checksum(document.content())));
        assertThat(edits.diffs().size(), is(3));
        final List<Diff> diffs = edits.diffs();
        assertThat(diffs.get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Diff.Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Diff.Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));
    }
}
