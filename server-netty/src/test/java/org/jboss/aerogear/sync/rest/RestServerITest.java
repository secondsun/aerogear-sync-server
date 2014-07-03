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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jboss.aerogear.sync.datastore.CouchDBSyncDataStore;
import org.jboss.aerogear.sync.DefaultDocument;
import org.jboss.aerogear.sync.Document;
import org.jboss.aerogear.sync.JsonMapper;
import org.jboss.aerogear.sync.datastore.SyncDataStore;
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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.*;
import static org.jboss.aerogear.sync.JsonMapper.*;

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
        final CorsConfig corsConfig = CorsConfig.withAnyOrigin().build();
        final SyncDataStore syncDataStore = new CouchDBSyncDataStore("http://127.0.0.1:5984", "sync-test");
        sb.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        sb.childHandler(new TestHttpServerInitializer(corsConfig, syncDataStore));
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
        final NettyClient client = new NettyClient("localhost", port);
        try {
            client.send(putRequest(documentId, content));
            assertThat(client.getResponse().status(), is(OK));
            assertThat(client.getResponse().headers().get(CONTENT_TYPE), equalTo("application/json"));
            final Document document = fromJson(client.getBody(), Document.class);
            assertThat(document.id(), equalTo(documentId));
            assertThat(document.revision(), is(notNullValue()));
            assertThat(document.content(), equalTo(content));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void getDocument() throws Exception {
        final String documentId = UUID.randomUUID().toString();
        final String content = "{\"model\":\"honda\"}";
        final NettyClient client = new NettyClient("localhost", port);
        try {
            client.send(putRequest(documentId, content)).send(getRequest(documentId));
            assertThat(client.getResponse().headers().get(CONTENT_TYPE), equalTo("application/json"));
            final Document document = fromJson(client.getBody(), Document.class);
            assertThat(document.id(), equalTo(documentId));
            assertThat(document.revision(), is(notNullValue()));
            assertThat(document.content(), equalTo(content));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void getDocumentWithContextPath() throws Exception {
        final String contextPath = "/buddies/";
        final String documentId = UUID.randomUUID().toString();
        final String content = "{\"model\":\"honda\"}";
        final NettyClient client = new NettyClient("localhost", port);
        try {
            client.send(putRequest(contextPath, documentId, content)).send(getRequest(contextPath, documentId, ""));
            assertThat(client.getResponse().headers().get(CONTENT_TYPE), equalTo("application/json"));
            final Document document = fromJson(client.getBody(), Document.class);
            assertThat(document.id(), equalTo(documentId));
            assertThat(document.revision(), is(notNullValue()));
            assertThat(document.content(), equalTo(content));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void updateDocumentWithConflict() throws Exception {
        final String documentId = UUID.randomUUID().toString();
        final String content = "{\"model\":\"honda\"}";
        final NettyClient client = new NettyClient("localhost", port);
        try {
            client.send(putRequest(documentId, content)).send(getRequest(documentId));
            final Document original = fromJson(client.getBody(), Document.class);
            final Document update = new DefaultDocument(original.id(), original.revision(), "{\"model\":\"bmw\"}");
            client.send(putRequest(update));
            final Document updated = fromJson(client.getBody(), Document.class);
            final Document conflict = new DefaultDocument(updated.id(), original.revision(), "{\"model\":\"mazda\"}");
            client.send(putRequest(conflict));
            assertThat(client.getResponse().status(), is(CONFLICT));
            assertThat(fromJson(client.getBody(), Document.class).revision(), equalTo(updated.revision()));
        } finally {
            client.shutdown();
        }
    }

    @Test
    public void deleteDocument() throws Exception {
        final String documentId = UUID.randomUUID().toString();
        final String content = "{\"model\":\"honda\"}";
        final NettyClient client = new NettyClient("localhost", port);
        try {
            client.send(putRequest(documentId, content));
            final Document doc = fromJson(client.getBody(), Document.class);
            client.send(deleteRequest(documentId, "/", jsonRev(doc.revision())));
            assertThat(client.getResponse().status(), equalTo(OK));
            assertThat(client.getResponse().headers().get(CONTENT_TYPE), equalTo("application/json"));
            assertThat(asJsonNode(client.getBody()).get("rev").asText(), not(equalTo(doc.revision())));
        } finally {
            client.shutdown();
        }
    }

    private static String jsonRev(final String revision) {
        return JsonMapper.newObjectNode().put("rev", revision).toString();
    }

    private static class NettyClient {

        private final String host;
        private final int port;
        private final ClientHandler clientHandler;
        private final NioEventLoopGroup group;
        private final Bootstrap bootstrap;
        private Channel ch;

        private NettyClient(final String host, final int port) {
            this.host = host;
            this.port = port;
            clientHandler = new ClientHandler();
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new ClientInitializer(clientHandler));
        }

        public NettyClient send(final FullHttpRequest request) throws InterruptedException {
            ch = bootstrap.connect(host, port).sync().channel();
            ch.writeAndFlush(request).sync();
            ch.closeFuture().sync();
            return this;
        }

        public HttpResponse getResponse() {
            return clientHandler.getResponse();
        }

        public String getBody() {
            return clientHandler.body();
        }

        public void shutdown() {
            group.shutdownGracefully();
        }

    }

    private static class ClientInitializer extends ChannelInitializer<SocketChannel> {

        private final ChannelHandler handler;

        private ClientInitializer(final ChannelHandler handler) {
            this.handler = handler;
        }

        @Override
        protected void initChannel(final SocketChannel ch) throws Exception {
            final ChannelPipeline p = ch.pipeline();
            p.addLast("http-codec", new HttpClientCodec());
            p.addLast("handler", handler);
        }
    }

    private static FullHttpRequest getRequest(final String documentId) {
        return getRequest("/", documentId, "");
    }

    private static FullHttpRequest getRequest(final String contextPath,
                                              final String documentId,
                                              final String revision) {
        return httpRequest(contextPath + documentId, GET, revision(revision));
    }

    private static FullHttpRequest putRequest(final String documentId, final String payload) {
        return putRequest("/", documentId, payload);
    }

    private static FullHttpRequest putRequest(final Document doc) {
        return httpRequest('/' + doc.id(), PUT, contentWithRev(doc.revision(), doc.content()));
    }

    private static FullHttpRequest putRequest(final String contextPath,
                                               final String documentId,
                                               final String payload) {
        return httpRequest(contextPath + documentId, PUT, content(payload));
    }

    private static ByteBuf content(final String content) {
        final String payload = "{\"content\":" + content + '}';
        return Unpooled.copiedBuffer(payload, UTF_8);
    }

    private static ByteBuf revision(final String rev) {
        return Unpooled.copiedBuffer(jsonRev(rev), UTF_8);
    }

    private static ByteBuf contentWithRev(final String rev, final String content) {
        final String payload = "{\"rev\":\"" + rev + "\", \"content\":" + content + '}';
        return Unpooled.copiedBuffer(payload, UTF_8);
    }

    private static FullHttpRequest deleteRequest(final String documentId, final String contextPath,
                                                 final String content) {
        return httpRequest(contextPath + documentId, DELETE, Unpooled.copiedBuffer(content, UTF_8));
    }

    private static FullHttpRequest httpRequest(final String path, final HttpMethod method, final ByteBuf payload) {
        final String url = "http://localhost:" + port + path;
        final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, method, url, payload);
        addHeaders(request, payload.readableBytes());
        return request;
    }

    private static void addHeaders(final HttpRequest request, final int contentSize) {
        request.headers().set(HOST, "localhost");
        request.headers().set(CONTENT_LENGTH, contentSize);
        request.headers().set(CONTENT_TYPE, "application/json");
    }

    @ChannelHandler.Sharable
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
