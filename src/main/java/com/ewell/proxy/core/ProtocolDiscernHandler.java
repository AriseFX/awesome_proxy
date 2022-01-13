package com.ewell.proxy.core;

import com.ewell.proxy.Main;
import com.ewell.proxy.core.http.HttpProxyHandler;
import com.ewell.proxy.core.ss5.Socks5CommandHandler;
import com.ewell.proxy.core.ss5.Socks5HandshakeHandler;
import com.ewell.proxy.core.ss5.Socks5PasswordHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;


/**
 * @author wy
 * @date 2021/12/15 1:32 PM
 * @desctiption 区分ss5和http协议
 */
public class ProtocolDiscernHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public ProtocolDiscernHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte head = msg.getByte(0);
        ChannelPipeline p = ctx.pipeline();
        p.remove(this);
        if (head == 5) {
            p.addLast(Socks5ServerEncoder.DEFAULT);
            p.addLast(new Socks5InitialRequestDecoder());
            p.addLast(new Socks5HandshakeHandler());
            if (Main.auth != null) {
                p.addLast(new Socks5PasswordAuthRequestDecoder());
                //auth
                p.addLast(new Socks5PasswordHandler());
            }
            p.addLast(new Socks5CommandRequestDecoder());
            //command
            p.addLast(new Socks5CommandHandler());
        } else {
            //http/https
            p.addLast(new HttpRequestDecoder());
            p.addLast(new HttpProxyHandler());
        }
        p.fireChannelRead(msg);
    }
}
