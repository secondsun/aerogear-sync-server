## AeroGear Data Synchronization Project [![Build Status](https://travis-ci.org/aerogear/aerogear-sync-server.png)](https://travis-ci.org/aerogear/aerogear-sync-server)
This project aims to prototype a data synchronization protocol that can be used by AeroGear's client libraries.

AeroGear Sync consists of the following modules:


## AeroGear Server Differential Synchronization Project
This implementation is based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) by Neil Fraser.

### Resources
* [Differential Synchronization presentation](https://www.youtube.com/watch?v=S2Hp_1jqpY8)
* [Differential Synchronization paper](http://research.google.com/pubs/pub35605.html)
* [Differential Synchronization step by step keynote presentation](https://www.icloud.com/iw/#keynote/BAKHgqmqd5ETPe9ebKyBhSINoBo1QHaNPYeF/diffsync)

### Modules

* [api](./api)  
Contains the interfaces for the server and client, plus definitions of common types.

* [core](./core)  
Contains a base implementation of the common types from the [api](./api) module. These are used by both the
[client-core](./client-core) and the [server-core](./server-core) modules.

* [server-core](./server-core)  
Contains a concrete implementation of the server side core API. This implementation is inteded to be "embedded" in a server
component, like a Servlet, a Netty server, etc.

* [server-netty](./server-netty)  
Contains a Netty server that uses the [server-core](./server-core).

* [server-xmpp](./server-xmpp)  
Contains a Google Cloud Messaging XMPP implmentation that is used by the [server-netty](./server-netty) module.

* [client-core](./client-core)  
Contains a concrete implementation of the client side core API. This implementation is inteded to be "embedded" in a client
component, like a Netty client, an Android device, etc.

* [client-netty](./client-netty)  
A diffsync client that uses Netty.

* [client-xmpp](./client-xmpp)  
A diffsync client that uses Google Cloud Messaging XMPP.

* [itests](./itests)  
Contains tests that use both the [server-core](./server-core) and [client-core](./client-core) in cooperation.

* [common](./common)  
Contains [google-diff-match-patch](https://code.google.com/p/google-diff-match-patch/) with some minor tweaks.

* [js-client](./js-client)  
Contains a JavaScript client library and tests for verifiying the servers funtionality.
This will most likely be moved out of this project if we decide to move forward with it.

### Usage

#### Starting the server
    cd server-netty
    mvn exec:exec


#### Building/Testing
To build this project simply use the following maven command:

    mvn install

To run the the unit tests:

    mvn test


