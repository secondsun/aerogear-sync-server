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

    private static final String GCM_SERVER = "gcm.googleapis.com";
    private static final int GCM_PORT = 5235;

    private static final String GCM_ELEMENT_NAME = "gcm";
    private static final String GCM_NAMESPACE = "google:mobile:data";

    private static final String DEFAULT_CONFIG = "/sync.config";

    /**
     * Connects to GCM Cloud Connection Server using the supplied credentials.
     *
     * @param senderId Your GCM project number
     * @param apiKey API Key of your project
     */
    public void connect(long senderId, String apiKey)
            throws XMPPException, IOException, SmackException {
        ConnectionConfiguration config
                = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        config.setReconnectionAllowed(true);
        config.setRosterLoadedAtLogin(false);
        config.setSendPresence(false);
        config.setSocketFactory(SSLSocketFactory.getDefault());

        connection = new XMPPTCPConnection(config);
        connection.connect();

        connection.addConnectionListener(new LoggingConnectionListener());

        // Handle incoming packets
        connection.addPacketListener(new DiffSyncHandler(null, connection), new PacketTypeFilter(Message.class));

        // Log all outgoing packets
        connection.addPacketInterceptor(new PacketInterceptor() {
            @Override
            public void interceptPacket(Packet packet) {
                logger.log(Level.INFO, "Sent: {0}", packet.toXML());
            }
        }, new PacketTypeFilter(Message.class));

        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }

    public static void main(String[] args) throws Exception {
        final long senderId = 213383135458L; // your GCM sender id
        final String password = "AIzaSyB9MZDkP8kNcehJRmyNnRE-E8CWDRXBBSg";

        DiffSyncServer ccsClient = new DiffSyncServer();

        ccsClient.connect(senderId, password);

        while (true);
    }
//    
//    public static void main(final String... args) throws Exception {
//        final String configFile = args.length == 0 ? DEFAULT_CONFIG : args[0];
//        final StandaloneConfig config = ConfigReader.parse(configFile);
//        
//        final ServerSynchronizer<String> synchronizer = new DefaultServerSynchronizer();
//        final ServerInMemoryDataStore dataStore = new ServerInMemoryDataStore();
//        final ServerSyncEngine<String> syncEngine = new ServerSyncEngine<String>(synchronizer, dataStore);
//        final DiffSyncHandler diffSyncHandler = new DiffSyncHandler(syncEngine);
//        try {
//            final ServerBootstrap sb = new ServerBootstrap();
//            sb.group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        public void initChannel(final SocketChannel ch) throws Exception {
//                            ch.pipeline().addLast(
//                                    new HttpRequestDecoder(),
//                                    new HttpObjectAggregator(65536),
//                                    new HttpResponseEncoder(),
//                                    new WebSocketServerProtocolHandler("/sync"),
//                                    diffSyncHandler);
//                        }
//                    });
//
//            final Channel ch = sb.bind(config.host(), config.port()).sync().channel();
//            System.out.println("SyncServer bound to " + config.host() + ':' + config.port());
//
//            ch.closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//      }

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
