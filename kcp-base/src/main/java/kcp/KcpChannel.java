package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.ChannelConfig;
import io.netty.util.internal.shaded.org.jctools.queues.MpscChunkedArrayQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;

/**
 * Created by JinMiao
 * 2020/7/28.
 */
public class KcpChannel extends AbstractChannel {
    protected final ChannelMetadata metadata = new ChannelMetadata(false);
    protected final DefaultChannelConfig config = new DefaultChannelConfig(this);


    protected final KcpServerChannel kcpServerChannel;
    protected final InetSocketAddress remoteAddress;
    protected SocketAddress localAddress;
    protected volatile boolean open = true;

    protected Ukcp ukcp;

    protected Queue<ByteBuf> readQueue = new MpscChunkedArrayQueue<>(2<<11);


    public KcpChannel(KcpServerChannel kcpServerChannel, InetSocketAddress remote) {
        super(kcpServerChannel);
        this.kcpServerChannel = kcpServerChannel;
        this.remoteAddress = remote;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {

            @Override
            public void connect(SocketAddress addr1, SocketAddress addr2, ChannelPromise pr) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return false;
    }

    @Override
    protected SocketAddress localAddress0() {
        return this.localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return this.remoteAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() {
        this.kcpServerChannel.removeChannel(ukcp);
    }


    @Override
    protected void doBeginRead() throws Exception {

    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        //从发送缓冲区到kcp缓冲区
        ByteBuf byteBuf =  null;
        while(ukcp.canSend(false)){
            Object current = in.current();
            if(current==null){
                break;
            }
            byteBuf = (ByteBuf) current;
            try {
                ukcp.send(byteBuf);
                in.remove();
            } catch (IOException e) {
                ukcp.getKcpListener().handleException(e, ukcp);
                return;
            }
        }

        //如果有发送 则检测时间
        if(!ukcp.canSend(false)||(ukcp.checkFlush()&& ukcp.isFastFlush())){
            long now =System.currentTimeMillis();
            long next = ukcp.flush(now);
            ukcp.setTsUpdate(now+next);
        }
    }


    public Queue<ByteBuf> getReadQueue() {
        return readQueue;
    }

    @Override
    public ChannelConfig config() {
        return this.config;
    }

    @Override
    public boolean isOpen() {
        return isActive();
    }

    @Override
    public boolean isActive() {
        return open;
    }

    @Override
    public ChannelMetadata metadata() {
        return metadata;
    }
}
