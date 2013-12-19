package org.jboss.aerogear.sync.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.jboss.aerogear.sync.*;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.UUID;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.util.CharsetUtil.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

public class DefaultRestProcessorTest {

    @Test
    public void handlePutNoContent() throws Exception {
        final HttpResponse response = restProcessor().processPut(mockRequest(PUT), mockContext());
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void processPut() throws Exception {
        final String json = "{\"content\": {\"model\": \"name\"}}";
        final String id = UUID.randomUUID().toString();
        final HttpResponse response = restProcessor().processPut(mockRequest(PUT, '/' + id, json), mockContext());
        assertThat(response.getStatus(), is(CREATED));
        assertThat(response, is(instanceOf(FullHttpResponse.class)));
        final Document document = fromJson(((ByteBufHolder) response).content());
        assertThat(document.id(), equalTo("1"));
        assertThat(document.revision(), equalTo("1"));
        assertThat(document.content(), equalTo(json));
    }

    @Test
    public void processGet() throws Exception {
        final String json = "{\"content\": {\"model\": \"name\"}}";
        final String id = UUID.randomUUID().toString();
        final Document doc = put(id, json);
        final HttpResponse response = restProcessor().processGet(mockRequest(GET, '/' + doc.id()), mockContext());
        assertThat(response.getStatus(), is(OK));
        final Document document = fromJson(((ByteBufHolder) response).content());
        assertThat(document.id(), equalTo(doc.id()));
        assertThat(document.revision(), equalTo(doc.revision()));
        assertThat(document.content(), equalTo(json));
    }

    @Test
    public void processDelete() throws Exception {
        final String json = "{\"content\": {\"model\": \"name\"}}";
        final String id = UUID.randomUUID().toString();
        final Document doc = put(id, json);

        final String deleteJson = "{\"rev\": \"" + doc.revision() + "\"}";
        final HttpResponse response = restProcessor().processDelete(mockRequest(DELETE, '/' + doc.id(), deleteJson), mockContext());
        assertThat(response.getStatus(), is(OK));
        assertThat(response, is(instanceOf(FullHttpResponse.class)));
        final FullHttpResponse fullHttpResponse = (FullHttpResponse) response;
        assertThat(fullHttpResponse.content().isReadable(), is(true));
        assertThat(fullHttpResponse.content().toString(UTF_8), equalTo("mockDeletedRevision"));
    }

    @Test
    public void processPost() throws Exception {
        final HttpResponse response = restProcessor().processPost(mockRequest(POST, "/1234"), mockContext());
        assertThat(response.getStatus(), is(NOT_IMPLEMENTED));
    }

    private static Document put(final String id, final String json) throws Exception {
        final HttpResponse postResponse = restProcessor().processPut(mockRequest(PUT, '/' + id, json), mockContext());
        return JsonMapper.fromJson(((ByteBufHolder) postResponse).content().toString(UTF_8), Document.class);
    }

    private static RestProcessor restProcessor() throws Exception {
        final SyncManager syncManager = mock(SyncManager.class);
        final Document document = new DefaultDocument("1", "1", "{\"content\": {\"model\": \"name\"}}");
        when(syncManager.create(anyString(), anyString())).thenReturn(document);
        when(syncManager.read(anyString())).thenReturn(document);
        when(syncManager.update(Matchers.any(Document.class))).thenReturn(document);
        when(syncManager.delete(anyString(), anyString())).thenReturn("mockDeletedRevision");
        return new DefaultRestProcessor(syncManager);
    }

    public static HttpRequest mockRequest(final HttpMethod method) {
        return mockRequest(method, "/");
    }

    public static HttpRequest mockRequest(final HttpMethod method, final String path) {
        return mockRequest(method, path, null);
    }

    public static HttpRequest mockRequest(final HttpMethod method, final String path, final String body) {
        if (body == null) {
            return new DefaultFullHttpRequest(HTTP_1_0, method, path);
        }
        return new DefaultFullHttpRequest(HTTP_1_0, method, path, Unpooled.copiedBuffer(body, UTF_8));
    }

    public static ChannelHandlerContext mockContext() {
        return mock(ChannelHandlerContext.class);
    }

    public static Document fromJson(final ByteBuf content) {
        return JsonMapper.fromJson(content.toString(UTF_8), Document.class);
    }
}
