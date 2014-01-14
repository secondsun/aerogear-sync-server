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
package org.jboss.aerogear.sync.ds;

import org.jboss.aerogear.sync.common.DiffMatchPatch;
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
    private Document<String> clientDoc;
    private DataStore<String> dataStore;
    private ClientSyncEngine<String> syncEngine;

    @Before
    public void setup() {
        dataStore = new InMemoryDataStore();
        syncEngine = new ClientSyncEngine<String>(new DefaultSynchronizer(), dataStore);
        clientDoc = new DefaultDocument<String>(DOC_ID, ORGINAL_TEXT);
        syncEngine.newSyncDocument(clientDoc);
    }

    @Test
    public void newDocument() {
        assertThat(dataStore.getDocument(clientDoc.id()), is(notNullValue()));
        assertThat(dataStore.getShadowDocument(clientDoc.id()), is(notNullValue()));
        assertThat(dataStore.getBackupShadowDocument(clientDoc.id()), is(notNullValue()));
    }

    @Test
    public void clientDiff() {
        syncEngine.diff(new DefaultDocument<String>(DOC_ID, UPDATED_TEXT));
        assertEdits(dataStore.getEdit(DOC_ID));
        final ShadowDocument shadowDocument = dataStore.getShadowDocument(DOC_ID);
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.serverVersion(), is(0L));
    }

    @Test
    public void patchShadow() {
        syncEngine.patchShadow(syncEngine.diff(new DefaultDocument<String>(DOC_ID, UPDATED_TEXT)));
        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(DOC_ID);
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.serverVersion(), is(1L));
        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(DOC_ID);
        assertThat(backupShadowDocument.version(), is(0L));
    }

    @Test
    public void patchDocument() {
        final Document<String> serverUpdate = new DefaultDocument<String>(DOC_ID, UPDATED_TEXT);
        final Edits edits = syncEngine.diff(serverUpdate);
        final Document<String> document = syncEngine.patchDocument(edits);
        assertThat(document.content(), equalTo(UPDATED_TEXT));
    }

    private void assertEdits(final Edits edits) {
        assertThat(edits.id(), is(DOC_ID));
        assertThat(edits.version(), is(0L));
        assertThat(edits.checksum(), equalTo(DiffMatchPatch.checksum(dataStore.getShadowDocument(edits.id()).document().content())));
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
