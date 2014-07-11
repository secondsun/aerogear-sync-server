## AeroGear Data Synchronization Project
This project aims to prototype a data synchronization protocol that can be used by AeroGear's client libraries.

AeroGear Sync consists of the following modules:

* [restsync](./restsync)  
Implementation that uses a simple approach of rejecting conflicts and delegating the responsibility to resolve conflicts
to the client. This server uses HTTP and supports SPDY where available.

* [diffsync](./diffsync)  
This module contains the core classes for data synchronization. These are intended to be used by server implementation.
Based on Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) by Neil Fraser.
This server implementation is WebSocket based.

* [common](./common)  
Contains [google-diff-match-patch](https://code.google.com/p/google-diff-match-patch/) with some minor tweaks.

* [js-client](./js-client)  
Contains a JavaScript client library and tests for verifiying the servers funtionality.
This will most likely be moved out of this project if we decide to move forward with it.


