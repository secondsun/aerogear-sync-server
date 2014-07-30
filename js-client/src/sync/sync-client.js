this.Sync = this.Sync || {};

Sync.Client = function ( config ) {
    if ( ! ( this instanceof Sync.Client ) ) {
        return new Sync.Client( config );
    }

    var sendQueue = [], ws;
    var that = this;
    var syncEngine = config.syncEngine || new Sync.Engine();

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

            console.log ( 'WebSocket opened. SendQueue.length=', sendQueue.length );
            while ( sendQueue.length ) {
                var task = sendQueue.pop();
                if ( task.type === 'add' ) {
                    send ( task.type, task.msg );
                } else {
                    that.sendEdits( task.msg );
                }
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
    };

    this.patch = function( data ) {
        syncEngine.patch( data );
    };

    this.getDocument = function( id ) {
        return syncEngine.getDocument( id );
    };

    this.diff = function( data ) {
        return syncEngine.diff( data );
    };

    this.addDocument = function( doc ) {
        syncEngine.addDocument( doc );

        if ( ws.readyState === 0 ) {
            sendQueue.push( { type: 'add', msg: doc } );
        } else if ( ws.readyState === 1 ) {
            send( 'add', doc );
        }
    };

    this.sendEdits = function( edit ) {
        if ( ws.readyState === WebSocket.OPEN ) {
            console.log( 'sending edits:', edit );
            ws.send( JSON.stringify( edit ) );
        } else {
            console.log("Client is not connected. Add edit to queue");
            if ( sendQueue.length === 0 ) {
                sendQueue.push( { type: 'patch', msg: edit } );
            } else {
                var updated = false;
                for (var i = 0 ; i < sendQueue.length; i++ ) {
                    var task = sendQueue[i];
                    if (task.type === 'patch' && task.msg.clientId === edit.clientId && task.msg.id === edit.id) {
                        for (var j = 0 ; j < edit.edits.length; j++) {
                            task.msg.edits.push( edit.edits[j] );
                        }
                        updated = true;
                    }
                }
                if ( !updated ) {
                    sendQueue.push( { type: 'patch', msg: edit } );
                }
            }
        }
    };

    this.disconnect = function () {
        ws.close();
    };

    this.removeDoc = function( doc ) {
        console.log( "removing  doc from engine" );
    };

    this.update = function( docId ) {
        if ( sendQueue.length === 0 ) {
            var doc = syncEngine.getDocument( docId );
            var edits = syncEngine.diff( doc );
            that.sendEdits( edits );
        } else {
            while ( sendQueue.length ) {
                var task = sendQueue.shift();
                if ( task.type === 'add' ) {
                    send ( task.type, task.msg );
                } else {
                    that.sendEdits( task.msg );
                }
            }
        }
    };

    var send = function ( msgType, doc ) {
        var json = { msgType: msgType, id: doc.id, clientId: doc.clientId, content: doc.content };
        console.log ( 'sending ' + JSON.stringify ( json ) );
        ws.send( JSON.stringify ( json ) );
    };

};
