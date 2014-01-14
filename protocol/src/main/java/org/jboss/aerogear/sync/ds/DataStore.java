package org.jboss.aerogear.sync.ds;

/**
 * A DataStore implementation is responible for storing and serving data for a
 * Differential Synchronization implementation.
 *
 * @param <T> The type of the Document that this data store can handle.
 */
public interface DataStore<T> {

    /**
     * Saves a shadow document.
     *
     * @param shadowDocument the {@link ShadowDocument} to save.
     */
    void saveShadowDocument(ShadowDocument<T> shadowDocument);

    /**
     * Retrieves the {@link ShadowDocument} matching the passed-in document documentId.
     *
     * @param clientId the client for which to retrieve the shadow document.
     * @param documentId the document id of the shadow document.
     * @return {@link ShadowDocument} the shadow document matching the documentId.
     */
    ShadowDocument<T> getShadowDocument(String clientId, String documentId);

    /**
     * Saves a backup shadow document
     *
     * @param backupShadow the {@link BackupShadowDocument} to save.
     */
    void saveBackupShadowDocument(BackupShadowDocument<T> backupShadow);

    /**
     * Retrieves the {@link BackupShadowDocument} matching the passed-in document documentId.
     *
     * @param clientId the client identifier for which to fetch the document.
     * @param documentId the document identifier of the backup shadow document.
     * @return {@link ShadowDocument} the backup shadow document matching the documentId.
     */
    BackupShadowDocument<T> getBackupShadowDocument(String clientId, String documentId);

    /**
     * Saves a shadow document.
     *
     * @param document the {@link Document} to save.
     */
    void saveDocument(ClientDocument<T> document);

    /**
     * Retrieves the {@link Document} matching the passed-in document documentId.
     *
     * @param clientId the client identifier for which to fetch the document.
     * @param documentId the document identifier of the shadow document.
     * @return {@link Document} the document matching the documentId.
     */
    ClientDocument<T> getDocument(String clientId, String documentId);

    /**
     * Saves edits for the document.
     *
     * @param document the document identifier that the edits instance belongs to.
     */
    void saveEdits(final Edits edits, final ClientDocument<T> document);

    /**
     * Retreives the {@link Edits} for the specified document documentId.
     *
     * @param clientId the client identifier for which to fetch the document.
     * @param documentId the document identifier of the edit.
     * @return {@link Edits} the edit for the document.
     */
    Edits getEdit(String clientId, String documentId);

}
