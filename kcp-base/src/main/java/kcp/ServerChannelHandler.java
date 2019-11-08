package kcp;

import com.backblaze.erasure.ReedSolomon;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ServerChannelHandler.class);

    private IChannelManager channelManager;

    private ChannelConfig channelConfig ;

    private DisruptorExecutorPool disruptorExecutorPool;

    private KcpListener kcpListener;

    public ServerChannelHandler(IChannelManager channelManager, ChannelConfig channelConfig, DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener) {
        this.channelManager = channelManager;
        this.channelConfig = channelConfig;
        this.disruptorExecutorPool = disruptorExecutorPool;
        this.kcpListener = kcpListener;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("",cause);
        //SocketAddress socketAddress = ctx.channel().remoteAddress();
        //Ukcp ukcp = clientMap.get(socketAddress);
        //if(ukcp==null){
        //    logger.error("exceptionCaught ukcp is not exist address"+ctx.channel().remoteAddress(),cause);
        //    return;
        //}
        //ukcp.getKcpListener().handleException(cause,ukcp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        DatagramPacket msg = (DatagramPacket) object;
        Channel channel = ctx.channel();
        Ukcp ukcp = channelManager.get(msg);

        if (ukcp != null)
        {
            User user = ukcp.user();
            //每次收到消息重绑定地址
            user.setRemoteAddress(msg.sender());
            ukcp.read(msg.content());
            return;
        }

        //System.out.println("新连接"+Thread.currentThread().getName());
        IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
        KcpOutput kcpOutput = new KcpOutPutImp();

        ReedSolomon reedSolomon = null;
        if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
            reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(),channelConfig.getFecParityShardCount());
        }

        Ukcp newUkcp = new Ukcp(kcpOutput,kcpListener,disruptorSingleExecutor,reedSolomon,channelConfig);

        User user = new User(ctx.channel(),msg.sender(),msg.recipient());
        newUkcp.user(user);

        disruptorSingleExecutor.execute(() ->{
            try {
                newUkcp.getKcpListener().onConnected(newUkcp);
            }catch (Throwable throwable){
                newUkcp.getKcpListener().handleException(throwable,newUkcp);
            }
        });
        channelManager.New(msg.sender(),newUkcp);
        newUkcp.read(msg.content());

        ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor,newUkcp, channelManager);
        DisruptorExecutorPool.scheduleHashedWheel(scheduleTask, newUkcp.getInterval());
    }

}
