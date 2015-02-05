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

import com.github.fge.jsonpatch.JsonPatch;
import org.jboss.aerogear.sync.Diff;
import org.jboss.aerogear.sync.util.Arguments;

public class JsonPatchDiff implements Diff {

    private final JsonPatch jsonPatch;

    public JsonPatchDiff(final JsonPatch jsonPatch) {
        this.jsonPatch = Arguments.checkNotNull(jsonPatch, "jsonPatch must not be null");
    }

    public JsonPatch jsonPatch() {
        return jsonPatch;
    }

    @Override
    public String toString() {
        return "JsonPatchDiff[jsonPatch=" + jsonPatch + ']';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JsonPatchDiff that = (JsonPatchDiff) o;
        return jsonPatch.toString().equals(that.jsonPatch.toString());
    }

    @Override
    public int hashCode() {
        return jsonPatch.hashCode();
    }
}
