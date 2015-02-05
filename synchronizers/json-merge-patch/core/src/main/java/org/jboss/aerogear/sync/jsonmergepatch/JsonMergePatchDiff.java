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
package org.jboss.aerogear.sync.jsonmergepatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.util.Arguments;

public class JsonMergePatchDiff implements Diff {

    private final JsonMergePatch jsonMergePatch;
    private final int jsonNodeHashCode;

    private JsonMergePatchDiff(final JsonMergePatch jsonMergePatch, final int jsonNodeHashCode) {
        this.jsonMergePatch = Arguments.checkNotNull(jsonMergePatch, "jsonMergePatch must not be null");
        this.jsonNodeHashCode = Arguments.checkNotNull(jsonNodeHashCode, "jsonNodeHashCode must not be null");
    }

    public JsonMergePatch jsonMergePatch() {
        return jsonMergePatch;
    }

    @Override
    public String toString() {
        return "JsonMergePatchDiff[jsonMergePatch=" + jsonMergePatch + ']';
    }

    public static JsonMergePatchDiff fromJsonNode(final JsonNode jsonNode) {
        try {
            return new JsonMergePatchDiff(JsonMergePatch.fromJson(jsonNode), jsonNode.hashCode());
        } catch (final JsonPatchException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JsonMergePatchDiff that = (JsonMergePatchDiff) o;
        return jsonNodeHashCode == that.jsonNodeHashCode;
    }

    @Override
    public int hashCode() {
        int result = jsonMergePatch.hashCode();
        result = 31 * result + jsonNodeHashCode;
        return result;
    }
}
