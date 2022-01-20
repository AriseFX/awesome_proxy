package com.ewell.proxy.core.http;

import com.ewell.proxy.Main;
import com.ewell.proxy.common.NettyBootstrapFactory;
import com.ewell.proxy.common.os.OSHelper;
import com.ewell.proxy.common.os.PassThroughStrategy;
import com.ewell.proxy.core.Blacklist;
import com.ewell.proxy.core.ExceptionHandler;
import com.ewell.proxy.core.ForwardHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;

/**
 * @author wy
 * @date 2021/12/14 10:44 PM
 * @desctiption
 */
@Slf4j
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Bootstrap b = NettyBootstrapFactory.newBootstrap();

    private static final PassThroughStrategy passThrough = OSHelper.nativePassThrough();

    private HttpRequest request;
    private String host;
    private int port;
    private final ArrayList<HttpContent> contents = new ArrayList<>();

    public static String auth = null;

    static {
        if (Main.auth != null) {
            auth = "Basic " + Base64.getEncoder().encodeToString((Main.auth[0] + ":" + Main.auth[1]).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            if (!request.decoderResult().isSuccess()) {
                ctx.channel().close();
                return;
            }
            String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
            if (hostAndPortStr != null) {
                String[] hostPortArray = hostAndPortStr.split(":");
                host = hostPortArray[0];
                String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
                port = Integer.parseInt(portStr);
            }
        } else {
            ((HttpContent) msg).content().retain();
            contents.add((HttpContent) msg);
            if (msg instanceof LastHttpContent) {
                if (request.uri().charAt(0) == '/' || host == null) {
                    //非代理请求，必须要release
                    ctx.channel().close();
                    contents.forEach(ReferenceCounted::release);
                    return;
                }
                //校验密码
                if (auth != null) {
                    String basicAuth = request.headers().get("Proxy-Authorization");
                    if (!auth.equals(basicAuth)) {
                        contents.forEach(ReferenceCounted::release);
                        String hostAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
                        Blacklist.addIp(hostAddress);
                        log.info("认证异常,对方ip:{}", hostAddress);
                        DefaultHttpResponse resp = new DefaultHttpResponse(request.protocolVersion(), PROXY_AUTHENTICATION_REQUIRED);
                        resp.headers().add("Proxy-Authenticate", "Basic realm=\"awesomeproxy\"");
                        Channel inbound = ctx.channel();
                        inbound.pipeline().addLast(new HttpResponseEncoder());
                        inbound.writeAndFlush(resp).addListener(e -> {
                            inbound.close();
                        });
                        return;
                    }
                }
                handleProxy(ctx);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("HttpProxyHandler发生异常:", cause);
        contents.forEach(ReferenceCounted::release);
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            channel.flush();
            channel.close();
        }
    }

    protected void handleProxy(ChannelHandlerContext ctx) {
        Channel inbound = ctx.channel();
        Promise<Channel> promise = ctx.executor().newPromise();
        //处理https代理
        if (request.method().equals(HttpMethod.CONNECT)) {
            promise.addListener((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    //连接远程服务器成功
                    Channel outbound = future.getNow();
                    inbound.pipeline().addLast(new HttpResponseEncoder());
                    //重要
                    outbound.config().setAutoRead(false);
                    inbound.config().setAutoRead(false);
                    inbound.writeAndFlush(new DefaultHttpResponse(request.protocolVersion(), new HttpResponseStatus(200, "Connection Established"))).addListener(res -> {
                        if (res.isSuccess()) {
                            //去掉所有handler(后续走tunnel)
                            ChannelPipeline pipeline = ctx.pipeline();
                            while (pipeline.last() != null) {
                                pipeline.removeLast();
                            }
                            //流量透传
                            passThrough.accept(inbound, outbound);
                        }
                    });
                } else {
                    contents.forEach(ReferenceCounted::release);
                    if (inbound.isActive()) {
                        inbound.flush();
                        inbound.close();
                    }
                }
            });
        } else {
            promise.addListener((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.getNow();
                    ctx.pipeline().remove(this);
                    ctx.pipeline().addLast(new ForwardHandler(outbound));
                    //转发请求
                    outbound.pipeline().addLast(new HttpRequestEncoder());
                    outbound.pipeline().addLast(new ForwardHandler(inbound));
                    modRequest(request);
                    outbound.writeAndFlush(request);
                    contents.forEach(outbound::writeAndFlush);
                } else {
                    contents.forEach(ReferenceCounted::release);
                    if (inbound.isActive()) {
                        inbound.flush();
                        inbound.close();
                    }
                }
            });
        }
        if (b.config().group() == null) {
            b.group(inbound.eventLoop())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(ExceptionHandler.INSTANCE)
                    .connect(host, port).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            Channel channel = future.channel();
                            promise.trySuccess(channel);
                        } else {
                            promise.tryFailure(future.cause());
                        }
                    });
        }
    }

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
