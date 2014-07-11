package org.jboss.aerogear.sync;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.aerogear.sync.diffsync.DefaultClientDocument;
import org.jboss.aerogear.sync.diffsync.DefaultEdits;
import org.jboss.aerogear.sync.diffsync.Edit;
import org.jboss.aerogear.sync.diffsync.Edits;
import org.jboss.aerogear.sync.diffsync.client.ClientInMemoryDataStore;
import org.jboss.aerogear.sync.diffsync.client.ClientSyncEngine;
import org.jboss.aerogear.sync.diffsync.client.DefaultClientSynchronizer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMapperTest {

    @Test
    public void serializeEdits() {
        final Edits edits = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(edits);
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
    public void deserializeEdits() {
        final Edits edits = generateClientSideEdits("1234", "version1", "client1", "version2");
        final DefaultEdits deserialized = JsonMapper.fromJson(JsonMapper.toJson(edits), DefaultEdits.class);
        assertThat(deserialized.edits().size(), is(1));
        final Edit edit = deserialized.edits().peek();
        assertThat(edit.documentId(), equalTo("1234"));
        assertThat(edit.clientId(), equalTo("client1"));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.clientVersion(), is(0L));
        assertThat(edit.diffs().size(), is(3));
    }

    @Test
    public void serializeEdit() {
        final Edits edits = generateClientSideEdits("1234", "version1", "client1", "version2");
        final String json = JsonMapper.toJson(edits.edits().peek());
        final JsonNode edit = JsonMapper.asJsonNode(json);
        assertThat(edit.get("clientId").asText(), equalTo("client1"));
        assertThat(edit.get("id").asText(), equalTo("1234"));
        assertThat(edit.get("serverVersion").asText(), equalTo("0"));
        assertThat(edit.get("clientVersion").asText(), equalTo("0"));
        final JsonNode diffs = edit.get("diffs");
        assertThat(diffs.isArray(), is(true));
        assertThat(diffs.size(), is(3));
    }

    private static Edits generateClientSideEdits(final String documentId,
                                                       final String originalContent,
                                                       final String clientId,
                                                       final String updatedContent) {
        final ClientSyncEngine<String> clientSyncEngine = new ClientSyncEngine<String>(new DefaultClientSynchronizer(),
                new ClientInMemoryDataStore());
        clientSyncEngine.addDocument(new DefaultClientDocument<String>(documentId, originalContent, clientId));
        final DefaultClientDocument<String> doc = new DefaultClientDocument<String>(documentId, updatedContent, clientId);
        return clientSyncEngine.diff(doc);
    }
}
