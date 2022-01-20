package com.ewell.proxy.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wy
 * @date 2022/1/14 6:00 PM
 * @desctiption
 */
@Slf4j
@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    public static ExceptionHandler INSTANCE = new ExceptionHandler();
}
