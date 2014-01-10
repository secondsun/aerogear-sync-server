package org.jboss.aerogear.sync.rest;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jboss.aerogear.sync.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.util.CharsetUtil.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.*;

/**
 * Integration tests that requires a running CouchDB instance listening
 * to localhost:5984.
 */
public class RestServerITest {

    private static final int port = 1111;
    private static Channel channel;
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private static final DefaultEventExecutorGroup eventExecutorGroup = new DefaultEventExecutorGroup(1);

    @BeforeClass
    public static void startSimplePushServer() throws Exception {
        final ServerBootstrap sb = new ServerBootstrap();
        final CorsConfig corsConfig = CorsConfig.anyOrigin().build();
        final SyncManager syncManager = new CouchDBSyncManager("http://127.0.0.1:5984", "sync-test");
        sb.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        sb.childHandler(new HttpServerInitializer(corsConfig, syncManager));
        channel = sb.bind(port).sync().channel();
    }

    @AfterClass
    public static void stopSimplePushServer() throws InterruptedException {
        final ChannelFuture disconnect = channel.disconnect();
        disconnect.await(1000);
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        eventExecutorGroup.shutdownGracefully();
    }

    @Test
    public void createDocument() throws Exception {
        final String documentId = UUID.randomUUID().toString();
        final String content = "{\"model\":\"honda\"}";
        final FullHttpRequest request = httpRequest(PUT, documentId, content);

        final ClientHandler clientHandler = new ClientHandler();
        final EventLoopGroup group = new NioEventLoopGroup();
        try {
            final Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class) .handler(new ClientInitializer(clientHandler));
            final Channel ch = b.connect("localhost", port).sync().channel();
            ch.writeAndFlush(request);
            ch.closeFuture().sync();

            assertThat(clientHandler.getResponse().getStatus(), is(OK));
            assertThat(clientHandler.getResponse().headers().get(CONTENT_TYPE), equalTo("application/json"));
            final Document document = JsonMapper.fromJson(clientHandler.body(), Document.class);
            assertThat(document.id(), equalTo(documentId));
            assertThat(document.revision(), is(notNullValue()));
            assertThat(document.content(), equalTo(content));
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class ClientInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler handler;

        private ClientInitializer(final ChannelHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            final ChannelPipeline p = ch.pipeline();
            p.addLast("http-codec", new HttpClientCodec());
            p.addLast("handler", handler);
        }
    }

    private static FullHttpRequest httpRequest(final HttpMethod method, final String documentId, final String content) {
        final String payload = "{\"content\":" + content + '}';
        final ByteBuf byteBuf = Unpooled.copiedBuffer(payload, UTF_8);
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, method, '/' + documentId, byteBuf);
        request.headers().set(HOST, "localhost");
        request.headers().set(CONTENT_LENGTH, byteBuf.readableBytes());
        request.headers().set(CONTENT_TYPE, "application/json");
        return request;
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {

        private HttpResponse response;
        private String content;

        @Override
        protected void messageReceived(final ChannelHandlerContext ctx, final HttpObject msg) throws Exception {
            if (msg instanceof HttpResponse) {
                response = (HttpResponse) msg;
            }
            if (msg instanceof HttpContent) {
                if (!(msg instanceof LastHttpContent)) {
                    content = ((HttpContent) msg).content().toString(UTF_8);
                    ctx.close();
                }
            }
        }

        public HttpResponse getResponse() {
            return response;
        }

        public String body() {
            return content;
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            cause.printStackTrace();
        }
    }
}
