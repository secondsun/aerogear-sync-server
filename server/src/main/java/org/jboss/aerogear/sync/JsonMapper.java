package org.jboss.aerogear.sync;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.SimpleModule;

import java.io.IOException;
import java.io.StringWriter;

public final class JsonMapper {

    private static ObjectMapper om = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        om = new ObjectMapper();
        final SimpleModule module = new SimpleModule("MyModule", new Version(1, 0, 0, null));
        module.addDeserializer(Document.class, new DocumentDeserializer());
        module.addSerializer(DefaultDocument.class, new DocumentSerializer());
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
}

