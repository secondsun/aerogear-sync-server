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

    function getDocument( id ) {
        var xhr = xhrObject( 'GET', id );
        xhr.send( null );
        console.log( xhr.responseText );
        return Doc.fromJson( xhr.responseText );
    };

    function putDocument( id, content, rev ) {
        var xhr = xhrObject( 'PUT', id );
        xhr.send( JSON.stringify( {content: content, rev: rev} ) );
        return {status: xhr.status, doc: Doc.fromJson( xhr.responseText )};
    };

    function httpRequest( method, id, content ) {
        var xhr = xhrObject();
        xhr.send( JSON.stringify( {content: content} ) );
        return xhr;
    };

    function xhrObject( method, id ) {
        var xhr = new XMLHttpRequest();
        xhr.open( method, 'http://localhost:8080/' + id, false );
        xhr.setRequestHeader( 'Content-Type', 'application/json' );
        xhr.setRequestHeader( 'Accept', 'application/json' );
        return xhr;
    };

    function Doc( id, rev, content ) {
        this.id = id;
        this.rev = rev;
        this.content = content;
    };

    Doc.fromJson = function( str ) {
        var json = JSON.parse( str );
        return new Doc( json.id, json.rev, JSON.parse( json.content ) );
    }

    Doc.prototype = {
        toString: function() {
            return "[id=" + this.id + ", rev=" + this.rev + ", content=" + content + "]";
        }
    }

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
