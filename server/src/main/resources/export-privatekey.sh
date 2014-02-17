#!/bin/sh
keytool -importkeystore -srckeystore sync.keystore -destkeystore intermediate.p12 -deststoretype PKCS12
openssl pkcs12 -in intermediate.p12 -out privatekey -nodes


