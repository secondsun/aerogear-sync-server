this.Sync = this.Sync || {};

Sync.Client = function ( config ) {
    if ( ! ( this instanceof Sync.Client ) ) {
        return new Sync.Client( config );
    }

    var sendQueue = [], ws;

    config = config || {};

    if ( config.serverUrl === undefined ) {
        throw new Error("'config.serverUrl' must be specified" );
    }

    this.connect = function() {
        ws = new WebSocket( config.serverUrl );
        ws.onopen = function ( e ) {
            if ( config.onopen ) {
                config.onopen.apply( this, arguments );
            }

            console.log ( 'WebSocket opened' );

            while ( sendQueue.length ) {
                send ( 'add', sendQueue.pop() );
            }
        };
        ws.onmessage = function( e ) {
            if( config.onmessage ) {
                config.onmessage.apply( this, arguments );
            }
        };
        ws.onerror = function( e ) {
            if ( config.onerror ) {
                config.onerror.apply( this, arguments );
            } else {
                console.log ( 'Error: ' + e );
            }
        };
        ws.onclose = function( e ) {
            if ( config.onclose ) {
                 config.onclose.apply( this, arguments);
            } else {
                console.log ( 'Close [code=' + e.code + ', reason=' + e.reason + ', wasClean=' + e.wasClean + ']' );
            }
        };
    }
    this.connect();

    this.disconnect = function() {
        console.log('Closing Connection');
        ws.close();
    }

    this.addDoc = function( doc ) {
        if ( ws.readyState === 0 ) {
            sendQueue.push( doc );
        } else if ( ws.readyState === 1 ) {
            send( 'add', doc );
        }
    };

    this.sendEdit = function( edit ) {
        if ( ws.readyState === WebSocket.OPEN ) {
            ws.send( JSON.stringify( edit ) );
        } else {
            console.log("Client is not connected");
        }
    };

    this.disconnect = function () {
        ws.close();
    };

    this.removeDoc = function( doc ) {
        console.log( "removing  doc from engine" );
    };

    var send = function ( msgType, doc ) {
        var json = { msgType: msgType, id: doc.id, clientId: doc.clientId, content: doc.content };
        console.log ( 'sending ' + JSON.stringify ( json ) );
        ws.send( JSON.stringify ( json ) );
    };

};
