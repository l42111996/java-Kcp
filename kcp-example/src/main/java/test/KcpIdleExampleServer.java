package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试大量连接不通讯的例子
 * Created by JinMiao
 * 2019-07-10.
 */
public class KcpIdleExampleServer implements KcpListener {

    public static void main(String[] args) {

        KcpIdleExampleServer kcpIdleExampleServer = new KcpIdleExampleServer();
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,10,2,true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(Runtime.getRuntime().availableProcessors(), kcpIdleExampleServer, channelConfig, 10020);
        System.out.println("ce");
    }

    private AtomicInteger atomicInteger = new AtomicInteger();

    private AtomicInteger recieveAtomicInteger = new AtomicInteger();



    @Override
    public void onConnected(Ukcp ukcp) {
        int id = atomicInteger.incrementAndGet();
        ukcp.user().setCache(id);

        System.out.println("有连接进来,当前连接" + id);
    }

    int i = 0;

    long start = System.currentTimeMillis();

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        System.out.println("收到消息 "+recieveAtomicInteger.incrementAndGet());
        i++;
        long now = System.currentTimeMillis();
        if (now - start > 1000) {
            System.out.println("收到消息 time: " + (now - start) + "  message :" + i);
            start = now;
            i = 0;
        }
        //kcp.write(buf);
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp = new Snmp();
        System.out.println("连接断开了,当前连接" + atomicInteger.decrementAndGet());
    }
}
