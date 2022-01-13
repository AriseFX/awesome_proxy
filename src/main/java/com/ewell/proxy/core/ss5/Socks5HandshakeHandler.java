package com.ewell.proxy.core.ss5;

import com.ewell.proxy.Main;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wy
 * @date 2021/12/14 11:24 PM
 * @desctiption 处理socks5握手
 */
@Slf4j
public class Socks5HandshakeHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) {
        if (msg.decoderResult().isFailure()) {
            ctx.fireChannelRead(msg);
        } else {
            if (msg.version().equals(SocksVersion.SOCKS5)) {
                Socks5InitialResponse initialResponse;
                if (Main.auth != null) {
                    initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
                } else {
                    initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                }
                ctx.writeAndFlush(initialResponse);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
