this.Sync = {}; 

Sync.Client = function ( config ) {
    var sendQueue = []; 
    var ws;

    if ( ! ( this instanceof Sync.Client ) ) {
        return new Sync.Client( config );
    }   

    if ( config.serverUrl === undefined ) {
        throw new Error("'config.serverUrl' must be specified" );
    }

    ws = new WebSocket( config.serverUrl );
    ws.onopen = function ( e ) {
        console.log ( 'WebSocket opened' );
        while ( sendQueue.length ) {
            send ( 'add', sendQueue.pop() );
        }
    };
    ws.onmessage = function( e ) {
        config.onmessage( e );
    }
    ws.onerror = function( e ) {
        if ( config.onerror === undefined ) {
            console.log ( 'Error: ' + e );
        } else {
            config.onerror ( e );
        }
    };
    ws.onclose = function( e ) {
        if ( config.onclose === undefined ) {
            console.log ( 'Close [code=' + e.code + ', reason=' + e.reason + ', wasClean=' + e.wasClean + ']' );
        } else {
            config.onclose ( e );
        }
    }

    this.addDoc = function( doc ) {
        if ( ws.readyState === 0 ) {
            sendQueue.push( doc );
        } else if ( ws.readyState === 1 ) {
            send( 'add', doc );
        }
    };  

    this.disconnect = function () {
        ws.close();
    }

    this.removeDoc = function( doc ) {
        console.log( "removing  doc from engine" );
    };  

    var send = function ( msgType, doc ) {
        var json = { msgType: msgType, docId: doc.docId, clientId: doc.clientId, content: doc.content };
        console.log ( 'sending ' + JSON.stringify ( json ) );
        ws.send( JSON.stringify ( json ) );
    };

};
