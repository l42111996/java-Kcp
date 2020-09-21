package kcp;

import io.netty.channel.AbstractServerChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.EventLoop;
import kcp.IChannelManager;
import kcp.Ukcp;

import java.net.SocketAddress;

/**
 * Created by JinMiao
 * 2020/7/28.
 */
public class KcpServerChannel extends AbstractServerChannel {

    private IChannelManager iChannelManager;

    protected void removeChannel(Ukcp ukcp){
        iChannelManager.del(ukcp);
    }


    @Override
    protected boolean isCompatible(EventLoop loop) {
        return false;
    }

    @Override
    protected SocketAddress localAddress0() {
        return null;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {

    }

    @Override
    protected void doClose() throws Exception {

    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {

    }


    @Override
    protected void doBeginRead() throws Exception {

    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
