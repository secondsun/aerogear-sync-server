## XMPP Differential Synchronization Server Implementation
This module contains a server implementations that uses the [server-core](../server-core)

### DiffSyncServer
The [DiffSyncServer](./src/main/java/org/jboss/aerogear/diffsync/DiffSyncServer.java) is a standalone server implementation
that can be started using Maven or directly in an IDE.

### Setup

 * Retrieve a SenderID and API key from [Google](https://developer.android.com/google/gcm/gs.html) and set the fields SENDER_ID and API_KEY in DiffSyncServer.java
 
#### Starting using Maven

    mvn exec:exec

The server can be configured using [sync.config](./src/main/resources/sync.config]


### DiffSyncHandler
[DiffSyncHandler](./src/main/java/org/jboss/aerogear/diffsync/DiffSyncHandler.java) is a Netty handler responsible for
delegating requests to the sync engine and sending back responses. It also handles networking tasks like reconnects etc.



