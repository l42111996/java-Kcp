package kcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by JinMiao
 * 2019-06-26.
 */
public class ClientChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Map<SocketAddress,Ukcp> ukcpMap;


    public ClientChannelHandler(Map<SocketAddress, Ukcp> ukcpMap) {
        this.ukcpMap = ukcpMap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SocketAddress socketAddress = ctx.channel().localAddress();
        Ukcp ukcp = ukcpMap.get(socketAddress);
        ukcp.getKcpListener().handleException(cause,ukcp);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        InetSocketAddress socketAddress = msg.recipient();
        Ukcp ukcp = ukcpMap.get(socketAddress);
        ukcp.read(msg.content());
    }
}
