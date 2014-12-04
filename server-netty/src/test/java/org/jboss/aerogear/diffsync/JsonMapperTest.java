package org.jboss.aerogear.diffsync;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.diffsync.client.DefaultClientSynchronizer;
import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMapperTest {

    @Test
    public void serializeEdits() {
        final PatchMessage patchMessage = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo("1234"));
        assertThat(jsonNode.get("clientId").asText(), equalTo("client1"));
        final JsonNode editsNode = jsonNode.get("edits");
        assertThat(editsNode.isArray(), is(true));
        assertThat(editsNode.size(), is(1));
        final JsonNode edit = editsNode.iterator().next();
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        assertThat(diffs.size(), is(3));
    }

    @Test
    public void serializeEditsWithArray() {
        final String content = "{\"content\": [\"one\", \"one\"]}";
        final String content2 = "{\"content\": [\"one\", \"two\"]}";
        final PatchMessage patchMessage = generateClientSideEdits("1234", content, "client1", content2);
        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo("1234"));
        assertThat(jsonNode.get("clientId").asText(), equalTo("client1"));
        final JsonNode editsNode = jsonNode.get("edits");
        assertThat(editsNode.isArray(), is(true));
        assertThat(editsNode.size(), is(1));
        final JsonNode edit = editsNode.iterator().next();
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        assertThat(diffs.size(), is(5));
        assertThat(diffs.get(0).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(0).get("text").asText(), equalTo("{\"content\": [\"one\", \""));
        assertThat(diffs.get(1).get("operation").asText(), equalTo("ADD"));
        assertThat(diffs.get(1).get("text").asText(), equalTo("tw"));
        assertThat(diffs.get(2).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(2).get("text").asText(), equalTo("o"));
        assertThat(diffs.get(3).get("operation").asText(), equalTo("DELETE"));
        assertThat(diffs.get(3).get("text").asText(), equalTo("ne"));
        assertThat(diffs.get(4).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(4).get("text").asText(), equalTo("\"]}"));
    }

    @Test
    public void serializeEditsWithArrayToJsonAndBack() {
        final String content = "{\"content\": [\"one\", \"one\"]}";
        final String content2 = "{\"content\": [\"one\", \"two\"]}";
        final PatchMessage patchMessage = generateClientSideEdits("1234", content, "client1", content2);
        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo("1234"));
        assertThat(jsonNode.get("clientId").asText(), equalTo("client1"));
        final JsonNode editsNode = jsonNode.get("edits");
        assertThat(editsNode.isArray(), is(true));
        assertThat(editsNode.size(), is(1));
        final JsonNode edit = editsNode.iterator().next();
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        assertThat(diffs.size(), is(5));
        assertThat(diffs.get(0).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(0).get("text").asText(), equalTo("{\"content\": [\"one\", \""));
        assertThat(diffs.get(1).get("operation").asText(), equalTo("ADD"));
        assertThat(diffs.get(1).get("text").asText(), equalTo("tw"));
        assertThat(diffs.get(2).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(2).get("text").asText(), equalTo("o"));
        assertThat(diffs.get(3).get("operation").asText(), equalTo("DELETE"));
        assertThat(diffs.get(3).get("text").asText(), equalTo("ne"));
        assertThat(diffs.get(4).get("operation").asText(), equalTo("UNCHANGED"));
        assertThat(diffs.get(4).get("text").asText(), equalTo("\"]}"));
    }

    @Test
    public void deserializeEdits() {
        final PatchMessage patchMessage = generateClientSideEdits("1234", "version1", "client1", "version2");
        final DefaultPatchMessage deserialized = JsonMapper.fromJson(JsonMapper.toJson(patchMessage), DefaultPatchMessage.class);
        assertThat(deserialized.edits().size(), is(1));
        final Edit edit = deserialized.edits().peek();
        assertThat(edit.documentId(), equalTo("1234"));
        assertThat(edit.clientId(), equalTo("client1"));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.diffs().size(), is(3));
    }

    @Test
    public void deserializeEditsWithNullElement() {
        final String json = "{\"msgType\":\"patch\",\"id\":\"1234\",\"clientId\":\"client1\",\"edits\":[null]}";
        final DefaultPatchMessage deserialized = JsonMapper.fromJson(json, DefaultPatchMessage.class);
        assertThat(deserialized.edits().isEmpty(), is(true));
    }

    @Test
    public void deserializeEditsWithNullDiffElement() {
        final String json = "{\"msgType\":\"patch\",\"id\":\"1234\",\"clientId\":\"client1\",\"edits\":[{\"clientVersion\":0,\"serverVersion\":0,\"checksum\":\"73ceb67f36054ea2c697aa7b587234ea3776f27f\",\"diffs\":[null]}]}";
        final DefaultPatchMessage deserialized = JsonMapper.fromJson(json, DefaultPatchMessage.class);
        assertThat(deserialized.edits().size(), is(1));
        final Edit edit = deserialized.edits().peek();
        assertThat(edit.documentId(), equalTo("1234"));
        assertThat(edit.clientId(), equalTo("client1"));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.diffs().isEmpty(), is(true));
    }

    @Test
    public void serializeEdit() {
        final PatchMessage patchMessage = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(patchMessage.edits().peek());
        final JsonNode edit = JsonMapper.asJsonNode(json);
        assertThat(edit.get("clientId").asText(), equalTo("client1"));
        assertThat(edit.get("id").asText(), equalTo("1234"));
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        assertThat(diffs.size(), is(3));
    }

    @Test
    public void asJsonNode() {
        final String json = "{\"content\": [\"one\", \"two\"]}";
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        final JsonNode contentNode = jsonNode.get("content");
        assertThat(contentNode.isArray(), is(true));
        assertThat(contentNode.size(), is(2));
        final Iterator<JsonNode> elements = contentNode.elements();
        assertThat(elements.next().asText(), equalTo("one"));
        assertThat(elements.next().asText(), equalTo("two"));
    }

    private static PatchMessage generateClientSideEdits(final String documentId,
                                                       final String originalContent,
                                                       final String clientId,
                                                       final String updatedContent) {
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(),
                new ClientInMemoryDataStore());
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, clientId, originalContent));
        final DefaultClientDocument<String> doc = new DefaultClientDocument<String>(documentId, clientId, updatedContent);
        return clientSyncEngine.diff(doc);
    }
}
