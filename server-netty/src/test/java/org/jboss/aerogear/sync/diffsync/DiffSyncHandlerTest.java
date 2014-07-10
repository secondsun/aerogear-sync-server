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
package org.jboss.aerogear.sync.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.diffsync.Diff.Operation;
import org.jboss.aerogear.sync.diffsync.client.ClientDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffsync.client.DefaultClientSynchronizer;
import org.jboss.aerogear.sync.diffsync.server.DefaultServerSynchronizer;
import org.jboss.aerogear.sync.diffsync.server.ServerInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.server.ServerSyncEngine;
import org.jboss.aerogear.sync.diffsync.server.ServerSynchronizer;
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

        final Edit clientEdit = generateClientSideEdits(docId, originalContent, client1Id, updatedContent);
        final JsonNode json = sendEditMsg(clientEdit, channel1);
        assertThat(json.get("result").asText(), equalTo("PATCHED"));

        // client1 should not get an update as it was the one making the change.
        assertThat(channel1.readOutbound(), is(nullValue()));

        // get the update from channel2.
        final TextWebSocketFrame serverUpdate = channel2.readOutbound();
        final Edit serverUpdates = JsonMapper.fromJson(serverUpdate.text(), Edit.class);
        assertThat(serverUpdates.documentId(), equalTo(docId));
        assertThat(serverUpdates.clientId(), equalTo(client2Id));
        assertThat(serverUpdates.clientVersion(), is(0L));
        assertThat(serverUpdates.serverVersion(), is(0L));
        assertThat(serverUpdates.diffs().size(), is(4));
        assertThat(serverUpdates.diffs().get(0).operation(), is(Operation.UNCHANGED));
        assertThat(serverUpdates.diffs().get(1).operation(), is(Operation.DELETE));
        assertThat(serverUpdates.diffs().get(1).text(), equalTo("."));
        assertThat(serverUpdates.diffs().get(2).operation(), is(Operation.ADD));
        assertThat(serverUpdates.diffs().get(2).text(), equalTo("!"));
        assertThat(serverUpdates.diffs().get(3).operation(), is(Operation.UNCHANGED));
    }

    private static JsonNode sendEditMsg(final Edit edit, final EmbeddedChannel ch) {
        return writeTextFrame(JsonMapper.toJson(edit), ch);
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

    private static Edit generateClientSideEdits(final String documentId,
                                                 final String originalContent,
                                                 final String clientId,
                                                 final String updatedContent) {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(), clientDataStore);
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId)).iterator().next();
    }

}
