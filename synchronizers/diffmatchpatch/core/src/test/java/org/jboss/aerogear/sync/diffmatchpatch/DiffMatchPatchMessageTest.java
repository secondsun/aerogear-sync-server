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
package org.jboss.aerogear.sync.diffmatchpatch;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffMatchPatchMessageTest {

    @Test (expected = NullPointerException.class)
    public void constructWithNullDocumentId() {
        patchMessage(null, "clientId", asQueue(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build()));
    }

    @Test (expected = NullPointerException.class)
    public void constructWithNullClientId() {
        patchMessage("docId", null, asQueue(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build()));
    }

    @Test (expected = NullPointerException.class)
    public void constructWithNullEdit() {
        patchMessage("docId", "clientId", null);
    }

    @Test
    public void equalsReflexsive() {
        final DiffMatchPatchMessage x = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        final DiffMatchPatchMessage y = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        assertThat(x, equalTo(y));
    }

    @Test
    public void equalsSymmetric() {
        final DiffMatchPatchMessage x = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        final DiffMatchPatchMessage y = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(x));
    }

    @Test
    public void equalsTransitive() {
        final DiffMatchPatchMessage x = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        final DiffMatchPatchMessage y = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        final DiffMatchPatchMessage z = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        assertThat(x, equalTo(y));
        assertThat(y, equalTo(z));
        assertThat(x, equalTo(z));
    }

    @Test
    public void equalsNull() {
        final DiffMatchPatchMessage x = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        assertThat(x.equals(null), is(false));
    }

    @Test
    public void nonEquals() {
        final DiffMatchPatchMessage x = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("lalle").build());
        final DiffMatchPatchMessage y = patchMessage(DiffMatchPatchEdit.withChecksum("bogus").unchanged("kalle").build());
        assertThat(x.equals(y), is(false));
    }

    private static DiffMatchPatchMessage patchMessage(DiffMatchPatchEdit edit) {
        return new DiffMatchPatchMessage("docId", "clientId", asQueue(edit));
    }

    private static DiffMatchPatchMessage patchMessage(final String documentId,
                                                      final String clientId,
                                                      final Queue<DiffMatchPatchEdit> edits) {
        return new DiffMatchPatchMessage(documentId, clientId, edits);
    }

    private static Queue<DiffMatchPatchEdit> asQueue(DiffMatchPatchEdit... edits) {
        return new LinkedList<DiffMatchPatchEdit>(Arrays.asList(edits));
    }

}
