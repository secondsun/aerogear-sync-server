## AeroGear Server Data Synchronization Project
This project aims to provide servers to support data synchronization protocols for testing by AeroGear's client libraries.

Currently two different types of server are provided:  
1 [RestServer](#restserver)  
Implementation that uses a simple approach of rejecting conflicts and delegating the responsibility to resolve conflicts
to the client.   
This server uses HTTP and supports SPDY where available.  

2 [DiffServer](#diffserver)  
Based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) by
Neil Fraser.   
This server implementation is WebSocket based.  

### Prerequisites
This project currently requires [CouchDB](http://couchdb.apache.org/), and at the moment requires 5.0.0.Alpha2-SNAPSHOT
build of [Netty](https://github.com/netty/netty).

### RestServer server
<a name="restserver"></a>
Initially this server will mimic CouchDB protocol and use a CouchDB instance behind the scenes. We expect this to
change later but doing this will allow our client libraries to have something to test against sooner rather
than later.

Similar to how CouchDB API works we could expose a RESTFul api for synchronizing data. Like CouchDB a revision is used
for each document and must be communicated when requesting/updating a document to get the correct version

#### Conflict resolution (handling)
Bascially this server will only detect conflicts and inform the calling client about the conflict. When a conflict is
detected a HTTP 409 "Conflict" will be returned to the calling client.  
For example, I conflict response might look something like this:

    HTTP/1.1 409 Conflict
    Content-Type: application/json
    {"docId":"2941995c-3b75-4de4-8db7-ab2405bc4cfe","rev":"2-161abbc9241c550e113571bab5dcc953","content":"[{\"model\":\"honda\",\"color\":\"black\"},{\"model\":\"bmw\"}]"}
The body of the response contains the latest version that the server has. The client can use this version to resolve the
conflict and then retry the update request.

For examples of using this server please see the following tests:

    js-client/rest-sync-client.js

#### Starting the server

    mvn exec:exec -Prestserver

The RestServer used SPDY which requires SSL/TLS, and we are currently using a self-signed certificate which has to
be accepted/imported into the browser you are using. This can be done by simply pointing you browser to:

    https://localhost:8080

And then add an exception for the certificate.

### Differential Synchronization Server
<a name="diffserver"></a>
As mentioned in the overview this server is based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html)
by Neil Fraser. There is a good video presentation by Neil Fraser on [youtube](https://www.youtube.com/watch?v=S2Hp_1jqpY8).
The server implementation is WebSocket based.

#### Starting the server
    mvn exec:exec -Pdiffserver


### Building/Testing
To build this project simply use the following maven command:

    mvn install

To run the the unit tests:

    mvn test

To run the integration tests (requires a running CouchDB)

    mvn test -Pitests


#### Testing from IDE
When running the RestServer you have to add a ```Xbootclasspath``` entry as this is required by SPDY as it uses
[NPN](http://wiki.eclipse.org/Jetty/Feature/NPN). This is done by maven when using the maven goals above.

For example, add the following as a "VM Option" in the "Edit Configuration Settings":

    -Xbootclasspath/p:server/target/npn/npn-boot-1.1.6.v20130911.jar

The exact version of this jar might be different but you an look in ```server/target/npn/``` to see the version to
use.

