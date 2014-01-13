#### Starting the server

    mvn exec:exec 

The RestServer used SPDY which requires SSL/TLS, and we are currently using a self-signed certificate which has to
be accepted/imported into the browser you are using. This can be done by simply pointing you browser to:

    https://localhost:8080

And then add an exception for the certificate.

#### Testing from IDE
When running the RestServer you have to add a ```Xbootclasspath``` entry as this is required by SPDY as it uses
[NPN](http://wiki.eclipse.org/Jetty/Feature/NPN). This is done by maven when using the maven goals above.

For example, add the following as a "VM Option" in the "Edit Configuration Settings":

    -Xbootclasspath/p:restsync/server-netty/target/npn/npn-boot-1.1.6.v20130911.jar

The exact version of this jar might be different but you an look in ```restsync/server-netty/target/npn/``` to see the version to
use.

### Troubleshooting
If you get the following error while running against this server:

    net::ERR_INSECURE_RESPONSE rest-sync-client.js:175

This is most probably because we are using selfsigned certifcates and an exception is required to be added regarding
trusting this certificate. This can be done by pointing your browser to ```https://localhost:8080```
