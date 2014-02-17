## AeroGear Sync Protocol
This module contains definitions of the application protocol for AeroGear's sync protocol.

The format of messages will be in JavaScript Object Notation (JSON)

### JSON
The JSON object/document transfered over the transport protocol will look like this:

    {
        docId: 'someUniqueDocName',
        rev: 1,
        content: { }
    }