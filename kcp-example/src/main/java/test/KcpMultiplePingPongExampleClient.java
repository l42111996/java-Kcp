package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 测试多连接吞吐量
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpMultiplePingPongExampleClient implements KcpListener {

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,0,true);
        channelConfig.setSndwnd(256);
        channelConfig.setRcvwnd(256);
        channelConfig.setMtu(400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);

        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(Runtime.getRuntime().availableProcessors(),channelConfig);
        KcpMultiplePingPongExampleClient kcpMultiplePingPongExampleClient = new KcpMultiplePingPongExampleClient();

        int clientNumber = 1000;
        for (int i = 0; i < clientNumber; i++) {
            channelConfig.setConv(i);
            kcpClient.connect(new InetSocketAddress("127.0.0.1", 10011), channelConfig, kcpMultiplePingPongExampleClient);
        }
    }

    Timer timer = new Timer();

    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println(ukcp.getConv());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1004);
                byteBuf.writeInt(1);
                byte[] bytes = new byte[1000];
                byteBuf.writeBytes(bytes);
                ukcp.writeMessage(byteBuf);
                byteBuf.release();
            }
        },100,100);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //System.out.println("收到消息");
        //ukcp.writeMessage(byteBuf);
        //int id = byteBuf.getInt(0);
        //if(j-id%10!=0){
        //    System.out.println("id"+id +"  j" +j);
        //}
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了"+kcp.getConv());
    }


}
