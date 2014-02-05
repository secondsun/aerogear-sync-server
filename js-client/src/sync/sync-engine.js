this.Sync = {}; 

Sync.Engine = function () {

    if ( ! ( this instanceof Sync.Engine ) ) {
        return new Sync.Engine();
    }   

    /**
     * Adds a new document to this sync engine.
     *
     * @param document the document to add.
     */
    this.addDocument = function( doc ) {
        saveDocument( doc );
        saveShadow( doc );
        saveShadowBackup( doc );
    };

    /**
     * Performs the client side of a differential sync.
     * When a client makes an update to it's document, it is first diffed against the shadow
     * document. The result of this is an {@link Edits} instance representing the changes.
     * There might be pending edits that represent edits that have not made it to the server
     * for some reason (for example packet drop). If a pending edit exits the contents (the diffs)
     * of the pending edit will be included in the returned Edits from this method.
     *
     * @param document the updated document.
     * @returns {object} containing the diffs that between the clientDoc and it's shadow doc.
     */
    this.diff = function( clientDoc ) {

    };

    this.patch = function( edits ) {

    };

    var saveDocument = function( doc ) {
        console.log( 'save doc: ' + doc );
    };

    var saveShadow = function( doc ) {
        console.log( 'save doc: ' + doc );
    };

    var saveShadowBackup = function( doc ) {
        console.log( 'save doc: ' + doc );
    };
};
