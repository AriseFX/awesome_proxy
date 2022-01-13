package com.ewell.proxy.core.ss5;

import com.ewell.proxy.common.NettyBootstrapFactory;
import com.ewell.proxy.common.os.OSHelper;
import com.ewell.proxy.common.os.PassThroughStrategy;
import com.ewell.proxy.core.RemoteChannelActiveHandler;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wy
 * @date 2021/12/16 3:34 PM
 * @desctiption 处理socks5命令
 */
@Slf4j
public class Socks5CommandHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private static final PassThroughStrategy passThrough = OSHelper.nativePassThrough();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        Channel inboundChannel = ctx.channel();
        if (msg.type().equals(Socks5CommandType.CONNECT)) {
            //普通tcp模式
            Promise<Channel> promise = inboundChannel.eventLoop().newPromise();
            promise.addListener(((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    //重要
                    inboundChannel.config().setAutoRead(false);
                    Channel remoteChannel = future.get();
                    Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                    inboundChannel.writeAndFlush(commandResponse);
                    //去掉所有handler(后续走tunnel)
                    ChannelPipeline pipeline = ctx.pipeline();
                    while (pipeline.last() != null) {
                        pipeline.removeLast();
                    }
                    //流量透传
                    passThrough.accept(inboundChannel,remoteChannel);
                    inboundChannel.config().setAutoRead(true);
                }
            }));
            NettyBootstrapFactory.newBootstrap()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new RemoteChannelActiveHandler(promise))
                    .group(ctx.channel().eventLoop())
                    .connect(msg.dstAddr(), msg.dstPort());
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
