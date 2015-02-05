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

import org.jboss.aerogear.sync.PatchMessage;
import org.jboss.aerogear.sync.util.Arguments;

import java.util.Queue;

public class JsonMergePatchMessage implements PatchMessage<JsonMergePatchEdit> {

    private final String documentId;
    private final String clientId;
    private final Queue<JsonMergePatchEdit> edits;

    public JsonMergePatchMessage(final String documentId, final String clientId, final Queue<JsonMergePatchEdit> edits) {
        this.documentId = Arguments.checkNotNull(documentId, "documentId must not be null");
        this.clientId = Arguments.checkNotNull(clientId, "clientId must not be null");
        this.edits = Arguments.checkNotNull(edits, "edits must not be null");
    }

    @Override
    public String documentId() {
        return documentId;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public Queue<JsonMergePatchEdit> edits() {
        return edits;
    }

    @Override
    public String toString() {
        return "JsonMergePatchMessage[documentId=" + documentId + ", clientId=" + clientId + ", edits=" + edits + ']';
    }

    @Override
    public String asJson() {
        return JsonMapper.toJson(this);
    }

    @Override
    public JsonMergePatchMessage fromJson(final String json) {
        return JsonMapper.fromJson(json, JsonMergePatchMessage.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JsonMergePatchMessage that = (JsonMergePatchMessage) o;
        if (!clientId.equals(that.clientId)) {
            return false;
        }
        if (!documentId.equals(that.documentId)) {
            return false;
        }
        return edits.equals(that.edits);
    }

    @Override
    public int hashCode() {
        int result = documentId.hashCode();
        result = 31 * result + clientId.hashCode();
        result = 31 * result + edits.hashCode();
        return result;
    }
}
