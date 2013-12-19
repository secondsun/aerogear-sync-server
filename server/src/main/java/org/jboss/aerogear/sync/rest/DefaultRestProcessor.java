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
import io.netty.handler.codec.http.*;
import org.jboss.aerogear.sync.*;

import static io.netty.buffer.Unpooled.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.util.CharsetUtil.*;

public class DefaultRestProcessor implements RestProcessor {

    private final SyncManager sync;

    public DefaultRestProcessor(final SyncManager sync) {
        this.sync = sync;
    }

    @Override
    public HttpResponse processGet(final HttpRequest request, final ChannelHandlerContext ctx) {
        final String id = extractId(request);
        try {
            final Document document = sync.read(id);
            return responseWithContent(request.getProtocolVersion(), OK, toJson(document));
        } catch (final DocumentNotFoundException e) {
            return new DefaultHttpResponse(request.getProtocolVersion(), NOT_FOUND);
        }
    }

    @Override
    public HttpResponse processPut(final HttpRequest request, final ChannelHandlerContext ctx) {
        if (request instanceof FullHttpRequest) {
            final String id = extractId(request);
            final FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            if (fullHttpRequest.content().isReadable()) {
                try {
                    final Document updateDoc = JsonMapper.partialDocument(id, fullHttpRequest.content().toString(UTF_8));
                    final Document updatedDoc = sync.update(updateDoc);
                    return responseWithContent(request.getProtocolVersion(), CREATED, toJson(updatedDoc));
                } catch (final ConflictException e) {
                    return new DefaultHttpResponse(request.getProtocolVersion(), BAD_REQUEST);
                }
            }
        }
        return new DefaultHttpResponse(request.getProtocolVersion(), BAD_REQUEST);
    }

    @Override
    public HttpResponse processPost(final HttpRequest request, final ChannelHandlerContext ctx) {
        return new DefaultHttpResponse(request.getProtocolVersion(), NOT_IMPLEMENTED);
    }

    private static String extractId(final HttpRequest request) {
        final String path = new QueryStringDecoder(request.getUri()).path();
        return path.substring(1);
    }

    private static String toJson(final Document doc) {
        return JsonMapper.toJson(doc);
    }

    @Override
    public HttpResponse processDelete(final HttpRequest request, final ChannelHandlerContext ctx) {
        return null;
    }

    private static FullHttpResponse responseWithContent(final HttpVersion version, final HttpResponseStatus status,
                                                        final String content) {
        return new DefaultFullHttpResponse(version, status, copiedBuffer(content, UTF_8));
    }
}
