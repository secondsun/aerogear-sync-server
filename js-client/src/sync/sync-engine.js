this.Sync = {}; 

Sync.Engine = function () {

    if ( ! ( this instanceof Sync.Engine ) ) {
        return new Sync.Engine();
    }   

    var docs = AeroGear.DataManager( { name: 'doc', recordId: 'id' } ).stores.doc;
    var shadows = AeroGear.DataManager( { name: 'shadow', recordId: 'id' } ).stores.shadow;
    var backups = AeroGear.DataManager( { name: 'backup', recordId: 'id' } ).stores.backup;
    var dmp = new diff_match_patch();

    /**
     * Adds a new document to this sync engine.
     *
     * @param doc the document to add.
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
     * @param doc the updated document.
     * @returns {object} containing the diffs that between the clientDoc and it's shadow doc.
     */
    this.diff = function( doc ) {
        var shadow = shadows.read( doc.id )[0];
        console.log("diff shadow : " + JSON.stringify( shadow.doc.content) );
        return dmp.diff_main( JSON.stringify( shadow.doc.content ), JSON.stringify( doc.content ) );
    };

    this.patch = function( edits ) {
    };

    var saveDocument = function( doc ) {
        console.log( 'saving doc: ' + JSON.stringify( doc) );
        docs.save( doc );
    };

    var saveShadow = function( doc ) {
        var shadow = { id: doc.id, serverVersion: 0, clientVersion: 0, doc: doc };
        console.log( 'saving shadow: ' + JSON.stringify( shadow ) );
        shadows.save( shadow );
    };

    var saveShadowBackup = function( doc ) {
        var backup = { id: doc.id, clientVersion: 0, doc: doc };
        console.log( 'saving backup: ' + JSON.stringify( backup ) );
        backups.save( backup );
    };

    this.getDocument = function( id ) {
        return docs.read( id )[0];
    };

};
