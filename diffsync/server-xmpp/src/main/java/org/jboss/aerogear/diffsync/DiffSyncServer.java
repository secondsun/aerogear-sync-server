/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.aerogear.diffsync;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.jboss.aerogear.diffsync.server.DefaultServerSynchronizer;
import org.jboss.aerogear.diffsync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.diffsync.server.ServerSyncEngine;
import org.jboss.aerogear.diffsync.server.ServerSynchronizer;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * A Netty based WebSocket server that is able to handle differential
 * synchronization edits.
 */
public final class DiffSyncServer {

    private static final Logger logger = Logger.getLogger(DiffSyncServer.class.getCanonicalName());

    private XMPPConnection connection;

    private static final long SENDER_ID = 0l;
    private static final String API_KEY = "";
    
    private static final String GCM_SERVER = "gcm.googleapis.com";
    private static final int GCM_PORT = 5235;

    private static final String GCM_ELEMENT_NAME = "gcm";
    private static final String GCM_NAMESPACE = "google:mobile:data";

    private static final String DEFAULT_CONFIG = "/sync.config";

    public void connect(long senderId, String apiKey, ServerSyncEngine<String> syncEngine)
            throws XMPPException, IOException, SmackException {
        ConnectionConfiguration config
                = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        config.setReconnectionAllowed(true);
        config.setRosterLoadedAtLogin(false);
        config.setDebuggerEnabled(true);
        
        config.setSendPresence(false);
        config.setSocketFactory(SSLSocketFactory.getDefault());

        connection = new XMPPTCPConnection(config);
        

        connection.addConnectionListener(new LoggingConnectionListener());

        // Handle incoming packets
        connection.addPacketListener(new DiffSyncHandler(syncEngine, connection), new PacketTypeFilter(Message.class));

        // Log all outgoing packets
        connection.addPacketInterceptor(new PacketInterceptor() {
            @Override
            public void interceptPacket(Packet packet) {
                logger.log(Level.INFO, "Sent: {0}", packet.toXML());
            }
        }, new PacketTypeFilter(Message.class));

        connection.connect();
        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }

    public static void main(String[] args) throws Exception {
        
        final long senderId = SENDER_ID; // your GCM sender id
        final String password = API_KEY;

        final String configFile = args.length == 0 ? DEFAULT_CONFIG : args[0];
        final StandaloneConfig config = ConfigReader.parse(configFile);
        final ServerSynchronizer<String> synchronizer = new DefaultServerSynchronizer();
        final ServerInMemoryDataStore dataStore = new ServerInMemoryDataStore();
        final ServerSyncEngine<String> syncEngine = new ServerSyncEngine<String>(synchronizer, dataStore);
        
        DiffSyncServer ccsClient = new DiffSyncServer();

        ccsClient.connect(senderId, password, syncEngine);

        while (true);
    }


    private static final class LoggingConnectionListener
            implements ConnectionListener {

        @Override
        public void connected(XMPPConnection xmppConnection) {
            logger.info("Connected.");
        }

        @Override
        public void authenticated(XMPPConnection xmppConnection) {
            logger.info("Authenticated.");
        }

        @Override
        public void reconnectionSuccessful() {
            logger.info("Reconnecting..");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            logger.log(Level.INFO, "Reconnection failed.. ", e);
        }

        @Override
        public void reconnectingIn(int seconds) {
            logger.log(Level.INFO, "Reconnecting in %d secs", seconds);
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            logger.info("Connection closed on error.");
        }

        @Override
        public void connectionClosed() {
            logger.info("Connection closed.");
        }
    }
}