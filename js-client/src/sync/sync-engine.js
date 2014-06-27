this.Sync = {}; 

Sync.Engine = function () {

    if ( ! ( this instanceof Sync.Engine ) ) {
        return new Sync.Engine();
    }   

    var dm = AeroGear.DataManager( ['docs', 'shadows', 'backups', 'edits'] );
    var dmp = new diff_match_patch();

    /**
     * Adds a new document to this sync engine.
     *
     * @param doc the document to add.
     */
    this.addDocument = function( doc ) {
        var clonedDoc = JSON.parse( JSON.stringify( doc ) );
        saveDocument( clonedDoc );
        saveShadow( clonedDoc );
        saveShadowBackup( clonedDoc );
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
        var shadow = dm.stores.shadows.read( doc.id )[0];
        return { msgType: 'edits',
            docId: doc.id,
            clientId: shadow.clientId,
            version: shadow.clientVersion,
            // currently not implemented but we probably need this for checking the client and server shadow are identical be for patching.
            checksum: '', 
            diffs: asAeroGearDiffs( dmp.diff_main( JSON.stringify( shadow.doc.content ), JSON.stringify( doc.content ) ) )
        };
    };

    this.processServerEdits = function( edits ) {
        saveShadow( this.patchShadow( edits ) );
        var doc = this.saveDocument( this.patchDocument( edits ) );
        var backup = this.getBackup( doc.id );
        backup.content = doc.content;
        backup.clientVersion++;
        saveBackup( backup );
        //remove pending edits.
    };

    function asAeroGearDiffs( diffs ) {
        var agDiffs = [];
        diffs.forEach(function addDiff( value ) {
            agDiffs.push( {operation: asAgOperation( value[0] ), text: value[1] } );
        });
        return agDiffs;
    };

    function asDiffMatchPathDiffs( diffs ) {
        return diffs.map(function ( value ) {
            return [asDmpOperation ( value.operation ), value.text];
        });
    };

    function asDmpOperation( op ) {
        if ( op === 'DELETE' ) {
            return -1;
        } else if ( op === 'ADD' ) {
            return 1;
        } 
        return 0;
    }

    function asAgOperation( op ) {
        if ( op === -1 ) {
            return 'DELETE';
        } else if ( op === 1 ) {
            return 'ADD';
        }
        return "UNCHANGED";
    }

    this.patchShadow = function( edits ) {
        var shadow = this.getShadow( edits.docId );
        var patched = this.applyEditsToShadow( edits, shadow );
        shadow.doc.content = patched[0];
        shadow.serverVersion = edits.version;
        return shadow;
    };

    this.applyEditsToShadow = function ( edits, shadow ) {
        var doc = JSON.stringify( shadow.doc.content);
        var diffs = asDiffMatchPathDiffs( edits.diffs );
        var patches = dmp.patch_make( doc, diffs );
        return dmp.patch_apply( patches, doc );
    };

    this.patchDocument = function( edits ) {
        var patched = this.applyEditsToDoc( edits );
        var doc = this.getDocument( edits.docId );
        doc.content = patched[0];
        return doc;
    };

    this.patch = function( doc ) {
        var edits = this.diff( doc );
        return this.applyEditsToDoc( edits );
    }

    this.applyEditsToDoc = function ( edits ) {
        var doc = JSON.stringify( this.getDocument( edits.docId ).content);
        var diffs = asDiffMatchPathDiffs( edits.diffs );
        var patches = dmp.patch_make( doc, diffs );
        return dmp.patch_apply( patches, doc );
    };

    var saveDocument = function( doc ) {
        dm.stores.docs.save( doc );
    };

    var saveShadow = function( doc ) {
        var shadow = { id: doc.id, serverVersion: 0, clientId: doc.clientId, clientVersion: 0, doc: doc };
        dm.stores.shadows.save( shadow );
    };

    var saveShadowBackup = function( doc ) {
        var backup = { id: doc.id, clientVersion: 0, doc: doc };
        dm.stores.backups.save( backup );
    };

    this.getDocument = function( id ) {
        return dm.stores.docs.read( id )[0];
    };

    this.getShadow = function( id ) {
        return dm.stores.shadows.read( id )[0];
    };

    this.getBackup = function( id ) {
        return dm.stores.backups.read( id )[0];
    };

};
