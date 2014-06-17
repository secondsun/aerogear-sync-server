package org.jboss.aerogear.sync.diffsync.client;

import org.jboss.aerogear.sync.diffsync.ClientDocument;
import org.jboss.aerogear.sync.diffsync.DataStore;
import org.jboss.aerogear.sync.diffsync.Document;

/**
 * A client side DataStore implementation is responible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 */
public interface ClientDataStore<T> extends DataStore<T> {

    /**
     * Saves a client document document.
     *
     * @param document the {@link Document} to save.
     */
    void saveClientDocument(ClientDocument<T> document);

    /**
     * Retrieves the {@link Document} matching the passed-in document documentId.
     *
     * @param clientId the client identifier for which to fetch the document.
     * @param documentId the document identifier of the shadow document.
     * @return {@link Document} the document matching the documentId.
     */
    ClientDocument<T> getClientDocument(String clientId, String documentId);

}
