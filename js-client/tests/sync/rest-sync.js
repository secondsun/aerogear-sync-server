(function() {

    module('Sync integration test');

    asyncTest('create document', function() {
        var documentId = uuid();
        var content = {model: 'honda'};
        var rev = 0;
        //var response = putDocument( documentId, {model: 'honda'} );
        var xhr = new XMLHttpRequest();
        //xhr.withCredentials = true;
        xhr.open( 'GET', 'https://localhost:8080/', true );
        xhr.timeout = 99999999999999;
        xhr.ontimeout = function () {
            console.log ( 'request timed out!' );
        };
        //xhr.open( 'PUT', 'https://localhost:8080/' + documentId, true );
        xhr.setRequestHeader( 'Content-Type', 'application/json' );
        //xhr.setRequestHeader( 'Accept', 'application/json' );
        xhr.onreadystatechange = function( e ){
            if ( this.readyState === 4 ) {
                console.log ( 'status = ' + this.status );
                console.log ( 'responseText = ' + this.responseText );
                if ( this.status === 200 ) {
                    //return { status: xhr.status, doc: fromJson( xhr.responseText ) };
                    //return { status: this.status, doc: fromJson( this.responseText ) };
                    ok ( xhr );
                } else if ( this.status >= 400 && this.status != 409 ) {
                    ok ( xhr );
                    //return { status: xhr.status };
                }
                start();
            } else {
                console.log ( 'readState was not 4, but was: ' + this.readyState );
            }
        };
        //xhr.send( null );
        xhr.send( JSON.stringify( {content: content, rev: rev} ) );
        //console.log ( 'response = ' + response );
        console.log ( 'send PUT. Now wait for response.' );
        //alert ("bajj");
        //equal( response.status, 200, 'Status should be 200' );
        //equal( response.doc.id, documentId, 'id should match the sent path id parameter.' );
        //equal( response.doc.content.model, 'honda', 'model property should be honda' );
    });

    /*

    test('create document - content is an array', function() {
        var documentId = uuid();
        var response = putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] );
        equal( response.status, 200, 'Status should be 200' );
        equal( response.doc.id, documentId, 'id should match the sent path id parameter.' );
        equal( isArray( response.doc.content ), true, 'content is an array' );
        equal( response.doc.content.length, 2, 'content Array length is 2' );
        equal( response.doc.content[ 0 ].model, 'honda', 'model property should be honda' );
    });

    test('get document', function() {
        var documentId = uuid(),
            getDoc;
        putDocument( documentId, {model: 'ferrari'} );
        getDoc = getDocument( documentId );
        equal( getDoc.id, documentId, 'id should match the sent path id parameter.' );
        equal( getDoc.content.model, 'ferrari', 'id should match the sent path id parameter.' );
    });

    test('update document', function() {
        var documentId = uuid(),
            putDoc,
            getDoc,
            updatedDoc;
        putDoc = putDocument( documentId, { model: 'bmw' } );
        getDoc = getDocument( documentId );

        getDoc.content.color = "black";

        updatedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );

        equal( updatedDoc.doc.id, documentId, 'id should match the sent path id parameter.');
        equal( Object.keys( updatedDoc.doc.content ).length, 2, 'the document should have 2 keys' );
        equal( updatedDoc.doc.content.color, 'black', 'color field should equal black' );
        notEqual( updatedDoc.doc.rev, getDoc.rev, 'revisions should be different' );
    });

    test('update document - content is an array', function() {
        var documentId = uuid(),
            putDoc,
            getDoc,
            updatedDoc;
        putDoc = putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] );
        getDoc = getDocument( documentId );

        getDoc.content[ 0 ].color = "black";

        updatedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );

        equal( updatedDoc.doc.id, documentId, 'id should match the sent path id parameter.');
        equal( Object.keys( updatedDoc.doc.content[ 0 ] ).length, 2, 'the document should have 2 keys' );
        equal( updatedDoc.doc.content[ 0 ].color, 'black', 'color field should equal black' );
        notEqual( updatedDoc.doc.rev, getDoc.rev, 'revisions should be different' );
    });

    test('update document with conflict', function() {
        var documentId = uuid(),
            putDoc,
            getDoc,
            updatedDoc,
            conflictUpdatedDoc;
        // Create a new Document
        putDoc = putDocument( documentId, { model: 'bmw' } );
        // Get it
        getDoc = getDocument( documentId );

        getDoc.content.color = "black";
        // Update it to get a new revision
        updatedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );

        getDoc.content.color = "blue";

        // Update it again with an old revision number
        conflictedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );
        equal( conflictedDoc.status, 409, "Should return a 409 Conflict" );
        equal( conflictedDoc.doc.id, documentId, 'Conflicted document id should be the same ');
        equal( conflictedDoc.doc.rev, updatedDoc.doc.rev, 'The latest revision that the server has should be the lastest update we made');
    });

    test('update document with conflict - content as an array', function() {
        var documentId = uuid(),
            putDoc,
            getDoc,
            updatedDoc,
            conflictUpdatedDoc;
        putDoc = putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] );
        getDoc = getDocument( documentId );

        getDoc.content[ 0 ].color = "black";

        updatedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );

        getDoc.content[ 1 ].color = "blue";

        // Update it again with an old revision number
        conflictedDoc = putDocument( getDoc.id, getDoc.content, getDoc.rev );
        equal( conflictedDoc.status, 409, "Should return a 409 Conflict" );
        equal( conflictedDoc.doc.id, documentId, 'Conflicted document id should be the same ');
        equal( conflictedDoc.doc.rev, updatedDoc.doc.rev, 'The latest revision that the server has should be the lastest update we made');
    });

    test('delete document', function() {
        var documentId = uuid();
        var putDoc = putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] );
        var response = deleteDocument( documentId, putDoc.doc.rev);
        equal( response.status, 200, 'Status should be 200' );
        notEqual( response.doc.rev, putDoc.doc.rev, 'Delete revision should be different' );
    });

    test('try getting deleted document', function() {
        var documentId = uuid();
        var putDoc = putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] );
        var response = deleteDocument( documentId, putDoc.doc.rev);

        var getDoc = getDocument( documentId );
        equal( getDoc.status, 404, 'Status should be 404, not found' );
    });
    */
   /*

    function getDocument( id ) {
        var xhr = xhrObject( 'GET', id );
        xhr.send( null );
        if( xhr.status >= 400 ) {
            return { status: xhr.status };
        }
        return fromJson( xhr.responseText );
    }

    function putDocument( id, content, rev ) {
        var xhr = new XMLHttpRequest();
        console.log ( 'doing the ascync thing' );
        xhr.open( 'PUT', 'https://localhost:8080/docs' + id, true );
        xhr.setRequestHeader( 'Content-Type', 'application/json' );
        xhr.setRequestHeader( 'Accept', 'application/json' );

        xhr.onload = function ( e ) {
            console.log ( 'xhrReadyState = ' + xhr.readyState );
            console.log ( 'responseText = ' + xhr.responseText );
            if (xhr.readyState === 4) {
                console.log ( 'status = ' + xhr.status );
                if ( xhr.status === 200 ) {
                    start();
                    return { status: xhr.status, doc: fromJson( xhr.responseText ) };
                } else if ( xhr.status >= 400 && xhr.status != 409 ) {
                    start();
                    return { status: xhr.status };
                }
            }
        };
        xhr.send( JSON.stringify( {content: content, rev: rev} ) );
    }

    function deleteDocument( id, rev ) {
        var xhr = xhrObject( 'DELETE', id );
        xhr.send( JSON.stringify( {rev: rev} ) );
        if( xhr.status >= 400) {
            return { status: xhr.status };
        }
        return { status: xhr.status, doc: JSON.parse( xhr.responseText ) };
    }

    function httpRequest( method, id, content ) {
        var xhr = xhrObject();
        xhr.send( JSON.stringify( {content: content} ) );
        return xhr;
    }

    function xhrObject( method, id ) {
        var xhr = new XMLHttpRequest();
        xhr.open( method, 'https://localhost:8080/docs' + id, true );
        xhr.setRequestHeader( 'Content-Type', 'application/json' );
        xhr.setRequestHeader( 'Accept', 'application/json' );
        xhr.onload = function ( e ) {
            if (xhr.readyState === 4) {
                start();
                return xhr;
            };
        };
    }

    function fromJson( str ) {
        var json = JSON.parse( str );
        return { id: json.docId, rev: json.rev, content: JSON.parse( json.content ) };
    }

    function isArray( obj ) {
        return ({}).toString.call( obj ) === "[object Array]";
    }
    */

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
