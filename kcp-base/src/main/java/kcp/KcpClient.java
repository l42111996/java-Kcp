package kcp;

import com.backblaze.erasure.ReedSolomon;
import com.backblaze.erasure.fec.Fec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.net.InetSocketAddress;

/**
 * kcp客户端
 * Created by JinMiao
 * 2019-06-26.
 */
public class KcpClient {


    private DisruptorExecutorPool disruptorExecutorPool;
    private Bootstrap bootstrap;
    private EventLoopGroup nioEventLoopGroup;
    /**客户端的连接集合**/
    private IChannelManager channelManager;


    public void init(ChannelConfig channelConfig) {
        if(channelConfig.isUseConvChannel()){
            int convIndex = 0;
            if(channelConfig.KcpTag){
                convIndex+=Ukcp.KCP_TAG;
            }
            if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
                convIndex+= Fec.fecHeaderSizePlus2;
            }
            channelManager = new ConvChannelManager(convIndex);
        }else{
            channelManager = new ClientAddressChannelManager();
        }
        int cpuNum = Runtime.getRuntime().availableProcessors();
        if (disruptorExecutorPool == null) {
            this.disruptorExecutorPool = new DisruptorExecutorPool();
            for (int i = 0; i < cpuNum; i++) {
                disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
            }
        }
        nioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.group(nioEventLoopGroup);
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline cp = ch.pipeline();
                if(channelConfig.isCrc32Check()){
                    Crc32Encode crc32Encode = new Crc32Encode();
                    Crc32Decode crc32Decode = new Crc32Decode();
                    cp.addLast(crc32Encode);
                    cp.addLast(crc32Decode);
                }
                cp.addLast(new ClientChannelHandler(channelManager));
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    public void init(DisruptorExecutorPool disruptorExecutorPool,ChannelConfig channelConfig) {
        this.disruptorExecutorPool = disruptorExecutorPool;
        init(channelConfig);
    }

    public void init(int workSize,ChannelConfig channelConfig) {
        this.disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
        }
        init(channelConfig);
    }

    public Ukcp connect(InetSocketAddress localAddress,InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        if(localAddress==null){
            localAddress = new InetSocketAddress(0);
        }

        ChannelFuture channelFuture = bootstrap.bind(localAddress);
        ChannelFuture sync = channelFuture.syncUninterruptibly();
        NioDatagramChannel channel = (NioDatagramChannel) sync.channel();
        localAddress = channel.localAddress();

        User user = new User(channel, remoteAddress, localAddress);
        IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
        KcpOutput kcpOutput = new KcpOutPutImp();

        ReedSolomon reedSolomon = null;
        if (channelConfig.getFecDataShardCount() != 0 && channelConfig.getFecParityShardCount() != 0) {
            reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(), channelConfig.getFecParityShardCount());
        }

        Ukcp ukcp = new Ukcp(kcpOutput, kcpListener, disruptorSingleExecutor, reedSolomon,channelConfig,channelManager);
        ukcp.user(user);

        disruptorSingleExecutor.execute(() -> {
            try {
                ukcp.getKcpListener().onConnected(ukcp);
            }catch (Throwable throwable){
                ukcp.getKcpListener().handleException(throwable,ukcp);
            }
        });
        channelManager.New(localAddress,ukcp);

        ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor, ukcp);
        DisruptorExecutorPool.scheduleHashedWheel(scheduleTask, ukcp.getInterval());

        return ukcp;
    }

    public Ukcp connect(InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {
        return connect(null,remoteAddress,channelConfig,kcpListener);
    }


    public void stop() {
        //System.out.println("关闭连接");
        channelManager.getAll().forEach(ukcp -> {
            try {
                ukcp.notifyCloseEvent();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        //System.out.println("关闭连接1");
        if (disruptorExecutorPool != null) {
            disruptorExecutorPool.stop();
        }
        //System.out.println("关闭连接2");
        if (nioEventLoopGroup != null) {
            nioEventLoopGroup.shutdownGracefully();
        }
        //System.out.println(Snmp.snmp);
        //System.out.println("关闭连接3");
    }
}
