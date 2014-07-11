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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.aerogear.sync.datastore.SyncDataStore;

import static io.netty.buffer.Unpooled.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.util.CharsetUtil.*;

/**
 * Processes HTTP GET, PUT, and DELETE requests to synchronize documents.
 * <p>
 * HTTP PUT is used to create new documents and to update existing documents. The choice of PUT over POST
 * is due because the documentId for the document is specified by the calling client as a path parameter.
 * For example:
 * <pre>
 * somepath/document22
 * </pre>
 * Since the client knows the location of the resource it can PUT the document directly. If the server
 * decided the location of the resource, is our case the document documentId, the a POST would have been
 * more appropriate.
 */
public class DefaultRestProcessor implements RestProcessor {

    private final SyncDataStore sync;

    public DefaultRestProcessor(final SyncDataStore sync) {
        this.sync = sync;
    }

    @Override
    public HttpResponse processGet(final HttpRequest request, final ChannelHandlerContext ctx) {
        try {
            final Document document = sync.read(extractId(request));
            return responseWithContent(request.protocolVersion(), OK, toJson(document));
        } catch (final DocumentNotFoundException e) {
            return new DefaultFullHttpResponse(request.protocolVersion(), NOT_FOUND);
        }
    }

    @Override
    public HttpResponse processPut(final HttpRequest request, final ChannelHandlerContext ctx) {
        if (request instanceof FullHttpRequest) {
            final FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            try {
                if (fullHttpRequest.content().isReadable()) {
                    try {
                        final Document updateDoc = JsonMapper.partialDocument(extractId(request), contentAsString(fullHttpRequest));
                        final Document updatedDoc = sync.update(updateDoc);
                        return responseWithContent(request.protocolVersion(), OK, toJson(updatedDoc));
                    } catch (final ConflictException e) {
                        return responseWithContent(request.protocolVersion(), CONFLICT, toJson(e.latest()));
                    }
                }
            } finally {
                fullHttpRequest.release();
            }
        }
        return new DefaultHttpResponse(request.protocolVersion(), BAD_REQUEST);
    }

    @Override
    public HttpResponse processPost(final HttpRequest request, final ChannelHandlerContext ctx) {
        return new DefaultHttpResponse(request.protocolVersion(), NOT_IMPLEMENTED);
    }

    @Override
    public HttpResponse processDelete(final HttpRequest request, final ChannelHandlerContext ctx) {
        if (request instanceof FullHttpRequest) {
            final FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            try {
                final String id = extractId(request);
                final Document doc = JsonMapper.partialDocument(id, contentAsString(fullHttpRequest));
                final ObjectNode revision = JsonMapper.newObjectNode();
                revision.put("rev", sync.delete(id, doc.revision()));
                return responseWithContent(request.protocolVersion(), OK, revision.toString());
            } finally {
                fullHttpRequest.release();
            }
        } else {
            return new DefaultHttpResponse(request.protocolVersion(), BAD_REQUEST);
        }
    }

    private static String contentAsString(final FullHttpRequest request) {
        return request.content().toString(UTF_8);
    }

    private static String extractId(final HttpRequest request) {
        final String path = new QueryStringDecoder(request.uri()).path();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String toJson(final Document doc) {
        return JsonMapper.toJson(doc);
    }

    private static FullHttpResponse responseWithContent(final HttpVersion version, final HttpResponseStatus status,
                                                        final String content) {
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(version, status, copiedBuffer(content, UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        return response;
    }
}
