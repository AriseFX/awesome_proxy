package com.ewell.proxy.core.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author wy
 * @date 2021/12/15 1:38 PM
 * @desctiption
 */
@Slf4j
public class ModHttpRequestHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            modRequest((HttpRequest) msg);
        }
        ctx.fireChannelRead(msg);
    }

    /**
     * 修改报文内容
     */
    private void modRequest(HttpRequest request) {
        request.headers().remove("Proxy-Authorization");
        String proxyConnection = request.headers().get("Proxy-Connection");
        if (Objects.nonNull(proxyConnection)) {
            request.headers().set("Connection", proxyConnection);
            request.headers().remove("Proxy-Connection");
        }
        //获取Host和port
        String hostAndPortStr = request.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);

        try {
            String url = request.uri();
            int index = url.indexOf(host) + host.length();
            url = url.substring(index);
            if (url.startsWith(":")) {
                url = url.substring(1 + String.valueOf(port).length());
            }
            request.setUri(url);
        } catch (Exception e) {
            log.error("无法获取url:{} {}", request.uri(), host);
        }
    }
}
