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
public class AbstractKcpServer {
    private Map<SocketAddress,Ukcp> socketAddresUkcpMap = new ConcurrentHashMap<>();

    private DisruptorExecutorPool disruptorExecutorPool;

    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Map<Integer,Channel> localAddresss = new ConcurrentHashMap<>();

    private Map<SocketAddress,Ukcp> ukcpMap = new ConcurrentHashMap<>();


    public AbstractKcpServer(int workSize,KcpListener kcpListener,ChannelConfig channelConfig,int...ports) {
        boolean epoll = true;
        String os = System.getProperty("os.name").toUpperCase();
        if(os.indexOf("WINDOWS")!=-1||os.indexOf("MAC")!=-1){
            epoll = false;
        }
        disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool");
        }
        bootstrap = new Bootstrap();
        group = epoll? new EpollEventLoopGroup(2): new NioEventLoopGroup(2);
        Class<? extends Channel> channelClass = epoll? EpollDatagramChannel.class:NioDatagramChannel.class;
        bootstrap.channel(channelClass);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<Channel>()
        {
            @Override
            protected void initChannel(Channel ch) {
                ServerChannelHandler serverChannelHandler = new ServerChannelHandler(ukcpMap,channelConfig,disruptorExecutorPool,kcpListener);
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


    public void stop(){
        localAddresss.values().forEach(
                channel -> channel.close()
        );
        ukcpMap.values().forEach(ukcp ->
        {
            ukcp.notifyCloseEvent();
        });


        if(group !=null)
            group.shutdownGracefully();
        if(disruptorExecutorPool!=null)
            disruptorExecutorPool.stop();
    }

    public static void main(String[] args) {
        int size = 5;
        for (int i = 0; i < 10; i++) {
            System.out.println(i&size-1);
        }

        //ChannelConfig channelConfig = new ChannelConfig();
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setMtu(300);
        //
        //channelConfig.setNocwnd(true);
        //channelConfig.setRcvwnd(512);
        //channelConfig.setSndwnd(512);
        //channelConfig.setInterval(40);
        //channelConfig.setFastresend(0);
        //
        //
        //new AbstractKcpServer(2, new KcpListener() {
        //    @Override
        //    public void onConnected(Ukcp ukcp) {
        //        System.out.println("有连接进来"+Thread.currentThread().getName());
        //    }
        //
        //    @Override
        //    public void handleReceive(ByteBuf bb, Ukcp kcp) {
        //        System.out.println("收到消息"+Thread.currentThread().getName());
        //        String content = bb.toString(Charset.forName("utf-8"));
        //        kcp.write(bb);//echo
        //    }
        //
        //    @Override
        //    public void handleException(Throwable ex, Ukcp kcp) {
        //        ex.printStackTrace();
        //    }
        //
        //    @Override
        //    public void handleClose(Ukcp kcp) {
        //        System.out.println("连接断开"+Thread.currentThread().getName());
        //    }
        //},channelConfig,10000);
    }
}
