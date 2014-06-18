(function() {

    module('Sync integration test');

    asyncTest('create document - content is an array', function() {
        var documentId = uuid();
        putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] ).then( function( response ) {
            start();
            equal( response.status, 200, 'Status should be 200' );
            equal( response.doc.id, documentId, 'id should match the sent path id parameter.' );
            equal( isArray( response.doc.content ), true, 'content is an array' );
            equal( response.doc.content.length, 2, 'content Array length is 2' );
            equal( response.doc.content[ 0 ].model, 'honda', 'model property should be honda' );
        }, function( errResponse ) {
            ok(false, errResponse);
        });
    });

    asyncTest('get document', function() {
        var documentId = uuid();
        putDocument( documentId, {model: 'ferrari'} ).then( function( response ) {
            return getDocument( response.doc.id );
        }).then( function ( response ) {
            start();
            equal( response.doc.id, documentId, 'id should match the sent path id parameter.' );
            equal( response.doc.content.model, 'ferrari', 'content.model should be ferrari' );
        }, function( erroResponse ) {
            ok(false, errResponse);
        });
    });

    asyncTest('update document', function() {
        var documentId = uuid(), revision;
        putDocument( documentId, {model: 'bmw'} ).then( function( response ) {
            return getDocument( response.doc.id );
        }).then( function ( response ) {
            response.doc.content.color = "black";
            revision = response.doc.rev;
            return putDocument( response.doc.id, response.doc.content, response.doc.rev );
        }).then( function ( response ) {
            start();
            equal( response.doc.id, documentId, 'id should match the sent path id parameter.');
            equal( Object.keys( response.doc.content ).length, 2, 'the document should have 2 keys' );
            equal( response.doc.content.color, 'black', 'color field should equal black' );
            notEqual( response.doc.rev, revision, 'revisions should be different' );
        }, function( erroResponse ) {
            ok(false, errResponse);
        });
    });


    asyncTest('update document - content is an array', function() {
        var documentId = uuid(), revision;
        putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] ).then( function( response ) {
            return getDocument( response.doc.id );
        }).then( function ( response ) {
            response.doc.content[ 0 ].color = "black";
            revision = response.doc.rev;
            return putDocument( response.doc.id, response.doc.content, response.doc.rev );
        }).then( function ( response ) {
            start();
            equal( response.doc.id, documentId, 'id should match the sent path id parameter.');
            equal( Object.keys( response.doc.content[ 0 ] ).length, 2, 'the document should have 2 keys' );
            equal( response.doc.content[ 0 ].color, 'black', 'color field should equal black' );
            notEqual( response.doc.rev, revision, 'revisions should be different' );
        }, function( erroResponse ) {
            ok(false, errResponse);
        });
    });

    asyncTest('update document with conflict', function() {
        var documentId = uuid(), oldRevision, updatedRevision;
        putDocument( documentId, {model: 'bmw'} ).then( function( response ) {
            return getDocument( response.doc.id );
        }).then( function ( response ) {
            oldRevision = response.doc.rev;
            response.doc.content.color = "black";
            return putDocument( response.doc.id, response.doc.content, response.doc.rev );
        }).then( function ( response ) {
            updatedRevision = response.doc.rev;
            response.doc.content.color = "pink";
            return putDocument( response.doc.id, response.doc.content, oldRevision);
        }).catch(function ( response ) {
            start();
            equal( response.status, 409, "Should return a 409 Conflict" );
            equal( response.doc.id, documentId, 'Conflicted document id should be the same ');
            equal( response.doc.rev, updatedRevision, 'The latest revision that the server has should be the lastest update we made');
        }, function( erroResponse ) {
            ok(false, errResponse);
        });
    });

    asyncTest('update document with conflict - content as an array', function() {
        var documentId = uuid(), oldRevision, updatedRevision;
        putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] ).then( function( response ) {
            return getDocument( response.doc.id );
        }).then( function ( response ) {
            oldRevision = response.doc.rev;
            response.doc.content[ 0 ].color = "black";
            return putDocument( response.doc.id, response.doc.content, response.doc.rev );
        }).then( function ( response ) {
            updatedRevision = response.doc.rev;
            response.doc.content[ 1 ].color = "blue";
            return putDocument( response.doc.id, response.doc.content, oldRevision);
        }).catch(function ( response ) {
            start();
            equal( response.status, 409, "Should return a 409 Conflict" );
            equal( response.doc.id, documentId, 'Conflicted document id should be the same ');
            equal( response.doc.rev, updatedRevision, 'The latest revision that the server has should be the lastest update we made');
        }, function( erroResponse ) {
            ok(false, errResponse);
        });
    });

    asyncTest('delete document', function() {
        var documentId = uuid(), revision;
        putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] ).then( function( response ) {
            revision = response.doc.rev;
            return deleteDocument( response.doc.id, response.doc.rev );
        }).then( function( response ) {
            start();
            equal( response.status, 200, 'Status should be 200' );
            notEqual( response.doc.rev, revision, 'Delete revision should be different' );
        });
    });

    asyncTest('try getting deleted document', function() {
        var documentId = uuid();
        putDocument( documentId, [ { model: 'honda' }, { model: 'bmw' } ] ).then( function( response ) {
            return deleteDocument( response.doc.id, response.doc.rev );
        }).then( function ( response ) {
            return getDocument( documentId );
        }).then(function( response ) {
            start();
            equal( response.status, 404, 'Status should be 404, not found' );
        });
    });

    function getDocument( id ) {
        var promise = new Promise(function( resolve, reject ) {
            var xhr = new XMLHttpRequest();
            xhr.open( 'GET', 'https://localhost:8080/docs/' + id, true );
            xhr.setRequestHeader( 'Accept', 'application/json' );
            xhr.onload = function ( e ) {
                if (xhr.readyState === 4) {
                    if ( xhr.status === 200 ) {
                        resolve({ status: xhr.status, doc: fromJson( xhr.responseText ) });
                    } else if ( xhr.status >= 400 && xhr.status != 409 ) {
                        resolve( { status: xhr.status, responseText: xhr.responseText } );
                    }
                }
            };
            xhr.send( null );
        });
        return promise;
    }

    function putDocument( id, content, rev ) {
        var promise = new Promise(function( resolve, reject ) {
            var xhr = new XMLHttpRequest();
            xhr.open( 'PUT', 'https://localhost:8080/docs/' + id, true );
            xhr.setRequestHeader( 'Content-Type', 'application/json' );
            xhr.setRequestHeader( 'Accept', 'application/json' );
            xhr.onload = function ( e ) {
                if (xhr.readyState === 4) {
                    if ( xhr.status === 200 ) {
                        resolve({ status: xhr.status, doc: fromJson( xhr.responseText ) });
                    } else if ( xhr.status == 409 ) {
                        reject( { status: xhr.status, doc: fromJson( xhr.responseText ) } );
                    } else if ( xhr.status >= 400 ) {
                        reject( JSON.stringify( { status: xhr.status, doc: fromJson( xhr.responseText ) } ) );
                    }
                }
            };
            xhr.send( JSON.stringify( {content: content, rev: rev} ) );
        });
        return promise;
    }

    function deleteDocument( id, rev ) {
        var promise = new Promise( function( resolve, reject ) {
            var xhr = new XMLHttpRequest();
            xhr.open( 'DELETE', 'https://localhost:8080/docs/' + id, true );
            xhr.setRequestHeader( 'Content-Type', 'application/json' );
            xhr.setRequestHeader( 'Accept', 'application/json' );
            xhr.onload = function ( e ) {
                if (xhr.readyState === 4) {
                    if( xhr.status >= 400) {
                        resolve( { status: xhr.status } );;
                    } else {
                        resolve( { status: xhr.status, doc: JSON.parse( xhr.responseText ) } );
                    }
                }
            };
            xhr.send( JSON.stringify( {rev: rev} ) );
        });
        return promise;
    }

    function fromJson( str ) {
        var json = JSON.parse( str );
        return { id: json.docId, rev: json.rev, content: JSON.parse( json.content ) };
    }

    function isArray( obj ) {
        return ({}).toString.call( obj ) === "[object Array]";
    }

    function uuid() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
