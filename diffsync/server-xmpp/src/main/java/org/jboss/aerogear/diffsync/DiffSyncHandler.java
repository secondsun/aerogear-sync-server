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

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.diffsync.server.MessageType;
import org.jboss.aerogear.diffsync.server.ServerSyncEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.jboss.aerogear.diffsync.server.MessageType.ADD;
import static org.jboss.aerogear.diffsync.server.MessageType.DETACH;
import static org.jboss.aerogear.diffsync.server.MessageType.PATCH;
import static org.jboss.aerogear.diffsync.server.MessageType.UNKNOWN;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;

public class DiffSyncHandler implements PacketListener {

    private static final Logger logger = Logger.getLogger(DiffSyncHandler.class.getCanonicalName());

    private XMPPConnection connection;

    private static final String GCM_SERVER = "gcm.googleapis.com";
    private static final int GCM_PORT = 5235;

    private static final String GCM_ELEMENT_NAME = "gcm";
    private static final String GCM_NAMESPACE = "google:mobile:data";

    /**
     * Indicates whether the connection is in draining state, which means that
     * it will not accept any new downstream messages.
     */
    protected volatile boolean connectionDraining = false;

    private static final ConcurrentHashMap<String, Set<Client>> clients
            = new ConcurrentHashMap<String, Set<Client>>();
    private final ServerSyncEngine<String> syncEngine;

    static {
        ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
            new PacketExtensionProvider() {
                @Override
                public PacketExtension parseExtension(XmlPullParser parser) throws
                        Exception {
                    String json = parser.nextText();
                    return new GcmPacketExtension(json);
                }
            });
    }
    
    public DiffSyncHandler(final ServerSyncEngine<String> syncEngine, XMPPConnection connection) {
        this.connection = connection;
        this.syncEngine = syncEngine;
    }

    /**
     * Handles an ACK.
     *
     * <p>
     * Logs a INFO message, but subclasses could override it to properly handle
     * ACKs.
     *
     * @param jsonObject the messageObject
     */
    protected void handleAckReceipt(JsonNode jsonObject) {
        String messageId = (String) jsonObject.get("message_id").asText();
        String from = (String) jsonObject.get("from").asText();
        logger.log(Level.INFO, "handleAckReceipt() from: " + from + ",messageId: " + messageId);
    }

    /**
     * Handles a NACK.
     *
     * <p>
     * Logs a INFO message, but subclasses could override it to properly handle
     * NACKs.
     *
     * @param jsonObject the messageObject
     */
    protected void handleNackReceipt(JsonNode jsonObject) {
        String messageId = (String) jsonObject.get("message_id").asText();
        String from = (String) jsonObject.get("from").asText();
        logger.log(Level.INFO, "handleNackReceipt() from: " + from + ",messageId: " + messageId);
    }

    protected void handleControlMessage(JsonNode jsonObject) {
        logger.log(Level.INFO, "handleControlMessage(): " + jsonObject);
        String controlType = (String) jsonObject.get("control_type").asText();
        if ("CONNECTION_DRAINING".equals(controlType)) {
            connectionDraining = true;
        } else {
            logger.log(Level.INFO, "Unrecognized control type: %s. This could happen if new features are " + "added to the CCS protocol.", controlType);
        }
    }

    /**
     * Creates a JSON encoded GCM message.
     *
     * @param to RegistrationId of the target device (Required).
     * @param messageId Unique messageId for which CCS will send an "ack/nack"
     * (Required).
     * @param payload Message content intended for the application. (Optional).
     * @param collapseKey GCM collapse_key parameter (Optional).
     * @param timeToLive GCM time_to_live parameter (Optional).
     * @param delayWhileIdle GCM delay_while_idle parameter (Optional).
     * @return JSON encoded GCM message.
     */
    public static String createJsonMessage(String to, String messageId,
            Map<String, String> payload, String collapseKey, Long timeToLive,
            Boolean delayWhileIdle) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("to", to);
        if (collapseKey != null) {
            message.put("collapse_key", collapseKey);
        }
        if (timeToLive != null) {
            message.put("time_to_live", timeToLive);
        }
        if (delayWhileIdle != null && delayWhileIdle) {
            message.put("delay_while_idle", true);
        }
        message.put("message_id", messageId);
        message.put("data", payload);
        return JSONValue.toJSONString(message);
    }

    /**
     * Creates a JSON encoded ACK message for an upstream message received from
     * an application.
     *
     * @param to RegistrationId of the device who sent the upstream message.
     * @param messageId messageId of the upstream message to be acknowledged to
     * CCS.
     * @return JSON encoded ack.
     */
    protected static String createJsonAck(String to, String messageId) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("message_type", "ack");
        message.put("to", to);
        message.put("message_id", messageId);
        return JSONValue.toJSONString(message);
    }

    @Override
    public void processPacket(Packet packet) {
        logger.log(Level.INFO, "Received: " + packet.toXML());
        Message incomingMessage = (Message) packet;
        GcmPacketExtension gcmPacket
                = (GcmPacketExtension) incomingMessage.
                getExtension(GCM_NAMESPACE);
        String body = gcmPacket.getJson();
        try {
            @SuppressWarnings("unchecked")
            JsonNode jsonObject = JsonMapper.asJsonNode(body);

            // present for "ack"/"nack", null otherwise
            Object messageType = jsonObject.get("message_type");

            if (messageType == null) {
                // Normal upstream data message
                messageReceived(jsonObject);

                // Send ACK to CCS
                String messageId = (String) jsonObject.get("message_id").asText();
                String from = (String) jsonObject.get("from").asText();
                String ack = createJsonAck(from, messageId);
                send(ack);
            } else if ("ack".equals(messageType.toString())) {
                // Process Ack
                handleAckReceipt(jsonObject);
            } else if ("nack".equals(messageType.toString())) {
                // Process Nack
                handleNackReceipt(jsonObject);
            } else if ("control".equals(messageType.toString())) {
                // Process control message
                handleControlMessage(jsonObject);
            } else {
                logger.log(Level.WARNING,
                        "Unrecognized message type (%s)",
                        messageType.toString());
            }
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Error parsing JSON " + body, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to process packet", e);
        }
    }

    protected void messageReceived(JsonNode json) throws Exception {

        JsonNode syncMessage = JsonMapper.asJsonNode(json.get("data").get("message").asText());
        
        if (true) {
            
            logger.info("Doc:" + json);
            switch (MessageType.from(syncMessage.get("msgType").asText())) {
                case ADD:
                    final Document<String> doc = documentFromJson(syncMessage);
                    final String clientId = json.get("from").toString();
                    addClientListener(doc.id(), clientId);
                    final PatchMessage patchMessage = addDocument(doc, clientId);

                    send((JsonMapper.toJson(patchMessage)));
                    break;
                case PATCH:
                    final PatchMessage clientPatchMessage = JsonMapper.fromJson(syncMessage.toString(), DefaultPatchMessage.class);
                    checkForReconnect(clientPatchMessage.documentId(), clientPatchMessage.clientId());
                    logger.log(Level.FINER, "Client Edits=" + clientPatchMessage);
                    patch(clientPatchMessage);
                    notifyClientListeners(clientPatchMessage);
                    break;
                case DETACH:
                    // detach the client from a specific document.
                    break;
                case UNKNOWN:
                    //unknownMessageType(ctx, json);
                    break;
            }
        }
    }

    private PatchMessage addDocument(final Document<String> document, final String clientId) {
        return syncEngine.addDocument(document, clientId);
    }

    private void patch(final PatchMessage clientEdit) {
        syncEngine.patch(clientEdit);
    }

    /**
     * Sends a packet with contents provided.
     */
    protected void send(String jsonRequest) throws SmackException.NotConnectedException {
        Packet request = new GcmPacketExtension(jsonRequest).toPacket();
        connection.sendPacket(request);
    }

    private static Document<String> documentFromJson(final JsonNode json) {
        final JsonNode contentNode = json.get("content");
        String content = null;
        if (contentNode != null && !contentNode.isNull()) {
            if (contentNode.isArray() || contentNode.isObject()) {
                content = JsonMapper.toString(contentNode);
            } else {
                content = contentNode.asText();
            }
        }
        return new DefaultDocument<String>(json.get("id").asText(), content);
    }

    private PatchMessage diffs(final String documentId, final String clientId) {
        return syncEngine.diffs(documentId, clientId);
    }

    private static void checkForReconnect(final String documentId, final String clientId) {

        logger.info("Reconnected client [" + clientId + "]. Adding as listener.");
        // the context was used to reconnect so we need to add client as a listener
        addClientListener(documentId, clientId);

    }

    private static void addClientListener(final String documentId, final String clientId) {
        final Client client = new Client(clientId);
        final Set<Client> newClient = Collections.newSetFromMap(new ConcurrentHashMap<Client, Boolean>());
        newClient.add(client);
        while (true) {
            final Set<Client> currentClients = clients.get(documentId);
            if (currentClients == null) {
                final Set<Client> previous = clients.putIfAbsent(documentId, newClient);
                if (previous != null) {
                    newClient.addAll(previous);
                    if (clients.replace(documentId, previous, newClient)) {
                        break;
                    }
                }
            } else {
                newClient.addAll(currentClients);
                if (clients.replace(documentId, currentClients, newClient)) {
                    break;
                }
            }
        }

    }

    private void notifyClientListeners(final PatchMessage clientPatchMessage) {
        final Edit peek = clientPatchMessage.edits().peek();
        if (peek == null) {
            // edits could be null as a client is allowed to send an patch message
            // that only contains an acknowledgement that it has received a specific
            // version from the server.
            return;
        }

        final String documentId = peek.documentId();
        for (Client client : clients.get(documentId)) {
           //TODO: this should be done async and not block the io thread!
            final PatchMessage patchMessage = diffs(documentId, client.id());
            logger.log(Level.FINE, "Sending to [" + client.registrationId + "] : " + patchMessage);
            try {
                send(JsonMapper.toJson(patchMessage));
            } catch (SmackException.NotConnectedException ex) {
                Logger.getLogger(DiffSyncHandler.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private static class Client {

        private final String registrationId;

        Client(final String clientId) {
            registrationId = clientId;
        }

        public String id() {
            return registrationId;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Client client = (Client) o;

            return !registrationId.equals(client.registrationId);
            
        }

        @Override
        public int hashCode() {
            int result = registrationId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Client[id=" + registrationId + ']';
        }
    }

    /**
     * XMPP Packet Extension for GCM Cloud Connection Server.
     */
    private static final class GcmPacketExtension extends DefaultPacketExtension {

        private final String json;

        public GcmPacketExtension(String json) {
            super(GCM_ELEMENT_NAME, GCM_NAMESPACE);
            this.json = json;
        }

        public String getJson() {
            return json;
        }

        @Override
        public String toXML() {
            return String.format("<%s xmlns=\"%s\">%s</%s>",
                    GCM_ELEMENT_NAME, GCM_NAMESPACE,
                    StringUtils.escapeForXML(json), GCM_ELEMENT_NAME);
        }

        public Packet toPacket() {
            Message message = new Message();
            message.addExtension(this);
            return message;
        }
    }
}
