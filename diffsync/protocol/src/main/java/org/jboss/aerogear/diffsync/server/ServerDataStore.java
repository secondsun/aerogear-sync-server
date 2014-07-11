package org.jboss.aerogear.diffsync.server;

import org.jboss.aerogear.diffsync.DataStore;
import org.jboss.aerogear.diffsync.Document;

/**
 * A server side DataStore implementation is responible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 */
public interface ServerDataStore<T> extends DataStore<T> {

    /**
     * Saves a server document.
     *
     * @param document the {@link Document} to save.
     */
    void saveDocument(Document<T> document);

    /**
     * Retrieves the {@link Document} matching the passed-in document documentId.
     *
     * @param documentId the document identifier of the shadow document.
     * @return {@link Document} the document matching the documentId.
     */
    Document<T> getDocument(String documentId);

}
