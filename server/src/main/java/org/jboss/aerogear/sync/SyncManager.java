package org.jboss.aerogear.sync;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SyncManager extends SimpleChannelInboundHandler<String>{

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final String msg) throws Exception {
        ctx.writeAndFlush(msg);
    }
}
