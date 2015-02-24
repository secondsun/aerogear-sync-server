### Modules
* [client-engine](./client-engine)  
Contains a concrete implementation of the client side core API. This implementation is inteded to be "embedded" in a client
component, like a Netty client, an Android device, etc.

* [client-netty](./client-netty)  
A client that embeds the _client-engine_ and provides network access to the Sync Server using WebSockets.


