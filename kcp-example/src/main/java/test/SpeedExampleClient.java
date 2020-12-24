package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;
import threadPool.disruptor.DisruptorExecutorPool;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleClient implements KcpListener {


    public SpeedExampleClient() {
    }

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,30,2,true);
        channelConfig.setSndwnd(2048);
        channelConfig.setRcvwnd(2048);
        channelConfig.setMtu(1400);
        channelConfig.setAckNoDelay(true);
        channelConfig.setConv(55);
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setCrc32Check(false);
        channelConfig.setWriteBufferSize(channelConfig.getMtu()*300000);
        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);

        SpeedExampleClient speedExampleClient = new SpeedExampleClient();
        kcpClient.connect(new InetSocketAddress("127.0.0.1",20004),channelConfig,speedExampleClient);

    }
    private static final int messageSize = 2048;
    private long start = System.currentTimeMillis();

    @Override
    public void onConnected(Ukcp ukcp) {
        new Thread(() -> {
            for(;;){
                long now =System.currentTimeMillis();
                if(now-start>=1000){
                    System.out.println("耗时 :" +(now-start) +" 发送数据: " +(Snmp.snmp.OutBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+Snmp.snmp.BytesSent.doubleValue()/1024.0/1024.0+" MB");
                    System.out.println(Snmp.snmp.toString());
                    Snmp.snmp = new Snmp();
                    start=now;
                }
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(messageSize);
                byteBuf.writeBytes(new byte[messageSize]);
                if(!ukcp.write(byteBuf)){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                byteBuf.release();
            }
        }).start();
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp)
    {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
    }
}
