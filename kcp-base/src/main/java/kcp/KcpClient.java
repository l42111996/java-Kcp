package kcp;

import com.backblaze.erasure.ReedSolomon;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * kcp客户端
 * Created by JinMiao
 * 2019-06-26.
 */
public class KcpClient {

    private DisruptorExecutorPool disruptorExecutorPool;
    private Bootstrap bootstrap;
    private EventLoopGroup nioEventLoopGroup;
    private NioDatagramChannel channel;
    private InetSocketAddress localAddress;
    private Map<SocketAddress, Ukcp> ukcpMap = new ConcurrentHashMap<>();


    public void init(int bindPort) {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        if (disruptorExecutorPool == null) {
            this.disruptorExecutorPool = new DisruptorExecutorPool();
            for (int i = 0; i < cpuNum; i++) {
                disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
            }
        }
        boolean epoll = true;
        String os = System.getProperty("os.name").toUpperCase();
        if (os.indexOf("WINDOWS") != -1 || os.indexOf("MAC") != -1) {
            epoll = false;
        }
        nioEventLoopGroup = epoll ? new EpollEventLoopGroup(2) : new NioEventLoopGroup(2);
        bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.group(nioEventLoopGroup);
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ChannelPipeline cp = ch.pipeline();
                cp.addLast(new ClientChannelHandler(ukcpMap));
            }
        });
        ChannelFuture channelFuture;
        if (bindPort == 0) {
            channelFuture = bootstrap.bind();
        } else {
            channelFuture = bootstrap.bind(bindPort);
        }
        ChannelFuture sync = channelFuture.syncUninterruptibly();
        channel = (NioDatagramChannel) sync.channel();
        localAddress = channel.localAddress();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    public void init(DisruptorExecutorPool disruptorExecutorPool, int bindPort) {
        this.disruptorExecutorPool = disruptorExecutorPool;
        init(bindPort);

    }

    public void init(int workSize, int bindPort) {
        this.disruptorExecutorPool = new DisruptorExecutorPool();
        for (int i = 0; i < workSize; i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptorExecutorPool" + i);
        }
        init(bindPort);
    }

    public Ukcp connect(InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener) {

        User user = new User(this.channel, remoteAddress, this.localAddress);
        IMessageExecutor disruptorSingleExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
        KcpOutput kcpOutput = new KcpOutPutImp();

        ReedSolomon reedSolomon = null;
        if (channelConfig.getFecDataShardCount() != 0 && channelConfig.getFecParityShardCount() != 0) {
            reedSolomon = ReedSolomon.create(channelConfig.getFecDataShardCount(), channelConfig.getFecParityShardCount());
        }

        Ukcp ukcp = new Ukcp(10, kcpOutput, kcpListener, disruptorSingleExecutor, reedSolomon,channelConfig);
        ukcp.user(user);

        disruptorSingleExecutor.execute(() -> {
            try {
                ukcp.getKcpListener().onConnected(ukcp);
            }catch (Throwable throwable){
                ukcp.getKcpListener().handleException(throwable,ukcp);
            }
        });

        ukcpMap.put(remoteAddress, ukcp);

        ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor, ukcp, ukcpMap);
        DisruptorExecutorPool.schedule(scheduleTask, ukcp.getInterval());

        return ukcp;
    }

    public Map<SocketAddress, Ukcp> getUkcpMap() {
        return ukcpMap;
    }

    public void stop() {
        //System.out.println("关闭连接");
        ukcpMap.values().forEach(ukcp -> {
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
        //System.out.println("关闭连接3");
    }
}
