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

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.hamcrest.CoreMatchers.*;
import static org.jboss.aerogear.sync.common.DiffMatchPatch.*;

public class DefaultSynchronizerTest {

    private static final String ORGINAL_TEXT = "Do or do not, there is no try.";
    private static final String UPDATED_TEXT = "Do or do not, there is no try!";
    private static final String DOC_ID = "123456";

    private Synchronizer<String> synchronizer;
    private Document<String> clientDoc;
    private ShadowDocument<String> clientShadow;
    private Document<String> serverDoc;
    private ShadowDocument<String> serverShadow;

    @Before
    public void createDocuments() {
        serverDoc = new DefaultDocument<String>(DOC_ID, ORGINAL_TEXT);
        clientDoc = new DefaultDocument<String>(DOC_ID, serverDoc.content());
        serverShadow = new DefaultShadowDocument<String>(0, 0, serverDoc);
        clientShadow = new DefaultShadowDocument<String>(0, 0, clientDoc);
        synchronizer = new DefaultSynchronizer();
    }

    @Test
    public void diff() {
        final Document<String> updatedDoc = new DefaultDocument<String>(DOC_ID, UPDATED_TEXT);
        final Edits edits = synchronizer.diff(updatedDoc, clientShadow);
        assertThat(edits.version(), is(0L));
        assertThat(edits.checksum(), equalTo(checksum(clientShadow.document().content())));
        assertThat(edits.diffs().size(), is(3));
        final List<Diff> diffs = edits.diffs();
        assertThat(diffs.get(0).operation(), is(Diff.Operation.UNCHANGED));
        assertThat(diffs.get(0).text(), equalTo("Do or do not, there is no try"));
        assertThat(diffs.get(1).operation(), is(Diff.Operation.DELETE));
        assertThat(diffs.get(1).text(), equalTo("."));
        assertThat(diffs.get(2).operation(), is(Diff.Operation.ADD));
        assertThat(diffs.get(2).text(), equalTo("!"));
    }

    @Test
    public void patchShadow() {
        final Document<String> clientUpdate = new DefaultDocument<String>(DOC_ID, UPDATED_TEXT);
        // Produce and edits that would be sent over the network to the server.
        final Edits edits = synchronizer.diff(clientUpdate, clientShadow);
        // On the server side, the server takes the edits and tries to patch the client's server side shadow.
        final ShadowDocument<String> patched = synchronizer.patchShadow(edits, serverShadow);
        assertThat(patched.clientVersion(), is(edits.version()));
        assertThat(patched.serverVersion(), is(serverShadow.serverVersion()));
        assertThat(patched.document().content(), equalTo(UPDATED_TEXT));
    }

    @Test
    public void patchDocument() {
        final ShadowDocument<String> clientShadow = new DefaultShadowDocument<String>(0, 0, clientDoc);
        final Document<String> clientUpdate = new DefaultDocument<String>(DOC_ID, UPDATED_TEXT);
        final Edits edits = synchronizer.diff(clientUpdate, clientShadow);
        final Document<String> patched = synchronizer.patchDocument(edits, serverDoc);
        assertThat(patched.content(), equalTo(UPDATED_TEXT));
    }
}
