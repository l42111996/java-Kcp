package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import test.tcp.NetConnector;
import test.tcp.TcpChannelInitializer;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2020/7/17.
 */
@ChannelHandler.Sharable
public class TcpLockStepSynchronizationClient extends SimpleChannelInboundHandler<ByteBuf> {

    public static void main(String[] args) {
        TcpLockStepSynchronizationClient tcpLockStepSynchronizationClient = new TcpLockStepSynchronizationClient();
        String ip = "127.0.0.1";
        if(args.length>0){
            ip = args[0];
        }
        int number= 100;
        if(args.length>1){
            number = Integer.parseInt(args[1]);
        }
        NetConnector netConnector = new NetConnector(new TcpChannelInitializer(tcpLockStepSynchronizationClient));
        try {
            for (int i = 0; i < number; i++) {
                netConnector.connect(new InetSocketAddress(ip,11009));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //模拟按键事件
        TimerThreadPool.scheduleWithFixedDelay(() -> {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(20);
            byteBuf.writeBytes(new byte[20]);
            ctx.channel().writeAndFlush(byteBuf);
            //byteBuf.release();
        },50);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接断开了"+ctx.channel().remoteAddress());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {

    }
}
