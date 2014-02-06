package org.jboss.aerogear.sync.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
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

/**
 * This class uses Mock object as the underlying SyncManager implementation. When adding
 * tests please bar this in mind as you may need to specify the behaviour that your tests
 * expect.
 */
public class DefaultRestProcessorTest {

    private static final String REV_ONE_JSON = "{\"state\":\"create\"}";
    private static final String REV_TWO_JSON = "{\"state\":\"update\"}";
    private static final Document REV_ONE_DOC = new DefaultDocument("1", "1", REV_ONE_JSON);
    private static final Document REV_TWO_DOC = new DefaultDocument("1", "2", REV_TWO_JSON);
    private static final String DELETED_REVISION = "mockDeletedRevision";
    private static final String DELETED_REVISION_RESPONSE = "{\"rev\":\"mockDeletedRevision\"}";

    @Test
    public void processPutNoContent() throws Exception {
        final HttpResponse response = restProcessor().processPut(mockRequest(PUT), mockContext());
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void processPutToCreate() throws Exception {
        final String id = UUID.randomUUID().toString();
        final FullHttpResponse response = (FullHttpResponse) restProcessor().processPut(mockRequest(PUT, id, REV_ONE_JSON), mockContext());
        assertThat(response.getStatus(), is(OK));
        final Document document = fromJson(response.content());
        assertThat(document.id(), equalTo(REV_ONE_DOC.id()));
        assertThat(document.revision(), equalTo(REV_ONE_DOC.revision()));
        assertThat(document.content(), equalTo(REV_ONE_DOC.content()));
        response.release();
    }

    @Test
    public void processPutToUpdate() throws Exception {
        final String id = UUID.randomUUID().toString();
        final RestProcessor restProcessor = restProcessor();
        // create the initial revision of the document
        restProcessor.processPut(mockRequest(PUT, id, REV_ONE_JSON), mockContext());
        // update the document
        final String updateBody = JsonMapper.toJson(new DefaultDocument(id, "1", REV_TWO_JSON));
        final FullHttpRequest fullHttpRequest = mockRequest(PUT, id, updateBody);
        final FullHttpResponse response = (FullHttpResponse) restProcessor.processPut(fullHttpRequest, mockContext());
        assertThat(response.getStatus(), is(OK));
        final Document document = fromJson(response.content());
        assertThat(document.id(), equalTo(REV_TWO_DOC.id()));
        assertThat(document.revision(), equalTo(REV_TWO_DOC.revision()));
        assertThat(document.content(), equalTo(REV_TWO_DOC.content()));
        response.release();
    }

    @Test
    public void processPutWithConflict() throws Exception {
        final String id = UUID.randomUUID().toString();
        final RestProcessor restProcessor = restProcessor();
        // create the initial revision of the document
        final FullHttpResponse putResponse = (FullHttpResponse) restProcessor.processPut(mockRequest(PUT, id, REV_ONE_JSON), mockContext());
        assertThat(putResponse.getStatus(), is(OK));
        putResponse.release();

        // update the document with
        final FullHttpRequest updateRequest = mockRequest(PUT, id, JsonMapper.toJson(new DefaultDocument(id, "2", REV_TWO_JSON)));
        final FullHttpResponse updateResponse = (FullHttpResponse) restProcessor.processPut(updateRequest, mockContext());
        assertThat(updateResponse.getStatus(), is(OK));
        updateResponse.release();

        // update the doument but this time with an older revision
        final FullHttpRequest conflictRequest = mockRequest(PUT, id, JsonMapper.toJson(new DefaultDocument(id, "1", REV_TWO_JSON)));
        final FullHttpResponse conflictResponse = (FullHttpResponse) restProcessor.processPut(conflictRequest, mockContext());
        assertThat(conflictResponse.getStatus(), is(CONFLICT));
        conflictResponse.release();
    }

    @Test
    public void processGet() throws Exception {
        final String id = UUID.randomUUID().toString();
        final Document doc = put(id, REV_ONE_JSON);
        final FullHttpResponse response = (FullHttpResponse) restProcessor().processGet(mockRequest(GET, doc.id()), mockContext());
        assertThat(response.getStatus(), is(OK));
        final Document document = fromJson(response.content());
        assertThat(document.id(), equalTo(doc.id()));
        assertThat(document.revision(), equalTo(doc.revision()));
        assertThat(document.content(), equalTo(REV_ONE_JSON));
        response.release();
    }

    @Test
    public void processDelete() throws Exception {
        final String id = UUID.randomUUID().toString();
        final Document doc = put(id, REV_ONE_JSON);
        final String deleteJson = "{\"rev\": \"" + doc.revision() + "\"}";
        final HttpRequest request = mockRequest(DELETE, doc.id(), deleteJson);
        final FullHttpResponse response = (FullHttpResponse) restProcessor().processDelete(request, mockContext());
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo(DELETED_REVISION_RESPONSE));
        response.release();
    }

    @Test
    public void processPost() throws Exception {
        final HttpResponse response = restProcessor().processPost(mockRequest(POST, "1234"), mockContext());
        assertThat(response.getStatus(), is(NOT_IMPLEMENTED));
    }

    private static Document put(final String id, final String json) throws Exception {
        final HttpResponse postResponse = restProcessor().processPut(mockRequest(PUT, '/' + id, json), mockContext());
        return JsonMapper.fromJson(((ByteBufHolder) postResponse).content().toString(UTF_8), Document.class);
    }

    /*
     * This method sets up a RestProcessor that uses a mocked SyncManager.
     */
    private static RestProcessor restProcessor() throws Exception {
        return new DefaultRestProcessor(mockSyncManager()
        );
    }

    private static SyncManager mockSyncManager() throws Exception {
        final SyncManager syncManager = mock(SyncManager.class);
        when(syncManager.create(anyString(), anyString())).thenReturn(REV_ONE_DOC);
        when(syncManager.read(anyString())).thenReturn(REV_ONE_DOC);
        when(syncManager.update(Matchers.any(Document.class)))
                .thenReturn(REV_ONE_DOC)
                .thenReturn(REV_TWO_DOC)
                .thenThrow(new ConflictException(REV_TWO_DOC, REV_ONE_DOC));
        when(syncManager.delete(anyString(), anyString())).thenReturn(DELETED_REVISION);
        return syncManager;
    }

    public static HttpRequest mockRequest(final HttpMethod method) {
        return mockRequest(method, "someid");
    }

    public static HttpRequest mockRequest(final HttpMethod method, final String id) {
        return mockRequest(method, id, null);
    }

    public static FullHttpRequest mockRequest(final HttpMethod method, final String id, final String body) {
        final String path = '/' + id;
        if (body == null) {
            return new DefaultFullHttpRequest(HTTP_1_1, method, path);
        }
        final String payload = "{\"content\":" + body + '}';
        return new DefaultFullHttpRequest(HTTP_1_1, method, path, Unpooled.copiedBuffer(payload, UTF_8));
    }

    public static ChannelHandlerContext mockContext() {
        return mock(ChannelHandlerContext.class);
    }

    public static Document fromJson(final ByteBuf content) {
        return JsonMapper.fromJson(content.toString(UTF_8), Document.class);
    }
}
