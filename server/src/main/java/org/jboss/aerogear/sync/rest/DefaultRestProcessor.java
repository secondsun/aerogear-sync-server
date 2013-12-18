package org.jboss.aerogear.sync.rest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.SyncManager;

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
        return null;
    }

    @Override
    public HttpResponse processPut(final HttpRequest request, final ChannelHandlerContext ctx) {
        return null;
    }

    @Override
    public HttpResponse processPost(final HttpRequest request, final ChannelHandlerContext ctx) {
        if (request instanceof FullHttpRequest) {
            final FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            if (fullHttpRequest.content().isReadable()) {
                final Document document = sync.create(fullHttpRequest.content().toString(UTF_8));
                return responseWithContent(request.getProtocolVersion(), CREATED, document.content());
            }
        }
        return new DefaultHttpResponse(request.getProtocolVersion(), BAD_REQUEST);
    }

    @Override
    public HttpResponse processDelete(final HttpRequest request, final ChannelHandlerContext ctx) {
        return null;
    }

    private static FullHttpResponse responseWithContent(final HttpVersion version, final HttpResponseStatus status,
                                                        final String content) {
        return new DefaultFullHttpResponse(version, CREATED, copiedBuffer(content, UTF_8));
    }
}
