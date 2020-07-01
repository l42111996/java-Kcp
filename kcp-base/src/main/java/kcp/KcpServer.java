package kcp;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.Snmp;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import threadPool.thread.DisruptorExecutorPool;

import java.util.List;
import java.util.Vector;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class KcpServer {
    private DisruptorExecutorPool disruptorExecutorPool;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private List<Channel> localAddresss = new Vector<>();
    private IChannelManager channelManager;


    public void init(int workSize, KcpListener kcpListener, ChannelConfig channelConfig, int... ports) {
        DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
        }
        init(disruptorExecutorPool, kcpListener, channelConfig, ports);
    }


    public void init(DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener, ChannelConfig channelConfig, int... ports) {
        if(channelConfig.isUseConvChannel()){
            int convIndex = 0;
            if(channelConfig.getFecDataShardCount()!=0&&channelConfig.getFecParityShardCount()!=0){
                convIndex+= Fec.fecHeaderSizePlus2;
            }
            channelManager = new ConvChannelManager(convIndex);
        }else{
            channelManager = new ServerAddressChannelManager();
        }


        boolean epoll = Epoll.isAvailable();
        this.disruptorExecutorPool = disruptorExecutorPool;
        bootstrap = new Bootstrap();
        int cpuNum = Runtime.getRuntime().availableProcessors();
        int bindTimes = 1;
        if (epoll) {
            //ADD SO_REUSEPORT ？ https://www.jianshu.com/p/61df929aa98b
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bindTimes = cpuNum;
        }

        group = epoll ? new EpollEventLoopGroup(cpuNum) : new NioEventLoopGroup(ports.length);
        Class<? extends Channel> channelClass = epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
        bootstrap.channel(channelClass);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ServerChannelHandler serverChannelHandler = new ServerChannelHandler(channelManager, channelConfig, disruptorExecutorPool, kcpListener);
                ChannelPipeline cp = ch.pipeline();
                if(channelConfig.isCrc32Check()){
                    Crc32Encode crc32Encode = new Crc32Encode();
                    Crc32Decode crc32Decode = new Crc32Decode();
                    //这里的crc32放在eventloop网络线程处理的，以后内核有丢包可以优化到单独的一个线程处理
                    cp.addLast(crc32Encode);
                    cp.addLast(crc32Decode);
                }
                cp.addLast(serverChannelHandler);
            }
        });
        //bootstrap.option(ChannelOption.SO_RCVBUF, 10*1024*1024);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);


        for (int port : ports) {
            for (int i = 0; i < bindTimes; i++) {
                ChannelFuture channelFuture = bootstrap.bind(port);
                Channel channel = channelFuture.channel();
                localAddresss.add(channel);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    public void stop() {
        localAddresss.forEach(
                channel -> channel.close()
        );
        channelManager.getAll().forEach(ukcp ->
                ukcp.notifyCloseEvent());
        if (disruptorExecutorPool != null) {
            disruptorExecutorPool.stop();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        System.out.println(Snmp.snmp);
    }

}
