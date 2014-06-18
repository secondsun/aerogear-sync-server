/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.aerogear.sync.rest;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a {@link SSLContext} for just server certificates.
 */
public final class SslServerContext {

    private static final String PROTOCOL = "TLS";
    private static final Logger logger = LoggerFactory.getLogger(SslServerContext.class);

    private SslServerContext() {
    }

    /**
     * Creates a new {@link SSLContext}. This is an expensive operation and should only be done
     * once and then the SSL context can be reused.
     *
     * @return {@link SSLContext} the SSLContext.
     */
    public static SSLContext sslContext(final String keyStore, final String password) {
        try {
            final SSLContext serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(keyManagerFactory(loadKeyStore(keyStore, password), password).getKeyManagers(), null, null);
            return serverContext;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to initialize the server-side SSLContext", e);
        }
    }

    private static KeyManagerFactory keyManagerFactory(final KeyStore ks, final String password) throws Exception {
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(getKeyManagerAlgorithm());
        kmf.init(ks, password.toCharArray());
        return kmf;
    }

    @SuppressWarnings("resource")
    private static KeyStore loadKeyStore(final String keyStore, final String password) throws Exception {
        InputStream fin = null;
        try {
            fin = SslServerContext.class.getResourceAsStream(keyStore);
            if (fin == null) {
                throw new IllegalStateException("Could not locate keystore [" + keyStore + ']');
            }
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(fin, password.toCharArray());
            return ks;
        } finally {
            safeClose(fin);
        }

    }

    private static String getKeyManagerAlgorithm() {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        return algorithm;
    }

    private static void safeClose(final Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (final IOException e) {
            logger.error("Error while trying to close closable [" + c + ']', e);
        }
    }

}
