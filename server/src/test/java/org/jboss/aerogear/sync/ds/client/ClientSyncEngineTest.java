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
package org.jboss.aerogear.sync.ds.client;

import org.jboss.aerogear.sync.common.DiffMatchPatch;
import org.jboss.aerogear.sync.ds.BackupShadowDocument;
import org.jboss.aerogear.sync.ds.ClientDocument;
import org.jboss.aerogear.sync.ds.DefaultClientDocument;
import org.jboss.aerogear.sync.ds.Diff;
import org.jboss.aerogear.sync.ds.Document;
import org.jboss.aerogear.sync.ds.Edits;
import org.jboss.aerogear.sync.ds.ShadowDocument;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        assertThat(dataStore.getShadowDocument(clientDoc.clientId(), clientDoc.id()), is(notNullValue()));
        assertThat(dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id()), is(notNullValue()));
    }

    @Test
    public void addDocumentWithMultipleClients() {
        final String clientId1 = "client1";
        final String clientId2 = "client2";
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId1));
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId2));

        assertThat(dataStore.getClientDocument(clientId1, DOC_ID).id(), equalTo(DOC_ID));
        assertThat(dataStore.getShadowDocument(clientId1, DOC_ID).document().clientId(), equalTo(clientId1));
        assertThat(dataStore.getBackupShadowDocument(clientId1, DOC_ID), is(notNullValue()));

        assertThat(dataStore.getClientDocument(clientId2, DOC_ID).id(), equalTo(DOC_ID));
        assertThat(dataStore.getShadowDocument(clientId2, DOC_ID).document().clientId(), equalTo(clientId2));
        assertThat(dataStore.getBackupShadowDocument(clientId2, DOC_ID), is(notNullValue()));
    }

    @Test
    public void diff() {
        final Edits edits = syncEngine.diff(new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID));
        assertEdits(dataStore.getEdit(edits.clientId(), edits.documentId()));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(edits.clientId(), edits.documentId());
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(0L));
    }

    @Test
    public void patchShadow() {
        final ClientDocument<String> clientDoc = new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID);
        syncEngine.patchShadow(syncEngine.diff(clientDoc));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(clientDoc.clientId(), clientDoc.id());
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id());
        assertThat(backupShadowDocument.version(), is(0L));
    }

    @Test
    public void patchDocument() {
        final ClientDocument<String> serverUpdate = new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID);
        final Edits edits = syncEngine.diff(serverUpdate);
        final Document<String> document = syncEngine.patchDocument(edits);
        assertThat(document.content(), equalTo(UPDATED_TEXT));
    }

    private void assertEdits(final Edits edits) {
        assertThat(edits.documentId(), is(DOC_ID));
        assertThat(edits.version(), is(0L));
        final ClientDocument<String> document = dataStore.getShadowDocument(edits.clientId(), edits.documentId()).document();
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
