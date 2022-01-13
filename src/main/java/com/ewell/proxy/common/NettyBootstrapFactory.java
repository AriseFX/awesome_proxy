package com.ewell.proxy.common;

import com.ewell.proxy.common.os.OSHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import net.openhft.chronicle.core.OS;

import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.channel.epoll.EpollChannelOption.EPOLL_MODE;

/**
 * @Author: wy
 * @Date: Created in 1:54 下午 2021/9/17
 * @Description:
 * @Modified: By：
 */
public class NettyBootstrapFactory {

    public static ServerBootstrap newServerBootstrap() {
        EventLoopGroup boss = OSHelper.eventLoopGroup(1, "gateway-boss");
        EventLoopGroup worker = OSHelper.eventLoopGroup(8, "gateway-worker");
        ServerBootstrap b = new ServerBootstrap();
        b.childOption(TCP_NODELAY, true);
        b.option(SO_BACKLOG, 16384);
        if (OS.isLinux()) {
            b.childOption(EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }
        b.group(boss, worker);
        b.channel(OSHelper.acceptChannelType());
        return b;
    }

    public static Bootstrap newBootstrap() {
        Bootstrap b = new Bootstrap();
        b.channel(OSHelper.channelType());
        b.option(TCP_NODELAY, true);
        if (OS.isLinux() ) {
            b.option(EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }
        return b;
    }
}
