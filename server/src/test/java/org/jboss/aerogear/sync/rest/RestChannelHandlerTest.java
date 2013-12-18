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
package org.jboss.aerogear.sync.rest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.Test;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RestChannelHandlerTest {

    @Test (expected = NullPointerException.class)
    public void nullRestHandlerShouldThrow() {
        new EmbeddedChannel(new RestChannelHandler(null));
    }

    @Test
    public void handleGetRequest() {
        final EmbeddedChannel channel = withMockGetHandler();
        channel.writeInbound(new DefaultHttpRequest(HTTP_1_1, GET, "/app/resource"));
        final HttpResponse response = channel.readOutbound();
        assertThat(response.getStatus(), is(OK));
    }

    @Test
    public void handlePutRequest() {
        final EmbeddedChannel channel = withMockPutHandler();
        channel.writeInbound(new DefaultHttpRequest(HTTP_1_1, PUT, "/app/resource"));
        final HttpResponse response = channel.readOutbound();
        assertThat(response.getStatus(), is(OK));
    }

    @Test
    public void handlePostRequest() {
        final EmbeddedChannel channel = withMockPostHandler();
        channel.writeInbound(new DefaultHttpRequest(HTTP_1_1, POST, "/app/resource"));
        final HttpResponse response = channel.readOutbound();
        assertThat(response.getStatus(), is(CREATED));
    }

    @Test
    public void handleDeleteRequest() {
        final EmbeddedChannel channel = withMockDeleteHandler();
        channel.writeInbound(new DefaultHttpRequest(HTTP_1_1, DELETE, "/app/resource"));
        final HttpResponse response = channel.readOutbound();
        assertThat(response.getStatus(), is(NO_CONTENT));
    }

    private static EmbeddedChannel withMockGetHandler() {
        final RestProcessor restProcessor = mock(RestProcessor.class);
        when(restProcessor.processGet(any(HttpRequest.class), any(ChannelHandlerContext.class))).thenReturn(okResponse());
        return new EmbeddedChannel(new RestChannelHandler(restProcessor));
    }

    private static EmbeddedChannel withMockPutHandler() {
        final RestProcessor restProcessor = mock(RestProcessor.class);
        when(restProcessor.processPut(any(HttpRequest.class), any(ChannelHandlerContext.class))).thenReturn(okResponse());
        return new EmbeddedChannel(new RestChannelHandler(restProcessor));
    }

    private static EmbeddedChannel withMockPostHandler() {
        final RestProcessor r = mock(RestProcessor.class);
        when(r.processPost(any(HttpRequest.class), any(ChannelHandlerContext.class))).thenReturn(postOkResponse());
        return new EmbeddedChannel(new RestChannelHandler(r));
    }

    private static EmbeddedChannel withMockDeleteHandler() {
        final RestProcessor r = mock(RestProcessor.class);
        when(r.processDelete(any(HttpRequest.class), any(ChannelHandlerContext.class))).thenReturn(deleteOkResponse());
        return new EmbeddedChannel(new RestChannelHandler(r));
    }

    private static HttpResponse okResponse() {
        return new DefaultHttpResponse(HTTP_1_1, OK);
    }

    private static HttpResponse postOkResponse() {
        return new DefaultHttpResponse(HTTP_1_1, CREATED);
    }

    private static HttpResponse deleteOkResponse() {
        return new DefaultHttpResponse(HTTP_1_1, NO_CONTENT);
    }
}
