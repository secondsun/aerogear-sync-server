## AeroGear Data Synchronization Project
Data synchronziation is an important feature for mobile clients to enable offline working. Mobile devices can
work without a connection, and later when a connection becomes available, any changes made by the client should be
synchronized with the source of the data.
This project aims to specify a data synchronization protocol that can be used by AeroGear's client libraries.

### Prerequisites
This project currently requires [CouchDB](http://couchdb.apache.org/), and at the moment requires 5.0.0.Alpha2-SNAPSHOT
build of [Netty](https://github.com/netty/netty).

AeroGear Sync consists of the following modules:

* [common](https://github.com/aerogear/aerogear-sync-server/tree/master/common)  
Contains [google-diff-match-patch](https://code.google.com/p/google-diff-match-patch/) with some minor tweaks.  

* [js-client](https://github.com/aerogear/aerogear-sync-server/tree/master/js-client)  
Contains a JavaScript client library and tests for verifiying the servers funtionality. This will most likely be moved
out of this project if we decide to move forward with it.  

* [protocol](https://github.com/aerogear/aerogear-sync-server/tree/master/protocol)  
This module currently only contains the API for the Differential Synchronization implementation.  

* [core](https://github.com/aerogear/aerogear-sync-server/tree/master/core)  
This module contains the core classes for data synchronization. These are intended to be used by server implementation.

* [server-netty](https://github.com/aerogear/aerogear-sync-server/tree/master/server-netty)  
Netty based server implementations. Please refer to the README.md for details about how to build and run the servers,
as well as information about the various implementations.


