import com.backblaze.erasure.ReedSolomon;
import com.backblaze.erasure.fec.Snmp;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import kcp.*;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.DisruptorSingleExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * Created by JinMiao
 * 2018/9/10.
 */
public class JKcp  implements KcpOutput, KcpListener {

    private Ukcp ukcp;

    private NioEventLoopGroup nioEventLoopGroup;

    private NioDatagramChannel channel;

    private InetSocketAddress addr;

    private InetSocketAddress remote;


    public static void main(String[] args) {
        new JKcp().init();
    }

    public void init(){
        DisruptorSingleExecutor disruptorSingleExecutor = new DisruptorSingleExecutor("Disruptor");
        disruptorSingleExecutor.start();
        nioEventLoopGroup = new NioEventLoopGroup(1);
        EventLoop eventExecutors  = nioEventLoopGroup.next();
        ukcp = new Ukcp(10,this,this,disruptorSingleExecutor,eventExecutors);
        ukcp.setMtu(300);
        ukcp.setNocwnd(true);
        ukcp.setRcvWnd(512);
        ukcp.setSndWnd(512);
        ukcp.setInterval(40);
        ukcp.setFastResend(0);
        //ukcp.setAckNoDelay(true);

        ReedSolomon reedSolomon = ReedSolomon.create(10,3);
        ukcp.initFec(reedSolomon);




        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.group(nioEventLoopGroup);
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>()
        {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception
            {
                ChannelPipeline cp = ch.pipeline();
                cp.addLast(new ChannelInboundHandlerAdapter()
                {
                    boolean connected = false;
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                    {
                        if(!connected){
                            connected = true;
                            ukcp.getKcpListener().onConnected(ukcp);
                        }
                        DatagramPacket dp = (DatagramPacket) msg;
                        ukcp.read(dp.content());
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                    {
                        ukcp.getKcpListener().handleException(cause,ukcp);
                        cause.printStackTrace();
                    }
                });
                cp.addLast(new ChannelInboundHandlerAdapter(){
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        System.out.println("fire read "+Thread.currentThread().getName());

                    }
                });
            }
        });
        ChannelFuture sync = bootstrap.bind(30000).syncUninterruptibly();
        channel = (NioDatagramChannel) sync.channel();
        addr = channel.localAddress();

        //remote = new InetSocketAddress("10.60.100.191",10001);

        remote = new InetSocketAddress("127.0.0.1",10001);

        future = scheduleSrv.scheduleWithFixedDelay(() -> {
            ukcp.write(rttMsg(++count));
            if (count >= rtts.length) {
                // finish
                future.cancel(true);
                ukcp.write(rttMsg(-1));

            }
        }, 20, 20, TimeUnit.MILLISECONDS);

        ScheduleTask scheduleTask = new ScheduleTask(disruptorSingleExecutor,ukcp,new ConcurrentHashMap<>());
        DisruptorExecutorPool.schedule(scheduleTask, ukcp.getInterval());


        Runtime.getRuntime().addShutdownHook(
                new Thread(() ->
                        nioEventLoopGroup.shutdownGracefully()));

    }


    @Override
    public void out(ByteBuf data, Kcp kcp) {
        Snmp.snmp.OutPkts.incrementAndGet();
        Snmp.snmp.OutBytes.addAndGet(data.writerIndex());
        DatagramPacket temp = new DatagramPacket(data, remote, this.addr);
        this.channel.writeAndFlush(temp);
    }


    private final ByteBuf data;

    private int[] rtts;

    private volatile int count;

    private ScheduledExecutorService scheduleSrv;

    private ScheduledFuture<?> future = null;

    private final long startTime ;


    public JKcp() {
        data = Unpooled.buffer(200);
        for (int i = 0; i < data.capacity(); i++) {
            data.writeByte((byte) i);
        }

        rtts = new int[300];
        for (int i = 0; i < rtts.length; i++) {
            rtts[i] = -1;
        }
        startTime = System.currentTimeMillis();
        scheduleSrv = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onConnected(Ukcp ukcp) {

    }

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        int curCount = buf.readShort();

        if (curCount == -1) {
            scheduleSrv.schedule(new Runnable() {
                @Override
                public void run() {
                    int sum = 0;
                    for (int rtt : rtts) {
                        sum += rtt;
                    }
                    System.out.println("average: "+ (sum / rtts.length));
                    System.out.println(Snmp.snmp.toString());
                    ukcp.setCloseTime(System.currentTimeMillis());
                    System.exit(0);
                }
            }, 3, TimeUnit.SECONDS);
        } else {
            int idx = curCount - 1;
            long time = buf.readInt();
            if (rtts[idx] != -1) {
                System.out.println("???");
            }
            //log.info("rcv count {} {}", curCount, System.currentTimeMillis());
            rtts[idx] = (int) (System.currentTimeMillis() - startTime - time);
            System.out.println("rtt : "+ curCount+"  "+ rtts[idx]);
        }
        //buf.release();
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        scheduleSrv.shutdown();
        try {
            scheduleSrv.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int sum = 0;
        for (int rtt : rtts) {
            sum += rtt;
        }
        System.out.println("average: "+ (sum / rtts.length));
        System.out.println(Snmp.snmp.toString());
    }



    /**
     * count+timestamp+dataLen+data
     *
     * @param count
     * @return
     */
    public ByteBuf rttMsg(int count) {
        ByteBuf buf = Unpooled.buffer(10);
        buf.writeShort(count);
        buf.writeInt((int) (System.currentTimeMillis() - startTime));

        //int dataLen = new Random().nextInt(200);
        //buf.writeBytes(new byte[dataLen]);

        int dataLen = data.readableBytes();
        buf.writeShort(dataLen);
        buf.writeBytes(data, data.readerIndex(), dataLen);

        return buf;
    }
}
