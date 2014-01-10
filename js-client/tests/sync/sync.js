(function() {

    module('Sync integration test');

    test('create document', function() {
        var documentId = uuid();
        var response = putDocument( documentId, {model: 'honda'} );
        equal( response.status, 200, 'Status should be 200' );
        equal( response.doc.id, documentId, 'id should match the sent path id parameter.' );
        equal( response.doc.content.model, 'honda', 'model property should be honda' );
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

    function getDocument( id ) {
        var xhr = xhrObject( 'GET', id );
        xhr.send( null );
        return fromJson( xhr.responseText );
    }

    function putDocument( id, content, rev ) {
        var xhr = xhrObject( 'PUT', id );
        xhr.send( JSON.stringify( {content: content, rev: rev} ) );
        if( xhr.status >= 400 && xhr.status != 409 ) {
            return { status: xhr.status };
        }
        return { status: xhr.status, doc: fromJson( xhr.responseText ) };
    }

    function httpRequest( method, id, content ) {
        var xhr = xhrObject();
        xhr.send( JSON.stringify( {content: content} ) );
        return xhr;
    }

    function xhrObject( method, id ) {
        var xhr = new XMLHttpRequest();
        xhr.open( method, 'http://localhost:8080/' + id, false );
        xhr.setRequestHeader( 'Content-Type', 'application/json' );
        xhr.setRequestHeader( 'Accept', 'application/json' );
        return xhr;
    }

    function fromJson( str ) {
        var json = JSON.parse( str );
        return { id: json.id, rev: json.rev, content: JSON.parse( json.content ) };
    }

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
