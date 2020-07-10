package kcp;

import com.backblaze.erasure.ReedSolomon;
import com.backblaze.erasure.fec.Fec;
import io.netty.buffer.ByteBuf;
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

    private ChannelConfig channelConfig;

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
        logger.error("", cause);
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
        final ChannelConfig channelConfig = this.channelConfig;
        DatagramPacket msg = (DatagramPacket) object;
        Ukcp ukcp = channelManager.get(msg);
        ByteBuf byteBuf = msg.content();

        if (ukcp != null) {
            User user = ukcp.user();
            //每次收到消息重绑定地址
            user.setRemoteAddress(msg.sender());
            ukcp.read(byteBuf);
            return;
        }

        //如果是新连接第一个包的sn必须为0
        int sn = getSn(byteBuf,channelConfig);
        if(sn!=0){
            msg.release();
            return;
        }
        IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
        KcpOutput kcpOutput = new KcpOutPutImp();
        ReedSolomon reedSolomon = null;
        if (channelConfig.getFecDataShardCount() != 0 && channelConfig.getFecParityShardCount() != 0) {
            reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(), channelConfig.getFecParityShardCount());
        }
        Ukcp newUkcp = new Ukcp(kcpOutput, kcpListener, disruptorSingleExecutor, reedSolomon, channelConfig, channelManager);

        User user = new User(ctx.channel(), msg.sender(), msg.recipient());
        newUkcp.user(user);
        channelManager.New(msg.sender(), newUkcp, msg);

        disruptorSingleExecutor.execute(() -> {
            try {
                newUkcp.getKcpListener().onConnected(newUkcp);
            } catch (Throwable throwable) {
                newUkcp.getKcpListener().handleException(throwable, newUkcp);
            }
        });

        newUkcp.read(byteBuf);


        ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor, newUkcp);
        DisruptorExecutorPool.scheduleHashedWheel(scheduleTask, newUkcp.getInterval());
    }


    private int getSn(ByteBuf byteBuf,ChannelConfig channelConfig){
        int headerSize = 0;
        if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
            headerSize+= Fec.fecHeaderSizePlus2;
        }

        int sn = byteBuf.getIntLE(byteBuf.readerIndex()+Kcp.IKCP_SN_OFFSET+headerSize);
        return sn;
    }

}
