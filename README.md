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

* [synchronizers](./synchronizers)  
Contains a implementations that perform the synchronization operations in the Differential Synchronization algorithm.

* [server](./server)  
Contains a concrete implementations of the server side core API. These implementation is inteded to be "embedded" in a server
component, like a Servlet, a Netty server, etc.

* [client](./client)  
Contains a concrete implementation of the client side core API. This implementation is inteded to be "embedded" in a client
component, like a Netty client, an Android device, etc.

* [itests](./itests)  
Contains tests that use both the [server-core](./server-core) and [client-core](./client-core) in cooperation.

### Usage

#### Starting the server
    cd server/server-netty
    mvn exec:exec


#### Building/Testing
To build this project simply use the following maven command:

    mvn install

To run the the unit tests:

    mvn test
    
### Creating a distribution
A distribution (a "fat" jar) can be produces by running the following maven command from the [distribution](./distribution)
directory:

    mvn package

This willl produce a ```target/aerogear-sync-server-VERSION.jar``` file.


## Message format
This section defines the message format that is sent between a client and a server.

### Add message type
The _add_ message is sent when a client wants to add a document/object into the server sync engine.
The format is in Java Object Notation (JSON) and has the following structure:

    { "msgType": "add",
      "id":"12345",
      "clientId":"76170b10-5d2f-496f-b4ba-c71b31a27f72",
      "content":{"name":"Luke Skywalker"}
    }

*msgType*  
The typeof this message. Must be ```add```.

*id*  
The document identifier for this document being added. This value is chosen by the client and all clients that use the same document id receive updates when this
document is updated.

*clientId*  
An identifier for the client adding the document. This value must be a unique identifier for the client.

*content*  
The actual content of the document/object being added. The type of content depends upon the type of documents the server supports.
In the above example the document/object content type is JSON.


## Patch message type
The _patch_ message is sent when a client or server has updates that need to be sent the the other side.
The format is in JSON and has the following structure:

    { "msgType": "patch",
      "id": "12345",
      "clientId":"76170b10-5d2f-496f-b4ba-c71b31a27f72",
      "edits":[
        { "clientVersion":0,
          "serverVersion":0,
          "checksum": "da39a3ee5e6b4b0d3255bfef95601890afd80709",
          "diffs": [
            { "op":"replace","path":"/name","value":"Darth Vader" }
          ]
        }
      ]
    }

*msgType*  
The typeof this message. Must be ```patch```.

*id*  
The document identifier for this document being updated.

*clientId*  
An identifier for the client.

*edits*  
Is an array of updates. An edit is created for each diff taken on the client or server side. These edits are stored before sending to the opposing side, and
removed after being applied. They act as acknowledgments.

*clientVersion*  
This is client version that this edit was based on.

*serverVersion*  
This is server version that this edit was based on.

*checksum*  
This is checksum for the shadow document on the opposing side.

*diffs*  
The format of diffs depends on the type of document/object type that the server supports. In the above example the type 
that the server stores is JsonNode instance, and the format of patches is of JSON Patch format. For more information 
about the various type, please refer to the different [synchronizers](./synchronizers).

## SyncServerEngine
The [SyncServerEngine](./server/server-core/src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java) is
responsible for driving the main differential synchronization algorithm but leaves the details of how data is stored and how diffs/patches are processed on that data to injectable
implementations.
It does this by getting injected during [construction](./server/server-core/src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java#L46) with an instance of a _ServerSynchronizer_ and a _ServerDataStore_.

### ServerDataStore
The [ServerDataStore](./api/src/main/java/org/jboss/aerogear/sync/server/ServerDataStore.java) defines the methods for
storing a specific document types.

### ServerSynchronizer
The [ServerSynchronizer](./api/src/main/java/org/jboss/aerogear/sync/server/ServerSynchronizer.java) defines the methods
required to perform the specific diff/patch processing on the data type supported.

#### Example of constructing a SyncServerEngine
To construct a server that uses the JSON Patch [synchronizer](./synchronizers/json-patch) you would use the following code:

    final JsonPatchServerSynchronizer synchronizer = new JsonPatchServerSynchronizer();
    final ServerInMemoryDataStore<JsonNode, JsonPatchEdit> dataStore = new ServerInMemoryDataStore<JsonNode, JsonPatchEdit>();
    final ServerSyncEngine<JsonNode, JsonPatchEdit> syncEngine = new ServerSyncEngine<JsonNode, JsonPatchEdit>(synchronizer, dataStore);

### Synchronizers
A _synchronizer_ in AeroGear is a module that serves two purposes which are closely related.
One, is to provide storage for the data type, and the second is to provide the patching algorithm to be used on that data type.
The name _synchronizer_ is because they take care of the synchronization part of the Differential Synchronization algorithm

For example, one synchronizer might support plain text while another supports JSON Objects as the content of documents being stored. But a patching algorithm used for plain text might
not be appropriate for JSON Objects. Hence the server and client side components are made up of "pluggable" [synchronizers](./synchronizers) and data stores implementations.

By implementing the _ServerDataStore_ and _ServerSynchronizer_ interfaces different data formats and diff/patching algoritms can be provided. The AeroGear project provides the following
[synchronizers](./synchronizers/json-patch) out of the box:

*  [DiffMatchPatch](./synchronizers/diffmatchpatch)
This synchronizer uses plain strings the document data type and Neil Frasers DiffMatchPatch Java implementation
for its patching algorithm.

*  [JSON Patch](./synchronizers/json-patch)
This synchronizer uses [Jackson's](http://wiki.fasterxml.com/JacksonHome) _JsonNode_ as the document data type and uses
[JSON PATCH](https://tools.ietf.org/html/rfc6902) for its patching algorithm.
