package kcp;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2018/11/2.
 */
public class User {

    public User(ChannelHandlerContext channelHandlerContext, InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
        this.channelHandlerContext = channelHandlerContext;
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
    }

    private ChannelHandlerContext channelHandlerContext;

    private InetSocketAddress remoteAddress;
    private InetSocketAddress localAddress;

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }
}
