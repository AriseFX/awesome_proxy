package com.ewell.proxy;

import com.ewell.proxy.common.NettyBootstrapFactory;
import com.ewell.proxy.core.ProtocolDiscernHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wy
 * @date 2021/12/17 10:28 AM
 * @desctiption
 */
@Slf4j
public class Main {

    public static String[] auth;

    public static void main(String[] args) throws InterruptedException {
        String port = System.getProperty("proxy.port");
        String nouser = System.getProperty("proxy.nouser");
        String username = System.getProperty("proxy.username");
        String password = System.getProperty("proxy.password");
        if (username != null && password != null) {
            auth = new String[]{username, password};
        } else {
            auth = new String[]{"admin", "123456"};
        }
        if (nouser != null) {
            auth = null;
        }
        if (port == null) {
            port = "8080";
        }
        runServer("0.0.0.0", Integer.parseInt(port));
    }

    public static void runServer(String host, int port) throws InterruptedException {
        ServerBootstrap b = NettyBootstrapFactory.newServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new ProtocolDiscernHandler());
            }
        });
        Channel channel = b.bind(host, port).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Server startup complete！[{}:{}]", host, port);
                log.info("useDirectBufferNoCleaner:{}", PlatformDependent.useDirectBufferNoCleaner());
            }
        }).sync().channel();
        channel.closeFuture().sync().addListener(e -> log.info("Server Stopped！"));
    }
}
