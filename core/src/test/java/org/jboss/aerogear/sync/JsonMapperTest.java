package org.jboss.aerogear.sync;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.sync.Diff.Operation;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMapperTest {

    @Test
    public void serializeEdits() {
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage patchMessage = patchMessage(documentId, clientId,
                DefaultEdit.withDocumentId(documentId)
                        .clientId(clientId)
                        .diff(new DefaultDiff(Operation.UNCHANGED, "version"))
                        .diff(new DefaultDiff(Operation.DELETE, "1"))
                        .diff(new DefaultDiff(Operation.ADD, "2"))
                        .build());

        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo(documentId));
        assertThat(jsonNode.get("clientId").asText(), equalTo(clientId));
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

    private static PatchMessage patchMessage(final String docId, final String clientId, Edit... edit) {
        return new DefaultPatchMessage(docId, clientId, new LinkedList<Edit>(Arrays.asList(edit)));
    }

    @Test
    public void serializeEditsWithArray() {
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage patchMessage = patchMessage(documentId, clientId,
                DefaultEdit.withDocumentId(documentId)
                        .clientId(clientId)
                        .diff(new DefaultDiff(Operation.UNCHANGED, "{\"content\": [\"one\", \""))
                        .diff(new DefaultDiff(Operation.ADD, "tw"))
                        .diff(new DefaultDiff(Operation.UNCHANGED, "o"))
                        .diff(new DefaultDiff(Operation.DELETE, "ne"))
                        .diff(new DefaultDiff(Operation.UNCHANGED, "\"]}"))
                        .build());
        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo(documentId));
        assertThat(jsonNode.get("clientId").asText(), equalTo(clientId));
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
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage patchMessage = patchMessage(documentId, clientId,
                DefaultEdit.withDocumentId(documentId)
                        .clientId(clientId)
                        .diff(new DefaultDiff(Operation.UNCHANGED, "{\"content\": [\"one\", \""))
                        .diff(new DefaultDiff(Operation.ADD, "tw"))
                        .diff(new DefaultDiff(Operation.UNCHANGED, "o"))
                        .diff(new DefaultDiff(Operation.DELETE, "ne"))
                        .diff(new DefaultDiff(Operation.UNCHANGED, "\"]}"))
                        .build());
        final String json = JsonMapper.toJson(patchMessage);
        final JsonNode jsonNode = JsonMapper.asJsonNode(json);
        assertThat(jsonNode.get("msgType").asText(), equalTo("patch"));
        assertThat(jsonNode.get("id").asText(), equalTo(documentId));
        assertThat(jsonNode.get("clientId").asText(), equalTo(clientId));
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
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage patchMessage = patchMessage(documentId, clientId,
                DefaultEdit.withDocumentId(documentId)
                        .clientId(clientId)
                        .diff(new DefaultDiff(Operation.UNCHANGED, "version"))
                        .diff(new DefaultDiff(Operation.DELETE, "1"))
                        .diff(new DefaultDiff(Operation.ADD, "2"))
                        .build());
        final DefaultPatchMessage deserialized = JsonMapper.fromJson(JsonMapper.toJson(patchMessage), DefaultPatchMessage.class);
        assertThat(deserialized.edits().size(), is(1));
        final Edit edit = deserialized.edits().peek();
        assertThat(edit.documentId(), equalTo(documentId));
        assertThat(edit.clientId(), equalTo(clientId));
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
        final String documentId = "1234";
        final String clientId = "client1";
        final PatchMessage patchMessage = patchMessage(documentId, clientId,
                DefaultEdit.withDocumentId(documentId)
                        .clientId(clientId)
                        .diff(new DefaultDiff(Operation.UNCHANGED, "version"))
                        .diff(new DefaultDiff(Operation.DELETE, "1"))
                        .diff(new DefaultDiff(Operation.ADD, "2"))
                        .build());
        final String json = JsonMapper.toJson(patchMessage.edits().peek());
        final JsonNode edit = JsonMapper.asJsonNode(json);
        assertThat(edit.get("clientId").asText(), equalTo(clientId));
        assertThat(edit.get("id").asText(), equalTo(documentId));
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

}
