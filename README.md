## AeroGear Data Synchronization Project
This project goal is to specify a data synchronization protocol that can be used by AeroGear's client libraries.

Data synchronziation is an important feature for mobile clients to enable offline working. Mobile clients can
work without a connection, and later when a connection becomes available, any changes made by the client should be
synchronized with the source of the data.

Initially this server will mimic CouchDB protocol and use a CouchDB instance behind the scenes. We expect this to
change later but doing this will allow our client libraries to have something to test against sooner rather
than later.

### Prerequisites
This project currently requires [CouchDB](http://couchdb.apache.org/).


