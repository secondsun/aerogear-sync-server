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
        var shadow = shadows.read( doc.id )[0];
        return { msgType: 'edits',
            docId: doc.id,
            clientId: shadow.clientId,
            version: shadow.clientVersion,
            // currently not implemented but we probably need this for checking the client and server shadow are identical be for patching.
            checksum: '', 
            diffs: asAeroGearDiffs( dmp.diff_main( JSON.stringify( shadow.doc.content ), JSON.stringify( doc.content ) ) )
        };
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

    this.patch = function( doc ) {
        var edits = this.diff( doc );
        return this.applyEdits( edits );
    }

    this.applyEdits = function ( edits ) {
        var doc = JSON.stringify( this.getDocument( edits.docId ).content);
        var diffs = asDiffMatchPathDiffs( edits.diffs );
        var patches = dmp.patch_make( doc, diffs );
        return dmp.patch_apply( patches, doc );
    };

    var saveDocument = function( doc ) {
        docs.save( doc );
    };

    var saveShadow = function( doc ) {
        var shadow = { id: doc.id, serverVersion: 0, clientId: doc.clientId, clientVersion: 0, doc: doc };
        shadows.save( shadow );
    };

    var saveShadowBackup = function( doc ) {
        var backup = { id: doc.id, clientVersion: 0, doc: doc };
        backups.save( backup );
    };

    this.getDocument = function( id ) {
        return docs.read( id )[0];
    };

};
