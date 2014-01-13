#!/bin/sh

keytool -genkey -keyalg RSA -alias sync -keystore sync.keystore -storepass syncstore -validity 360 -keysize 512
