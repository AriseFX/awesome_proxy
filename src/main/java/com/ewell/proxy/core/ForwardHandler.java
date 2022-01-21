package com.ewell.proxy.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wy
 * @date 2021/12/15 1:38 PM
 * @desctiption
 */
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {

    private final Channel channel;

    public ForwardHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //处理背压
        channel.config().setAutoRead(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (channel.isActive()) {
            channel.writeAndFlush(msg);
        }else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (channel.isActive()) {
            channel.flush();
            channel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (channel.isActive()) {
            channel.flush();
            channel.close();
        }
    }
}
