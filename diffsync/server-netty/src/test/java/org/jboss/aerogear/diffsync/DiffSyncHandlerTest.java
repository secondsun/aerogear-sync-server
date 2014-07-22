/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.diffsync.Diff.Operation;
import org.jboss.aerogear.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.diffsync.client.DefaultClientSynchronizer;
import org.jboss.aerogear.diffsync.server.DefaultServerSynchronizer;
import org.jboss.aerogear.diffsync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.diffsync.server.ServerSyncEngine;
import org.jboss.aerogear.diffsync.server.ServerSynchronizer;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffSyncHandlerTest {

    @Test
    public void unknownMessageType() {
        final EmbeddedChannel channel = embeddedChannel();
        final JsonNode json = writeTextFrame(message("bogus").toString(), channel);
        assertThat(json.get("result").asText(), equalTo("Unknown msgType 'bogus'"));
    }

    @Test
    public void addDocument() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final JsonNode json = sendAddDocMsg(docId, clientId, "Once upon a time", channel);
        assertThat(json.get("result").asText(), equalTo("ADDED"));
    }

    @Test
    public void addDocumentObjectContent() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final String content = "{\"content\": {\"name\": \"Dr.Rosen\"}}";
        final JsonNode json = sendAddDocMsg(docId, clientId, content, channel);
        assertThat(json.get("result").asText(), equalTo("ADDED"));
    }

    @Test
    public void patch() {
        final ServerInMemoryDataStore dataStore = new ServerInMemoryDataStore();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String originalContent = "{\"content\": \"Do or do not, there is no try.\"}";
        final String updatedContent = "{\"content\": \"Do or do not, there is no try!\"}";
        final String client1Id = "client1";
        final String client2Id = "client2";

        // add same document but with two different clients/channels.
        sendAddDocMsg(docId, client1Id, originalContent, channel1);
        sendAddDocMsg(docId, client2Id, originalContent, channel2);

        final Edits clientEdit = generateClientSideEdits(docId, originalContent, client1Id, updatedContent);
        final String responseOne = sendEdit(clientEdit, channel1);
        final Edits edits = JsonMapper.fromJson(responseOne, DefaultEdits.class);
        assertThat(edits.documentId(), equalTo(docId));
        assertThat(edits.clientId(), equalTo(client1Id));
        assertThat(edits.edits().size(), is(1));
        assertThat(edits.edits().peek().diffs().get(0).operation(), is(Operation.UNCHANGED));

        // client1 should not get an update as it was the one making the change.
        assertThat(channel1.readOutbound(), is(nullValue()));

        // get the update from channel2.
        final TextWebSocketFrame serverUpdate = channel2.readOutbound();
        final Edits serverUpdates = JsonMapper.fromJson(serverUpdate.text(), DefaultEdits.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        final Edit edit = serverUpdates.edits().peek();
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.serverVersion(), is(0L));
        assertThat(edit.diffs().size(), is(4));
        assertThat(edit.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(edit.diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(edit.diffs().get(1).text(), equalTo("."));
        assertThat(edit.diffs().get(2).operation(), is(Operation.ADD));
        assertThat(edit.diffs().get(2).text(), equalTo("!"));
        assertThat(edit.diffs().get(3).operation(), is(Operation.UNCHANGED));
    }

    @Test
    public void patchJedi() {
        final ClientSyncEngine<String> clientSyncEngine = newClientSyncEngine();
        final ServerInMemoryDataStore dataStore = new ServerInMemoryDataStore();
        final EmbeddedChannel channel1 = embeddedChannel(dataStore);
        final EmbeddedChannel channel2 = embeddedChannel(dataStore);
        final String docId = UUID.randomUUID().toString();
        final String original = "I'm a Jedi";
        final String updateOne = "I'm a Sith";
        final String updateTwo = "Oh Yeah";
        final String client1Id = "client1";
        final String client2Id = "client2";

        // add same document but with two different clients/channels.
        sendAddDocMsg(docId, client1Id, original, channel1);
        sendAddDocMsg(docId, client2Id, original, channel2);

        clientSyncEngine.addDocument(new DefaultClientDocument<String>(docId, client1Id, original));

        final Edits clientEdit = clientSyncEngine.diff(new DefaultClientDocument<String>(docId, client1Id, updateOne));
        final String responseOne = sendEdit(clientEdit, channel1);
        final Edits edits = JsonMapper.fromJson(responseOne, DefaultEdits.class);
        assertThat(edits.documentId(), equalTo(docId));
        assertThat(edits.clientId(), equalTo(client1Id));
        assertThat(edits.edits().size(), is(1));
        assertThat(edits.edits().peek().diffs().get(0).operation(), is(Operation.UNCHANGED));
        // patch the client engine so that version are updated and edits cleared
        clientSyncEngine.patch(edits);

        // get the update from channel2.
        final TextWebSocketFrame serverUpdateOne = channel2.readOutbound();
        final Edits serverUpdates = JsonMapper.fromJson(serverUpdateOne.text(), DefaultEdits.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        final Edit editOne = serverUpdates.edits().peek();
        assertThat(editOne.clientVersion(), is(0L));
        assertThat(editOne.serverVersion(), is(0L));

        assertThat(editOne.diffs().size(), is(5));
        assertThat(editOne.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(editOne.diffs().get(0).text(), equalTo("I'm a "));
        assertThat(editOne.diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(editOne.diffs().get(1).text(), equalTo("Jed"));
        assertThat(editOne.diffs().get(2).operation(), is(Operation.ADD));
        assertThat(editOne.diffs().get(2).text(), equalTo("S"));
        assertThat(editOne.diffs().get(3).operation(), is(Operation.UNCHANGED));
        assertThat(editOne.diffs().get(3).text(), equalTo("i"));
        assertThat(editOne.diffs().get(4).operation(), is(Operation.ADD));
        assertThat(editOne.diffs().get(4).text(), equalTo("th"));

        final Edits clientEditTwo = clientSyncEngine.diff(new DefaultClientDocument<String>(docId, client1Id, updateTwo));
        final String responseTwo = sendEdit(clientEditTwo, channel1);
        final Edits editsTwo = JsonMapper.fromJson(responseTwo, DefaultEdits.class);
        assertThat(editsTwo.edits().size(), is(1));
        assertThat(editsTwo.edits().peek().diffs().get(0).operation(), is(Operation.UNCHANGED));

        final TextWebSocketFrame serverUpdateTwo = channel2.readOutbound();
        final Edits serverUpdatesTwo = JsonMapper.fromJson(serverUpdateTwo.text(), DefaultEdits.class);
        assertThat(serverUpdatesTwo.documentId(), equalTo(docId));
        assertThat(serverUpdatesTwo.clientId(), equalTo(client2Id));
        final Edit editTwo = serverUpdatesTwo.edits().peek();
        assertThat(editTwo.clientVersion(), is(0L));
        assertThat(editTwo.serverVersion(), is(1L));

        assertThat(editTwo.diffs().size(), is(7));
        assertThat(editTwo.diffs().get(0).operation(), is(Operation.DELETE));
        assertThat(editTwo.diffs().get(0).text(), equalTo("I'm"));
        assertThat(editTwo.diffs().get(1).operation(), is(Operation.ADD));
        assertThat(editTwo.diffs().get(1).text(), equalTo("Oh"));
        assertThat(editTwo.diffs().get(2).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diffs().get(2).text(), equalTo(" "));
        assertThat(editTwo.diffs().get(3).operation(), is(Operation.ADD));
        assertThat(editTwo.diffs().get(3).text(), equalTo("Ye"));
        assertThat(editTwo.diffs().get(4).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diffs().get(4).text(), equalTo("a"));
        assertThat(editTwo.diffs().get(5).operation(), is(Operation.DELETE));
        assertThat(editTwo.diffs().get(5).text(), equalTo(" Sit"));
        assertThat(editTwo.diffs().get(6).operation(), is(Operation.UNCHANGED));
        assertThat(editTwo.diffs().get(6).text(), equalTo("h"));
    }

    private static JsonNode sendEditMsg(final Edits edits, final EmbeddedChannel ch) {
        return writeTextFrame(JsonMapper.toJson(edits), ch);
    }

    private static String sendEdit(final Edits edits, final EmbeddedChannel ch) {
        return writeFrame(JsonMapper.toJson(edits), ch);
    }

    private static JsonNode sendAddDocMsg(final String docId,
                                          final String clientId,
                                          final String content,
                                          final EmbeddedChannel ch) {
        final ObjectNode docMsg = message("add");
        docMsg.put("msgType", "add");
        docMsg.put("id", docId);
        docMsg.put("clientId", clientId);
        docMsg.put("content", content);
        return writeTextFrame(docMsg.toString(), ch);
    }

    private static ObjectNode message(final String type) {
        final ObjectNode jsonNode = JsonMapper.newObjectNode();
        jsonNode.put("msgType", type);
        return jsonNode;
    }

    private static JsonNode writeTextFrame(final String content, final EmbeddedChannel ch) {
        ch.writeInbound(textFrame(content));
        final TextWebSocketFrame textFrame = ch.readOutbound();
        return JsonMapper.asJsonNode(textFrame.text());
    }

    private static String writeFrame(final String content, final EmbeddedChannel ch) {
        ch.writeInbound(textFrame(content));
        final TextWebSocketFrame textFrame = ch.readOutbound();
        return textFrame.text();
    }

    private static TextWebSocketFrame textFrame(final String content) {
        return new TextWebSocketFrame(content);
    }

    private static EmbeddedChannel embeddedChannel() {
        return embeddedChannel(new ServerInMemoryDataStore());
    }

    private static EmbeddedChannel embeddedChannel(final ServerInMemoryDataStore dataStore) {
        final ServerSynchronizer<String> synchronizer = new DefaultServerSynchronizer();
        final ServerSyncEngine<String> syncEngine = new ServerSyncEngine<String>(synchronizer, dataStore);
        return new EmbeddedChannel(new DiffSyncHandler(syncEngine));
    }

    private static Edits generateClientSideEdits(final String documentId,
                                                 final String originalContent,
                                                 final String clientId,
                                                 final String updatedContent) {
        final ClientSyncEngine<String> clientSyncEngine = newClientSyncEngine();
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, clientId, originalContent));
        final DefaultClientDocument<String> doc = new DefaultClientDocument<String>(documentId, clientId, updatedContent);
        return clientSyncEngine.diff(doc);
    }

    private static ClientSyncEngine<String> newClientSyncEngine() {
        return new ClientSyncEngine<String>(new DefaultClientSynchronizer(), new ClientInMemoryDataStore());
    }

}
