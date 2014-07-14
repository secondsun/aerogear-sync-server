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
package org.jboss.aerogear.diffsync.client;

import org.jboss.aerogear.diffsync.ClientDocument;
import org.jboss.aerogear.diffsync.Document;
import org.jboss.aerogear.diffsync.Edit;
import org.jboss.aerogear.diffsync.ShadowDocument;

/**
 * A instance of this class will be able to handle tasks needed to implement
 * Differential Synchronization for a specific type of documents.
 *
 * @param <T> The type of documents that this engine can handle.
 */
public interface ClientSynchronizer<T> {

    /**
     * Called when the shadow should be patched. Is called when an update is recieved.
     *
     * @param edit The edits.
     * @return {@link ShadowDocument} a new patched shadow document.
     */
    ShadowDocument<T> patchShadow(Edit edit, ShadowDocument<T> shadowDocument);

    /**
     * Called when the document should be patched.
     *
     * @param edit the edit to use to patch the client document.
     * @param document the client document that
     * @return {@link ClientDocument} a new patched document.
     */
    ClientDocument<T> patchDocument(Edit edit, ClientDocument<T> document);

    /**
     * The first step in a sync is to produce a an edit for the changes.
     * The produced edit can then be sent to the opposing side perform an update/sync.
     *
     * @param document the document containing
     * @param shadowDocument the document shadow.
     * @return {@link Edit} the edit representing the diff between the document and it's shadow document.
     */
    Edit diff(Document<T> document, ShadowDocument<T> shadowDocument);

}
