this.Sync = this.Sync || {};

Sync.Engine = function () {

    if ( ! ( this instanceof Sync.Engine ) ) {
        return new Sync.Engine();
    }

    var dm = AeroGear.DataManager( ['docs', 'shadows', 'backups', 'edits'] ), // Shouldn't depend on AG Datamaanger in the future
        dmp = new diff_match_patch();

    /**
     * Adds a new document to this sync engine.
     *
     * @param doc the document to add.
     */
    this.addDocument = function( doc ) {
        this._saveDocument( JSON.parse( JSON.stringify( doc ) ) );
        this._saveShadow( JSON.parse( JSON.stringify( doc ) ) );
        this._saveShadowBackup( JSON.parse( JSON.stringify( doc ) ) );
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
        return {
            msgType: 'patch',
            id: doc.id,
            clientId: shadow.clientId,
            edits: [{
                clientVersion: shadow.clientVersion,
                serverVersion: shadow.serverVersion,
                // currently not implemented but we probably need this for checking the client and server shadow are identical be for patching.
                checksum: '',
                diffs: this._asAeroGearDiffs( dmp.diff_main( JSON.stringify( shadow.doc.content ), JSON.stringify( doc.content ) ) )
            }]
        };
    };

    this.processServerEdits = function( edits ) {
        var doc = this._saveDocument( this.patchDocument( edits ) ),
            backup = this.getBackup( doc.id );

        this._saveShadow( this.patchShadow( edits ) );
        backup.content = doc.content;
        backup.clientVersion++;
        this._saveBackup( backup );
        //remove pending edits.
    };

    this._asAeroGearDiffs = function( diffs ) {
        return diffs.map(function( value ) {
            return {operation: this._asAgOperation( value[0] ), text: value[1] };
        }.bind(this));
    };

    this._asDiffMatchPathDiffs = function( diffs ) {
        return diffs.map(function ( value ) {
            return [this._asDmpOperation ( value.operation ), value.text];
        }.bind(this));
    };

    this._asDmpOperation = function( op ) {
        if ( op === 'DELETE' ) {
            return -1;
        } else if ( op === 'ADD' ) {
            return 1;
        }
        return 0;
    };

    this._asAgOperation = function( op ) {
        if ( op === -1 ) {
            return 'DELETE';
        } else if ( op === 1 ) {
            return 'ADD';
        }
        return "UNCHANGED";
    };

    this.patchShadow = function( patchMsg ) {
        var shadow = this.getShadow( patchMsg.docId ), edits = patchMsg.edits, i, patched;
        for ( i = 0; i < edits.length; i++ ) {
            var edit = edits[i];
            patched = this.applyEditsToShadow( edit, shadow );
            shadow.doc.content = patched[0];
            shadow.serverVersion++;
        }

        console.log(patched);
        return shadow;
    };

    this.applyEditsToShadow = function ( edits, shadow ) {
        var doc = JSON.stringify( shadow.doc.content),
            diffs = this._asDiffMatchPathDiffs( edits.diffs ),
            patches = dmp.patch_make( doc, diffs );

        return dmp.patch_apply( patches, doc );
    };

    this.patchDocument = function( patchMsg ) {
        console.log(patchMsg);
        var patches = this.applyEditsToDoc( patchMsg );
        var doc = this.getDocument( patchMsg.id );
        console.log('patches: ', patches);
        doc.content = patches[1][0];
        return doc;
    };

    this.patch = function( doc ) {
        var patchMsg = this.diff( doc );
        return this.applyEditsToDoc( patchMsg );
    };

    this.applyEditsToDoc = function ( patchMsg ) {
        var doc = JSON.stringify( this.getDocument( patchMsg.docId ).content);
        var edits = patchMsg.edits;
        var i;
        var patches;
        for (i = 0; i < edits.length; i++) {
            var edit = edits[i];
            var diffs = this._asDiffMatchPathDiffs( edit.diffs );
            var patches = dmp.patch_make( doc, diffs );
            patches.push( dmp.patch_apply( patches, doc ) );
        }
        console.log('patches:', patches);
        return patches;
    };

    this._saveDocument = function( doc ) {
        dm.stores.docs.save( doc );
    };

    this._saveShadow = function( doc ) {
        var shadow = { id: doc.id, serverVersion: 0, clientId: doc.clientId, clientVersion: 0, doc: doc };
        dm.stores.shadows.save( shadow );
    };

    this._saveShadowBackup = function( doc ) {
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
