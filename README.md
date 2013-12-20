## AeroGear Data Synchronization Project
This project aims to specify a data synchronization protocol that can be used by AeroGear's client libraries.

Data synchronziation is an important feature for mobile clients to enable offline working. Mobile devices can
work without a connection, and later when a connection becomes available, any changes made by the client should be
synchronized with the source of the data.

### Communication style
Should a request/response approach be used or should a full duplex communication channel be used. This really depends on
how a clients will work.

If the a client is only intended to synchronize with the server when saving data then a RESTful server would suffice.
But if a client should be able "listen" for updates from the server then server initiate push should be used. An example
of this would be WebSocket and use SockJS to support clients that do not support WebSockets.
Perhaps we should offer both approaches?

### RESTful
Similar to how CouchDB API works we could expose a RESTFul api for synchronizing data.

#### Create a document
To create a document a PUT request is made:

    PUT /document1234 HTTP/1.1
    {"model": "Toyota"}

A successful response will return a HTTP ```200``` with a body:

    HTTP/1.1 200 OK
    {"id":"document1234","rev":"1","content":"{\"model\": \"Toyota\"}"}

#### Update a document
To update a document a PUT request is made:

    PUT /document1234 HTTP/1.1
    {"id":"document1234","rev":"1","content":"{\"state\": \"update\"}"}

A successful response will return a HTTP ```200``` with a body:

    HTTP/1.1 200 OK
    {"id":"document1234","rev":"2","content":"{\"state\": \"update\"}"}

#### Get a document
To get a document a GET request is made:

    GET /document1234 HTTP/1.1

A successful response will return a HTTP ```200``` with a body:

    HTTP/1.1 200 OK
    {"id":"document1234","rev":"5","content":"{\"state\": \"update\"}"}

#### Delete a document
To delete a document a DELETE request is made:

    DELETE /document1234 HTTP/1.1

A successful response will return a HTTP ```200``` with a body:

    HTTP/1.1 200 OK
    <deleteRevision>


### Implementation
Initially this server will mimic CouchDB protocol and use a CouchDB instance behind the scenes. We expect this to
change later but doing this will allow our client libraries to have something to test against sooner rather
than later.

### Prerequisites
This project currently requires [CouchDB](http://couchdb.apache.org/).


### Building/Testing
To build this project simply use the following maven command:

    mvn install

To run the the unit tests:

    mvn test

To run the integration test (requires a running CouchDB)

    mvn test -Pcouchdb

