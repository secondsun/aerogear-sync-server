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
package org.jboss.aerogear.sync.jsonpatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Utility class responsible for creating JSON Merge Patch {@link JsonPatchDiff}s,
 * {@link JsonPatchEdit}s, and {@link JsonPatchMessage}s from utit tests.
 */
public final class Patches {

    private static final ObjectMapper OM = new ObjectMapper();

    private Patches() {
    }

    public static JsonPatch jsonPatch(final String oldName, final String newName) {
        final ObjectNode source = OM.createObjectNode().put("name", oldName);
        final ObjectNode target = OM.createObjectNode().put("name", newName);
        return JsonDiff.asJsonPatch(source, target);
    }

    public static JsonPatch jsonPatch() {
        return jsonPatch("fletch", "Fletch");
    }

    public static JsonPatchEdit newJsonPatchEdit() {
        return newJsonPatchEdit(jsonPatch());
    }

    public static JsonPatchEdit newJsonPatchEdit(final JsonPatch patch) {
        return JsonPatchEdit.withPatch(patch).checksum("123").build();
    }

    public static JsonPatchMessage patchMessage(final String documentId,
                                                final String clientId,
                                                final JsonPatchEdit edit) {
        return new JsonPatchMessage(documentId, clientId, asQueue(edit));
    }

    public static JsonPatchMessage patchMessage(final JsonPatchEdit edit) {
        return patchMessage("docId", "clientid", edit);
    }

    public static Queue<JsonPatchEdit> asQueue(final JsonPatchEdit... edits) {
        return new LinkedList<JsonPatchEdit>(Arrays.asList(edits));
    }

}
