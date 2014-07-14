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
package org.jboss.aerogear.diffsync.server;

import org.jboss.aerogear.diffsync.BackupShadowDocument;
import org.jboss.aerogear.diffsync.DefaultDocument;
import org.jboss.aerogear.diffsync.DefaultEdits;
import org.jboss.aerogear.diffsync.Diff.Operation;
import org.jboss.aerogear.diffsync.Document;
import org.jboss.aerogear.diffsync.Edit;
import org.jboss.aerogear.diffsync.EditBuilder;
import org.jboss.aerogear.diffsync.Edits;
import org.jboss.aerogear.diffsync.ShadowDocument;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static java.util.Arrays.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerSyncEngineTest {

    private ServerInMemoryDataStore dataStore;
    private ServerSyncEngine<String> engine;

    @Before
    public void setup() {
        dataStore = new ServerInMemoryDataStore();
        engine = new ServerSyncEngine<String>(new DefaultServerSynchronizer(), dataStore);
    }

    @Test
    public void addDocument() throws Exception {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "{\"name\": \"Mr.Babar\"}";
        final Document<String> document = new DefaultDocument<String>(documentId, originalVersion);
        engine.addDocument(document, clientId);

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.document().id(), equalTo(documentId));
        assertThat(shadowDocument.document().clientId(), equalTo(clientId));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(0L));
        assertThat(shadowDocument.document().content(), equalTo(originalVersion));

        final BackupShadowDocument<String> backupShadow = dataStore.getBackupShadowDocument(documentId, clientId);
        assertThat(backupShadow.version(), is(0L));
        assertThat(backupShadow.shadow(), equalTo(shadowDocument));
    }

    @Test
    public void diff() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "{\"name\": \"Mr.Babar\"}";
        final Document<String> document = new DefaultDocument<String>(documentId, originalVersion);
        engine.addDocument(document, clientId);

        final Edit edit = engine.diff(documentId, clientId);
        assertThat(edit.documentId(), equalTo(documentId));
        assertThat(edit.clientId(), equalTo(clientId));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.diffs().size(), is(1));
        assertThat(edit.diffs().peek().operation(), is(Operation.UNCHANGED));
        assertThat(edit.diffs().peek().text(), equalTo(originalVersion));
    }

    @Test
    public void patch() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "{\"name\": \"Mr.Babar\"}";
        final String updatedVersion = "{\"name\": \"Mr.Rosen\"}";
        final Document<String> document = new DefaultDocument<String>(documentId, originalVersion);
        engine.addDocument(document, clientId);

        final Edit edit = EditBuilder.withDocumentId(documentId)
                .clientId(clientId)
                .unchanged("{\"name\": ")
                .delete("\"Mr.Babar\"")
                .add("\"Mr.Rosen\"")
                .unchanged("}")
                .build();
        engine.patch(edits(documentId, clientId, edit));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.document().id(), equalTo(documentId));
        assertThat(shadowDocument.document().clientId(), equalTo(clientId));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.document().content(), equalTo(updatedVersion));

        final BackupShadowDocument<String> backupShadow = dataStore.getBackupShadowDocument(documentId, clientId);
        assertThat(backupShadow.version(), is(0L));
        assertThat(backupShadow.shadow().document().content(), equalTo(originalVersion));
    }

    @Test
    public void patchVersionAlreadyOnServer() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "{\"name\": \"Mr.Babar\"}";
        final String updatedVersion = "{\"name\": \"Mr.Rosen\"}";
        final Document<String> document = new DefaultDocument<String>(documentId, originalVersion);
        engine.addDocument(document, clientId);

        final Edit edit = EditBuilder.withDocumentId(documentId)
                .clientId(clientId)
                .unchanged("{\"name\": ")
                .delete("\"Mr.Babar\"")
                .add("\"Mr.Rosen\"")
                .unchanged("}")
                .build();
        engine.patch(edits(documentId, clientId, edit, edit));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.document().id(), equalTo(documentId));
        assertThat(shadowDocument.document().clientId(), equalTo(clientId));
        assertThat(shadowDocument.serverVersion(), is(0L));
        assertThat(shadowDocument.clientVersion(), is(1L));
        assertThat(shadowDocument.document().content(), equalTo(updatedVersion));

        final BackupShadowDocument<String> backupShadow = dataStore.getBackupShadowDocument(documentId, clientId);
        assertThat(backupShadow.version(), is(0L));
        assertThat(backupShadow.shadow().document().content(), equalTo(originalVersion));
    }

    @Test
    public void patchMultipleVersions() {
        final String documentId = "1234";
        final String clientId = "client1";
        final String originalVersion = "{\"name\": \"Mr.Babar\"}";
        final String secondVersion = "{\"name\": \"Mr.Poon\"}";
        final Document<String> document = new DefaultDocument<String>(documentId, originalVersion);
        engine.addDocument(document, clientId);

        final Edit edit1 = EditBuilder.withDocumentId(documentId)
                .clientId(clientId)
                .clientVersion(0)
                .serverVersion(0)
                .unchanged("{\"name\": ")
                .delete("\"Mr.Babar\"")
                .add("\"Mr.Rosen\"")
                .unchanged("}")
                .build();
        final Edit edit2 = EditBuilder.withDocumentId(documentId)
                .clientId(clientId)
                // after the first diff on the client, the shadow client version will have been incremented
                // and the following diff will use that shadow, hence the incremented client version here.
                .clientVersion(1)
                .serverVersion(0)
                .unchanged("{\"name\": ")
                .delete("\"Mr.Rosen\"")
                .add("\"Mr.Poon\"")
                .unchanged("}")
                .build();
        engine.patch(edits(documentId, clientId, edit1, edit2));

        final ShadowDocument<String> shadowDocument = dataStore.getShadowDocument(documentId, clientId);
        assertThat(shadowDocument.document().content(), equalTo(secondVersion));
        assertThat(shadowDocument.clientVersion(), is(2L));
        assertThat(shadowDocument.serverVersion(), is(0L));

        final BackupShadowDocument<String> backupShadowDocument = dataStore.getBackupShadowDocument(documentId, clientId);
        assertThat(backupShadowDocument.shadow().document().content(), equalTo(originalVersion));
        assertThat(backupShadowDocument.version(), is(0L));
        assertThat(backupShadowDocument.shadow().clientVersion(), is(0L));
    }

    private static Edits edits(final String docId, final String clientId, Edit... edit) {
        return new DefaultEdits(docId, clientId, new LinkedList<Edit>(asList(edit)));
    }
}
