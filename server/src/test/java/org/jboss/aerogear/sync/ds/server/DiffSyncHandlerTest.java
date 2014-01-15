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
package org.jboss.aerogear.sync.ds.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.ds.DefaultClientDocument;
import org.jboss.aerogear.sync.ds.Diff;
import org.jboss.aerogear.sync.ds.Edits;
import org.jboss.aerogear.sync.ds.client.ClientDataStore;
import org.jboss.aerogear.sync.ds.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.ds.client.ClientSyncEngine;
import org.jboss.aerogear.sync.ds.client.DefaultClientSynchronizer;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
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
        final JsonNode json = sendAddDocMsg(docId, "Once upon a time", channel);
        assertThat(json.get("result").asText(), equalTo("CREATED"));
    }

    @Test
    public void addShadowDocument() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        sendAddDocMsg(docId, "Once upon a time", channel);
        final JsonNode json = sendAddShadowMsg(docId, clientId, channel);
        assertThat(json.get("result").asText(), equalTo("CREATED"));
    }

    @Test @Ignore ("work in progress")
    public void edits() {
        final EmbeddedChannel channel = embeddedChannel();
        final String docId = UUID.randomUUID().toString();
        final String clientId = "client1";
        final String originalContent = "Once upon a time";
        sendAddDocMsg(docId, originalContent, channel);
        sendAddShadowMsg(docId, clientId, channel);
        final Edits edits = generateClientSideEdits(docId, originalContent, clientId, "Once upon a time,");
        final JsonNode json = sendEditMsg(edits, channel);
        assertThat(json.get("result").asText(), equalTo("PATCHED"));
    }

    private static JsonNode sendEditMsg(final Edits edits, final EmbeddedChannel ch) {
        final ObjectNode objectNode = JsonMapper.newObjectNode();
        objectNode.put("msgType", "edits");
        objectNode.put("docId", edits.documentId());
        objectNode.put("version", edits.version());
        objectNode.put("checksum", edits.checksum());
        if (!edits.diffs().isEmpty()) {
            final ArrayNode diffArray = objectNode.putArray("diffs");
            for (Diff diff : edits.diffs()) {
                final ObjectNode diffNode = diffArray.addObject();
                diffNode.put(diff.operation().toString(), diff.text());
            }
        }
        System.out.println(objectNode);
        return writeTextFrame(objectNode.toString(), ch);
    }

    private static JsonNode sendAddDocMsg(final String docId, final String content, final EmbeddedChannel ch) {
        final ObjectNode docMsg = message("add");
        docMsg.put("msgType", "add");
        docMsg.put("docId", docId);
        docMsg.put("content", content);
        return writeTextFrame(docMsg.toString(), ch);
    }

    private static JsonNode sendAddShadowMsg(final String docId, final String clientId, final EmbeddedChannel ch) {
        final ObjectNode shadowMsg = message("shadow");
        shadowMsg.put("docId", docId);
        shadowMsg.put("clientId", clientId);
        return writeTextFrame(shadowMsg.toString(), ch);
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
        final ServerSynchronizer<String> synchronizer = new DefaultServerSynchronizer();
        final ServerInMemoryDataStore dataStore = new ServerInMemoryDataStore();
        final ServerSyncEngine<String> syncEngine = new ServerSyncEngine<String>(synchronizer, dataStore);
        return new EmbeddedChannel(new DiffSyncHandler(syncEngine));
    }

    private static Edits generateClientSideEdits(final String documentId,
                                                 final String originalContent,
                                                 final String clientId,
                                                 final String updatedContent) {
        final ClientDataStore<String> clientDataStore = new ClientInMemoryDataStore();
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(), clientDataStore);
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        return clientSyncEngine.diff(new DefaultClientDocument<String>(documentId, updatedContent, clientId));
    }

}
