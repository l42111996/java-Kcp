package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重复新连接进入断开测试内存泄漏客户端
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpDisconnectExampleClient implements KcpListener {

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true, 40, 2, true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);
        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);
        channelConfig.setConv(55);
        channelConfig.setUseConvChannel(true);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(1, channelConfig);

        KcpDisconnectExampleClient kcpClientRttExample = new KcpDisconnectExampleClient();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    try {
                        channelConfig.setConv(id.incrementAndGet());
                        kcpClient.connect(new InetSocketAddress("127.0.0.1", 10031), channelConfig, kcpClientRttExample);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }, 1000, 1000);
    }

    private static  final AtomicInteger id = new AtomicInteger();

    @Override
    public void onConnected(Ukcp ukcp) {
        for (int i = 0; i < 100; i++) {
            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1024);
            byteBuf.writeInt(i);
            byte[] bytes = new byte[1020];
            byteBuf.writeBytes(bytes);
            ukcp.write(byteBuf);
            byteBuf.release();
        }
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        if(byteBuf.getInt(0)==99){
            ukcp.close();
        }
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了");
    }


}
