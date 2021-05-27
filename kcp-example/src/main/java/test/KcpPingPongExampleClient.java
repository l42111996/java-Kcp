package test;

import com.backblaze.erasure.FecAdapt;
import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import kcp.*;
import threadPool.disruptor.DisruptorExecutorPool;
import threadPool.disruptor.DisruptorSingleExecutor;

import java.net.InetSocketAddress;

/**
 * 测试单连接吞吐量
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpPingPongExampleClient implements KcpListener {

    static DefaultEventLoop logicThread = new DefaultEventLoop();
    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,2,true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()));
        //channelConfig.setFecAdapt(new FecAdapt(10,3));
        channelConfig.setAckNoDelay(false);
        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);

        KcpPingPongExampleClient kcpClientRttExample = new KcpPingPongExampleClient();
        kcpClient.connect(new InetSocketAddress("127.0.0.1", 10001), channelConfig, kcpClientRttExample);
    }
    int i =0;

    @Override
    public void onConnected(Ukcp ukcp) {
        for (int i = 0; i < 100; i++) {
            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1024);
            byteBuf.writeInt(i++);
            byte[] bytes = new byte[1020];
            byteBuf.writeBytes(bytes);
            ukcp.write(byteBuf);
            byteBuf.release();
        }
    }
    int j =0;

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        ByteBuf newBuf = byteBuf.retainedDuplicate();
        logicThread.execute(() -> {
            try {
                ukcp.write(newBuf);
                newBuf.release();
                j++;
                if(j%100000==0){
                    System.out.println(Snmp.snmp.toString());
                    System.out.println("收到了 返回回去"+j);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        });

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
