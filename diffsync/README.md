## AeroGear Server Differential Synchronization Project
This implementation is based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) by Neil Fraser.
The server implementation is WebSocket based.

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

* [client-core](./server-core)  
Contains a concrete implementation of the client side core API. This implementation is inteded to be "embedded" in a client
component, like a Netty client, an Android device, etc.

* [server-netty](./server-netty)  
Contains a Netty server that uses the [server-core](./server-core).

* [itests](./itests)  
Contains tests that use both the [server-core](./server-core) and [client-core](./client-core) in cooperation.

### Usage

#### Starting the server
    cd server-netty
    mvn exec:exec -Pserver


#### Building/Testing
To build this project simply use the following maven command:

    mvn install

To run the the unit tests:

    mvn test

