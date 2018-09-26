package kcp;

import com.backblaze.erasure.ReedSolomon;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class ServerChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    static final Logger logger = LoggerFactory.getLogger(ServerChannelHandler.class);

    private Map<SocketAddress,Ukcp> ukcpMap;

    private ChannelConfig channelConfig ;

    private DisruptorExecutorPool disruptorExecutorPool;

    private KcpListener kcpListener;

    public ServerChannelHandler(Map<SocketAddress, Ukcp> ukcpMap, ChannelConfig channelConfig, DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener) {
        this.ukcpMap = ukcpMap;
        this.channelConfig = channelConfig;
        this.disruptorExecutorPool = disruptorExecutorPool;
        this.kcpListener = kcpListener;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        Ukcp ukcp = ukcpMap.get(socketAddress);
        if(ukcp==null){
            logger.error("exceptionCaught ukcp is not exist address"+ctx.channel().remoteAddress(),cause);
            return;
        }
        ukcp.getKcpListener().handleException(cause,ukcp);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        InetSocketAddress socketAddress = msg.sender();
        Ukcp ukcp = ukcpMap.get(socketAddress);
        if(ukcp==null){
            System.out.println("新连接"+Thread.currentThread().getName());
            IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
            KcpOutput kcpOutput = new KcpOutPutImp();
            Ukcp newUkcp = new Ukcp(10,kcpOutput,kcpListener,disruptorSingleExecutor);
            newUkcp.setNodelay(channelConfig.isNodelay());
            newUkcp.setInterval(channelConfig.getInterval());
            newUkcp.setFastResend(channelConfig.getFastresend());
            newUkcp.setNocwnd(channelConfig.isNocwnd());
            newUkcp.setSndWnd(channelConfig.getSndwnd());
            newUkcp.setRcvWnd(channelConfig.getRcvwnd());
            newUkcp.setMtu(channelConfig.getMtu());
            newUkcp.setMinRto(channelConfig.getMinRto());
            newUkcp.setCloseTime(channelConfig.getTimeout());
            newUkcp.setStream(channelConfig.isStream());

            newUkcp.setAckNoDelay(channelConfig.isAckNoDelay());
            newUkcp.setFastFlush(channelConfig.isFastFlush());

            newUkcp.channel(ctx.channel());

            if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
                ReedSolomon reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(),channelConfig.getFecParityShardCount());
                newUkcp.initFec(reedSolomon);
            }
            disruptorSingleExecutor.execute(() -> newUkcp.getKcpListener().onConnected(newUkcp));
            ukcpMap.put(socketAddress,newUkcp);
            newUkcp.read(msg.content());
            return;
        }
        ukcp.read(msg.content());
    }
}
