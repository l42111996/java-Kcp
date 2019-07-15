package kcp;

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

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class KcpServer {
    private DisruptorExecutorPool disruptorExecutorPool;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    //private Map<Integer,Channel> localAddresss = new ConcurrentHashMap<>();
    private List<Channel> localAddresss = new Vector<>();
    private Map<SocketAddress,Ukcp> clientMap = new ConcurrentHashMap<>();


    public void init(int workSize, KcpListener kcpListener, ChannelConfig channelConfig, int...ports){
        DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool"+i);
        }
        init(disruptorExecutorPool,kcpListener,channelConfig,ports);
    }


    public void init(DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener, ChannelConfig channelConfig, int...ports){
        boolean epoll = Epoll.isAvailable();
        this.disruptorExecutorPool = disruptorExecutorPool;
        bootstrap = new Bootstrap();
        int cpuNum = Runtime.getRuntime().availableProcessors();
        int bindTimes = 1;
        if(epoll){
            //ADD SO_REUSEPORT ？ https://www.jianshu.com/p/61df929aa98b
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bindTimes = cpuNum;
        }

        group = epoll? new EpollEventLoopGroup(cpuNum): new NioEventLoopGroup(ports.length);
        Class<? extends Channel> channelClass = epoll? EpollDatagramChannel.class:NioDatagramChannel.class;
        bootstrap.channel(channelClass);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<Channel>()
        {
            @Override
            protected void initChannel(Channel ch) {
                ServerChannelHandler serverChannelHandler = new ServerChannelHandler(clientMap,channelConfig,disruptorExecutorPool,kcpListener);
                ChannelPipeline cp = ch.pipeline();
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

    public void stop(){
        localAddresss.forEach(
                channel -> channel.close()
        );
        clientMap.values().forEach(ukcp ->
                ukcp.notifyCloseEvent());
        if(disruptorExecutorPool!=null) {
            disruptorExecutorPool.stop();
        }
        if(group !=null)
            group.shutdownGracefully();
        System.out.println(Snmp.snmp);
    }

}
