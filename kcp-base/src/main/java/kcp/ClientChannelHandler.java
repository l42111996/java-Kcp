package kcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by JinMiao
 * 2019-06-26.
 */
public class ClientChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private IChannelManager channelManager;

    public ClientChannelHandler(IChannelManager channelManager) {
        this.channelManager = channelManager;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("",cause);
        //SocketAddress socketAddress = ctx.channel().localAddress();
        //Ukcp ukcp = ukcpMap.get(socketAddress);
        //ukcp.getKcpListener().handleException(cause,ukcp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        Channel channel  = ctx.channel();
        Ukcp ukcp = this.channelManager.get(channel,msg);
        ukcp.read(msg.content());
    }
}
