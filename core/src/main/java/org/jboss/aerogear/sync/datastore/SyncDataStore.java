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
package org.jboss.aerogear.sync.datastore;

import org.jboss.aerogear.sync.ConflictException;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.DocumentNotFoundException;

/**
 * Is responsible handling synchronization of documents.
 */
public interface SyncDataStore {

    /**
     * Read a specific revision of a document.
     *
     * @param id the documents identifier.
     * @param revision the document revision to read.
     * @return {@link org.jboss.aerogear.sync.Document} the document matching the documentId and revision.
     * @throws DocumentNotFoundException if the document could not be found.
     */
    Document read(String id, String revision) throws DocumentNotFoundException;

    /**
     * Read the latest revision of a document.
     *
     * @param id the documents identifier.
     * @return {@link org.jboss.aerogear.sync.Document} the document matching the documentId.
     * @throws DocumentNotFoundException if the document could not be found.
     */
    Document read(String id) throws DocumentNotFoundException;

    /**
     * Creates a new document with the documentId and json passed in.
     *
     * @param id the new documents identifier.
     * @param json the contents of the new document
     * @return {@link org.jboss.aerogear.sync.Document} the new document.
     */
    Document create(String id, String json);

    /**
     * Updates the document passed in
     *
     * @param doc the document containing the new update.
     * @return {@link org.jboss.aerogear.sync.Document} the newly updated document.
     * @throws ConflictException if a newer revision of this document already exists.
     */
    Document update(Document doc) throws ConflictException;

    /**
     * Deletes (marks for deletion) the docuement with the specified revision.
     *
     * @param id the documents identifier.
     * @param revision the document revision to delete.
     * @return {@code String} the revision for the deleted document.
     */
    String delete(String id, String revision);

}
