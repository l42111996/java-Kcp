package kcp;

import com.backblaze.erasure.ReedSolomon;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ServerChannelHandler.class);

    private Map<SocketAddress,Ukcp> clientMap;

    private ChannelConfig channelConfig ;

    private DisruptorExecutorPool disruptorExecutorPool;

    private KcpListener kcpListener;

    public ServerChannelHandler(Map<SocketAddress, Ukcp> clientMap, ChannelConfig channelConfig, DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener) {
        this.clientMap = clientMap;
        this.channelConfig = channelConfig;
        this.disruptorExecutorPool = disruptorExecutorPool;
        this.kcpListener = kcpListener;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        Ukcp ukcp = clientMap.get(socketAddress);
        if(ukcp==null){
            logger.error("exceptionCaught ukcp is not exist address"+ctx.channel().remoteAddress(),cause);
            return;
        }
        ukcp.getKcpListener().handleException(cause,ukcp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        {
            InetSocketAddress socketAddress = msg.sender();
            Ukcp ukcp = clientMap.get(socketAddress);
            if(ukcp==null){
                User user = new User(ctx.channel(),msg.sender(),msg.recipient());
                //System.out.println("新连接"+Thread.currentThread().getName());
                IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
                KcpOutput kcpOutput = new KcpOutPutImp();

                ReedSolomon reedSolomon = null;
                if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
                    reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(),channelConfig.getFecParityShardCount());
                }

                Ukcp newUkcp = new Ukcp(10,kcpOutput,kcpListener,disruptorSingleExecutor,channelConfig.isCrc32Check(),reedSolomon);

                newUkcp.setNodelay(channelConfig.isNodelay());
                newUkcp.setInterval(channelConfig.getInterval());
                newUkcp.setFastResend(channelConfig.getFastresend());
                newUkcp.setNocwnd(channelConfig.isNocwnd());
                newUkcp.setSndWnd(channelConfig.getSndwnd());
                newUkcp.setRcvWnd(channelConfig.getRcvwnd());
                newUkcp.setMtu(channelConfig.getMtu());
                newUkcp.setMinRto(channelConfig.getMinRto());
                newUkcp.setTimeoutMillis(channelConfig.getTimeoutMillis());
                newUkcp.setStream(channelConfig.isStream());
                newUkcp.setAckNoDelay(channelConfig.isAckNoDelay());
                newUkcp.setFastFlush(channelConfig.isFastFlush());
                newUkcp.user(user);

                disruptorSingleExecutor.execute(() -> newUkcp.getKcpListener().onConnected(newUkcp));
                clientMap.put(socketAddress,newUkcp);
                newUkcp.read(msg.content());

                ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor,newUkcp, clientMap);
                DisruptorExecutorPool.schedule(scheduleTask, newUkcp.getInterval());
                return;
            }
            ukcp.read(msg.content());
        }

    }

}
