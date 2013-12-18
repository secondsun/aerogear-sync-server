package org.jboss.aerogear.sync.rest;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.SyncManager;
import org.junit.Test;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.util.CharsetUtil.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

public class DefaultRestProcessorTest {

    @Test
    public void handlePostNoContent() {
        final HttpResponse response = restProcessor().processPost(mockRequest(POST, null), mockContext());
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void handlePost() {
        final String json = "{\"model\": \"name\"}";
        final HttpResponse response = restProcessor().processPost(mockRequest(POST, json), mockContext());
        assertThat(response.getStatus(), is(CREATED));
        assertThat(response, is(instanceOf(FullHttpResponse.class)));

        final FullHttpResponse full = (FullHttpResponse) response;
        final Document document = JsonMapper.fromJson(full.content().toString(UTF_8), Document.class);
        assertThat(document.id(), equalTo("1"));
        assertThat(document.revision(), equalTo("1"));
        assertThat(document.content(), equalTo(json));
    }

    private static RestProcessor restProcessor() {
        final SyncManager syncManager = mock(SyncManager.class);
        final Document document = new DefaultDocument("1", "1", "{\"model\": \"name\"}");
        when(syncManager.create(anyString())).thenReturn(document);
        return new DefaultRestProcessor(syncManager);
    }

    public static HttpRequest mockRequest(final HttpMethod method, final String body) {
        if (body == null) {
            return new DefaultFullHttpRequest(HTTP_1_0, method, "/dummypath");
        }
        return new DefaultFullHttpRequest(HTTP_1_0, method, "/dummypath", Unpooled.copiedBuffer(body, UTF_8));

    }

    public static ChannelHandlerContext mockContext() {
        return mock(ChannelHandlerContext.class);
    }
}