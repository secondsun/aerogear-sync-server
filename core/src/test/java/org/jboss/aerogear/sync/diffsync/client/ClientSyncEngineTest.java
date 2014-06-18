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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
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
        MatcherAssert.assertThat(dataStore.getClientDocument(clientDoc.clientId(), clientDoc.id()), CoreMatchers.is(CoreMatchers.notNullValue()));
        MatcherAssert.assertThat(dataStore.getShadowDocument(clientDoc.id(), clientDoc.clientId()), CoreMatchers.is(CoreMatchers.notNullValue()));
        MatcherAssert.assertThat(dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id()), CoreMatchers.is(CoreMatchers.notNullValue()));
    }

    @Test
    public void addDocumentWithMultipleClients() {
        final String clientId1 = "client1";
        final String clientId2 = "client2";
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId1));
        syncEngine.addDocument(new DefaultClientDocument<String>(DOC_ID, ORGINAL_TEXT, clientId2));

        MatcherAssert.assertThat(dataStore.getClientDocument(clientId1, DOC_ID).id(), CoreMatchers.equalTo(DOC_ID));
        MatcherAssert.assertThat(dataStore.getShadowDocument(DOC_ID, clientId1).document().clientId(), CoreMatchers.equalTo(clientId1));
        MatcherAssert.assertThat(dataStore.getBackupShadowDocument(clientId1, DOC_ID), CoreMatchers.is(CoreMatchers.notNullValue()));

        MatcherAssert.assertThat(dataStore.getClientDocument(clientId2, DOC_ID).id(), CoreMatchers.equalTo(DOC_ID));
        MatcherAssert.assertThat(dataStore.getShadowDocument(DOC_ID, clientId2).document().clientId(), CoreMatchers.equalTo(clientId2));
        MatcherAssert.assertThat(dataStore.getBackupShadowDocument(clientId2, DOC_ID), CoreMatchers.is(CoreMatchers.notNullValue()));
    }

    @Test
    public void diff() {
        final Edits edits = syncEngine.diff(new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID));
        assertEdits(dataStore.getEdit(edits.clientId(), edits.documentId()));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(edits.documentId(), edits.clientId());
        MatcherAssert.assertThat(shadowDocument.clientVersion(), CoreMatchers.is(1L));
        MatcherAssert.assertThat(shadowDocument.serverVersion(), CoreMatchers.is(0L));
    }

    @Test
    public void patch() {
        final ClientDocument<String> clientDoc = new DefaultClientDocument<String>(DOC_ID, UPDATED_TEXT, CLIENT_ID);
        final ClientDocument<String> patched = syncEngine.patch(syncEngine.diff(clientDoc));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(clientDoc.id(), clientDoc.clientId());
        MatcherAssert.assertThat(shadowDocument.clientVersion(), CoreMatchers.is(0L));
        MatcherAssert.assertThat(shadowDocument.serverVersion(), CoreMatchers.is(1L));
        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(clientDoc.clientId(), clientDoc.id());
        MatcherAssert.assertThat(backupShadowDocument.version(), CoreMatchers.is(0L));
        MatcherAssert.assertThat(patched.content(), CoreMatchers.equalTo(UPDATED_TEXT));
    }

    private void assertEdits(final Edits edits) {
        MatcherAssert.assertThat(edits.documentId(), CoreMatchers.is(DOC_ID));
        MatcherAssert.assertThat(edits.version(), CoreMatchers.is(0L));
        final ClientDocument<String> document = dataStore.getShadowDocument(edits.documentId(), edits.clientId()).document();
        MatcherAssert.assertThat(edits.checksum(), CoreMatchers.equalTo(DiffMatchPatch.checksum(document.content())));
        MatcherAssert.assertThat(edits.diffs().size(), CoreMatchers.is(3));
        final List<Diff> diffs = edits.diffs();
        MatcherAssert.assertThat(diffs.get(0).operation(), CoreMatchers.is(Diff.Operation.UNCHANGED));
        MatcherAssert.assertThat(diffs.get(0).text(), CoreMatchers.equalTo("Do or do not, there is no try"));
        MatcherAssert.assertThat(diffs.get(1).operation(), CoreMatchers.is(Diff.Operation.DELETE));
        MatcherAssert.assertThat(diffs.get(1).text(), CoreMatchers.equalTo("."));
        MatcherAssert.assertThat(diffs.get(2).operation(), CoreMatchers.is(Diff.Operation.ADD));
        MatcherAssert.assertThat(diffs.get(2).text(), CoreMatchers.equalTo("!"));
    }
}
