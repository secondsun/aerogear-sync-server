## AeroGear Server Data Synchronization Project
This project aims to provide servers to support data synchronization protocols for testing by AeroGear's client libraries.

Currently two different types of server are provided, one is a [RESTful server](#restserver) implementation that uses a simple approach
of rejecting conflicts and delegating the responsibility to resolve conflicts to the client. This is similar to what
most version control systems do. This server uses HTTP and supports SPDY where available.

The [second server](#diffserver) is based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) by
Neil Fraser. This server implementation is WebSocket based.

### Prerequisites
This project currently requires [CouchDB](http://couchdb.apache.org/).

### RESTful server
<a name="restserver"></a>
Initially this server will mimic CouchDB protocol and use a CouchDB instance behind the scenes. We expect this to
change later but doing this will allow our client libraries to have something to test against sooner rather
than later.

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
    {"rev":"5"}

A successful response will return a HTTP ```200``` with a body:

    HTTP/1.1 200 OK
    <deleteRevision>

#### Starting the server

    mvn exec:exec -Prestserver

#### Differential Synchronization Server
<a name="diffserver"></a>
As mentioned in the overview this server is based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html)
by Neil Fraser. The server implementation is WebSocket based.

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

