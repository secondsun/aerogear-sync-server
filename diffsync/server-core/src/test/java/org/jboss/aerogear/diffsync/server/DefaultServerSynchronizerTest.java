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

import org.jboss.aerogear.diffsync.ClientDocument;
import org.jboss.aerogear.diffsync.DefaultClientDocument;
import org.jboss.aerogear.diffsync.DefaultDocument;
import org.jboss.aerogear.diffsync.DefaultShadowDocument;
import org.jboss.aerogear.diffsync.Diff.Operation;
import org.jboss.aerogear.diffsync.Document;
import org.jboss.aerogear.diffsync.Edit;
import org.jboss.aerogear.diffsync.ShadowDocument;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultServerSynchronizerTest {

    @Test
    public void clientDiff() throws Exception {
        final ServerSynchronizer<String> synchronizer = new DefaultServerSynchronizer();
        final Document<String> document = new DefaultDocument<String>("1234", "test");
        final ClientDocument<String> clientDocument = new DefaultClientDocument<String>("1234", "client1", "testing");
        final ShadowDocument<String> shadowDocument = new DefaultShadowDocument<String>(0L,  0L, clientDocument);

        final Edit edit = synchronizer.clientDiff(document, shadowDocument);
        assertThat(edit.clientId(), equalTo("client1"));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.diffs().size(), is(2));
        assertThat(edit.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diffs().get(0).text(), is("test"));
        assertThat(edit.diffs().get(1).operation(), is(Operation.ADD));
        assertThat(edit.diffs().get(1).text(), is("ing"));
    }
}
