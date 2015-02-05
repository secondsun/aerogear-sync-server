package org.jboss.aerogear.sync.jsonmergepatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Utility class responsible for creating JSON Merge Patch {@link JsonMergePatchDiff}s,
 * {@link JsonMergePatchEdit}s, and {@link JsonMergePatchMessage}s from utit tests.
 */
public final class Patches {

    private Patches() {
    }

    private static final ObjectMapper OM = new ObjectMapper();

    public static JsonMergePatchMessage patchMessage(final String documentId,
                                                     final String clientId,
                                                     final JsonMergePatchEdit edit) {
        return new JsonMergePatchMessage(documentId, clientId, asQueue(edit));
    }

    public static JsonMergePatchMessage patchMessage(final JsonMergePatchEdit edit) {
        return new JsonMergePatchMessage("docId", "clientId", asQueue(edit));
    }

    public static Queue<JsonMergePatchEdit> asQueue(final JsonMergePatchEdit... edits) {
        return new LinkedList<JsonMergePatchEdit>(Arrays.asList(edits));
    }

    public static JsonMergePatchEdit newJsonMergePatchEdit(final String name) {
        return JsonMergePatchEdit.withPatch(objectNode(name)).checksum("123").build();
    }

    public static ObjectNode objectNode(final String name) {
        return OM.createObjectNode().put("name", name);
    }

}
