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
package org.jboss.aerogear.sync;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aerogear.sync.diffsync.DefaultDiff;
import org.jboss.aerogear.sync.diffsync.DefaultEdits;
import org.jboss.aerogear.sync.diffsync.Diff;
import org.jboss.aerogear.sync.diffsync.Edits;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;

public final class JsonMapper {

    private static ObjectMapper om = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        om = new ObjectMapper();
        final SimpleModule module = new SimpleModule("MyModule", new Version(1, 0, 0, null, "aerogear", "sync"));
        module.addDeserializer(Document.class, new DocumentDeserializer());
        module.addSerializer(DefaultDocument.class, new DocumentSerializer());
        module.addDeserializer(Edits.class, new EditsDeserializer());
        module.addSerializer(DefaultEdits.class, new EditsSerializer());
        om.registerModule(module);
        return om;
    }

    private JsonMapper() {
    }

    /**
     * Transforms from JSON to the type specified.
     *
     * @param json the json to be transformed.
     * @param type the Java type that the JSON should be transformed to.
     * @return T an instance of the type populated with data from the json message.
     */
    public static <T> T fromJson(final String json, final Class<T> type) {
        try {
            return om.readValue(json, type);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    /**
     * Transforms from Java object notation to JSON.
     *
     * @param obj the Java object to transform into JSON.
     * @return {@code String} the json representation for the object.
     */
    public static String toJson(final Object obj) {
        final StringWriter stringWriter = new StringWriter();
        try {
            om.writeValue(stringWriter, obj);
            return stringWriter.toString();
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + obj + ']', e);
        }
    }

    /**
     * Allows for the creation of an incomplete {@link org.jboss.aerogear.sync.Document instance}
     *
     * @param id the documents documentId.
     * @param json the contents for the document.
     * @return {@code Document} the document which may not have a revision and/or a content field.
     */
    public static Document partialDocument(final String id, final String json) {
        final JsonFactory factory = om.getFactory();
        try {
            final JsonParser parser = factory.createParser(json);
            final ObjectCodec codec = parser.getCodec();
            final JsonNode node = codec.readTree(parser);
            final JsonNode revisionNode = node.get("rev");
            final String revision = revisionNode == null ? null : revisionNode.asText();
            final JsonNode contentNode = node.get("content");
            final String content = contentNode == null ? null : contentNode.toString();
            return new DefaultDocument(id, revision, content);
        } catch (final Exception e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    /**
     * Return a {@link JsonNode} for the passed in JSON string.
     *
     * @param json the string to be parsed.
     * @return JsonNode the JsonNode representing the passed-in JSON string.
     */
    public static JsonNode asJsonNode(final String json) {
        try {
            return om.readTree(json);
        } catch (final IOException e) {
            throw new RuntimeException("error trying to parse json [" + json + ']', e);
        }
    }

    public static ObjectNode newObjectNode() {
        return om.createObjectNode();
    }

    public static ArrayNode newArrayNode() {
        return om.createArrayNode();
    }

    private static class DocumentDeserializer extends JsonDeserializer<Document> {

        @Override
        public Document deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode node = oc.readTree(jp);
            return new DefaultDocument(node.get("id").asText(), node.get("rev").asText(), node.get("content").asText());
        }
    }

    private static class DocumentSerializer extends JsonSerializer<Document> {

        @Override
        public void serialize(final Document document,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeFieldName("id");
            jgen.writeString(document.id());
            jgen.writeFieldName("rev");
            jgen.writeString(document.revision());
            jgen.writeFieldName("content");
            jgen.writeString(document.content());
            jgen.writeEndObject();
        }
    }

    private static class EditsDeserializer extends JsonDeserializer<Edits> {

        @Override
        public Edits deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
            final ObjectCodec oc = jp.getCodec();
            final JsonNode node = oc.readTree(jp);
            final String clientId = node.get("clientId").asText();
            final String documentId = node.get("id").asText();
            final long version = node.get("version").asLong();
            final String checksum = node.get("checksum").asText();
            final JsonNode diffsNode = node.get("diffs");
            final LinkedList<Diff> diffs = new LinkedList<Diff>();
            if (diffsNode.isArray()) {
                for (JsonNode d : diffsNode) {
                    diffs.add(new DefaultDiff(Diff.Operation.valueOf(d.get("operation").asText()), d.get("text").asText()));
                }
            }
            return new DefaultEdits(clientId, documentId, version, checksum, diffs);
        }
    }

    private static class EditsSerializer extends JsonSerializer<Edits> {

        @Override
        public void serialize(final Edits edits,
                              final JsonGenerator jgen,
                              final SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("msgType", "patch");
            jgen.writeStringField("clientId", edits.clientId());
            jgen.writeStringField("id", edits.documentId());
            jgen.writeNumberField("version", edits.version());
            jgen.writeStringField("checksum", edits.checksum());
            if (!edits.diffs().isEmpty()) {
                jgen.writeArrayFieldStart("diffs");
                for (Diff diff : edits.diffs()) {
                    jgen.writeStartObject();
                    jgen.writeStringField("operation", diff.operation().toString());
                    jgen.writeStringField("text", diff.text());
                    jgen.writeEndObject();
                }
                jgen.writeEndArray();
            }
            jgen.writeEndObject();
        }
    }
}

