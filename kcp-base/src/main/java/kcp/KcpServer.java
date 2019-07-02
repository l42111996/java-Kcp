package kcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import threadPool.thread.DisruptorExecutorPool;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class KcpServer {
    private DisruptorExecutorPool disruptorExecutorPool;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Map<Integer,Channel> localAddresss = new ConcurrentHashMap<>();
    private Map<SocketAddress,Ukcp> clientMap = new ConcurrentHashMap<>();


    public void init(int workSize, KcpListener kcpListener, ChannelConfig channelConfig, int...ports){
        DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool"+i);
        }
        init(disruptorExecutorPool,kcpListener,channelConfig,ports);
    }


    public void init(DisruptorExecutorPool disruptorExecutorPool, KcpListener kcpListener, ChannelConfig channelConfig, int...ports){
        boolean epoll = true;
        String os = System.getProperty("os.name").toUpperCase();
        if(os.indexOf("WINDOWS")!=-1||os.indexOf("MAC")!=-1){
            epoll = false;
        }
        this.disruptorExecutorPool = disruptorExecutorPool;
        bootstrap = new Bootstrap();
        group = epoll? new EpollEventLoopGroup(2): new NioEventLoopGroup(2);
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
        for (int port : ports) {
            ChannelFuture channelFuture = bootstrap.bind(port);
            Channel channel = channelFuture.channel();
            localAddresss.put(port,channel);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }


    public Map<Integer, Channel> getLocalAddresss() {
        return localAddresss;
    }

    public void setLocalAddresss(Map<Integer, Channel> localAddresss) {
        this.localAddresss = localAddresss;
    }

    public void stop(){
        localAddresss.values().forEach(
                channel -> channel.close()
        );
        clientMap.values().forEach(ukcp ->
                ukcp.notifyCloseEvent());
        if(disruptorExecutorPool!=null) {
            disruptorExecutorPool.stop();
        }
        if(group !=null)
            group.shutdownGracefully();
    }

}
