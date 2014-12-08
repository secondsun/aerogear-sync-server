# Sync JavaScript client tests
This module is only intended to provide test for the sync server

## Prerequisites
[Grunt](http://gruntjs.com/) is used to build this project. Grunt depends on the
[Node Package Manager](https://npmjs.org/) (npn) to install itself and it's plugins. Please follow the instructions
for installing in previous links to install npn and Grunt.

## Building
Install the required plugins before running the build the first time, or after adding dependencies to project.json:

    npm install

    npm install -g bower

    bower install

## Running the test app
After running the ``npm``` and ```bower``` commands above, run the following from the command line:

    node server.js

This will start a server listening to port ```8080```. Next open a browser with the url [http://localhost:8080](http://localhost:8080)

## Testing

### Testing the Diff Sync Client
First, start the Diff Sync Server:

    grunt shell

Next, open the qunit test in a browser:

    open tests/sync/diff-sync-client.html

    open tests/sync/diff-sync-engine.html


