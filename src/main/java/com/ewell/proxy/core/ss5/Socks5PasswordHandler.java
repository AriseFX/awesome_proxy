package com.ewell.proxy.core.ss5;

import com.ewell.proxy.Main;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import lombok.extern.slf4j.Slf4j;


/**
 * @author wy
 * @date 2021/12/15 2:35 PM
 * @desctiption ss5账号密码验证
 */
@Slf4j
public class Socks5PasswordHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) {
//        log.info("收到的账号密码是 {}   {}", msg.username(), msg.password());
        if (Main.auth[0].equals(msg.username()) && Main.auth[1].equals(msg.password())) {
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.channel().writeAndFlush(response);
        } else {
            Socks5PasswordAuthResponse response = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.channel().writeAndFlush(response).addListener(e -> ctx.channel().close());
        }
    }
}
