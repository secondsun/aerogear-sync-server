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

import org.jboss.aerogear.sync.common.DiffMatchPatch;
import org.jboss.aerogear.sync.ds.ClientDocument;
import org.jboss.aerogear.sync.ds.DefaultClientDocument;
import org.jboss.aerogear.sync.ds.DefaultDocument;
import org.jboss.aerogear.sync.ds.Diff;
import org.jboss.aerogear.sync.ds.Document;
import org.jboss.aerogear.sync.ds.Edits;
import org.jboss.aerogear.sync.ds.ShadowDocument;
import org.jboss.aerogear.sync.ds.client.ClientDataStore;
import org.jboss.aerogear.sync.ds.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.ds.client.ClientSyncEngine;
import org.jboss.aerogear.sync.ds.client.DefaultClientSynchronizer;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerSyncEngineTest {

    private static final String ORGINAL_TEXT = "Do or do not, there is no try.";
    private static final String UPDATED_TEXT = "Do or do not, there is no try!";
    private static final String DOC_ID = "123456";
    private static final String CLIENT_ID = "clientA";
    private Document<String> serverDoc;
    private ServerDataStore<String> dataStore;
    private ServerSyncEngine<String> syncEngine;

    @Before
    public void setup() {
        dataStore = new ServerInMemoryDataStore();
        syncEngine = new ServerSyncEngine<String>(new DefaultServerSynchronizer(), dataStore);
        serverDoc = newDoc(DOC_ID, ORGINAL_TEXT);
    }

    @Test
    public void addDocument() {
        syncEngine.addDocument(serverDoc);
        final Document<String> document = dataStore.getDocument(serverDoc.id());
        assertThat(document.id(), equalTo(DOC_ID));
        assertThat(document.content(), equalTo(ORGINAL_TEXT));
    }

    @Test
    public void addShadow() {
        syncEngine.addDocument(serverDoc);
        syncEngine.addShadow(serverDoc.id(), CLIENT_ID);
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(CLIENT_ID, serverDoc.id());
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.document().id(), equalTo(DOC_ID));
    }

    @Test
    public void diff() {
        final String clientId = "diffTest";
        syncEngine.addDocument(serverDoc);
        syncEngine.addShadow(serverDoc.id(), clientId);
        final Edits edits = syncEngine.diff(newDoc(DOC_ID, UPDATED_TEXT), clientId);
        assertEdits(dataStore.getEdit(edits.clientId(), edits.documentId()));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(edits.clientId(), edits.documentId());
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
    }

    @Test
    public void patchShadow() {
        syncEngine.addDocument(serverDoc);
        syncEngine.addShadow(serverDoc.id(), CLIENT_ID);
        final Edits edits = generateClientSideEdits(DOC_ID, ORGINAL_TEXT, CLIENT_ID, UPDATED_TEXT);
        assertThat(edits.version(), is(0L));

        final ShadowDocument<String> shadowDocument = syncEngine.patchShadow(edits);
        assertThat(shadowDocument.clientVersion(), is(1L));
        // Server version is only updated when the server docuement is patched, not the shadow document.
        assertThat(shadowDocument.serverVersion(), is(0L));
    }

    @Test
    public void patchDocument() {
        syncEngine.addDocument(serverDoc);
        syncEngine.addShadow(serverDoc.id(), CLIENT_ID);
        final Edits edits = generateClientSideEdits(DOC_ID, ORGINAL_TEXT, CLIENT_ID, UPDATED_TEXT);
        // patchShadow is always done before patchDocument.
        syncEngine.patchShadow(edits);
        ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(CLIENT_ID, edits.documentId());
        assertThat(shadowDocument.clientVersion(), is(1L));

        final Document<String> document = syncEngine.patchDocument(edits);
        assertThat(document.content(), equalTo(UPDATED_TEXT));
        // The server version is only updated when the server docuement is patched, not the shadow document.
        shadowDocument = dataStore.getShadowDocument(CLIENT_ID, document.id());
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        assertThat(shadowDocument.document().content(), equalTo(UPDATED_TEXT));
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
