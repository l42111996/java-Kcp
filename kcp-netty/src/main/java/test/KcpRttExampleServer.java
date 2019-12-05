package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

/**
 *
 * Created by JinMiao
 * 2018/11/2.
 */
public class KcpRttExampleServer implements KcpListener {

    public static void main(String[] args) {

        KcpRttExampleServer kcpRttExampleServer = new KcpRttExampleServer();

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,2,true);
        channelConfig.setSndwnd(512);
        channelConfig.setRcvwnd(512);
        channelConfig.setMtu(512);
        channelConfig.setFecDataShardCount(3);
        channelConfig.setFecParityShardCount(1);
        channelConfig.setAckNoDelay(true);
        channelConfig.setTimeoutMillis(10000);
        channelConfig.setAutoSetConv(true);
        channelConfig.setUseConvChannel(true);
        channelConfig.setCrc32Check(true);
        channelConfig.setKcpTag(true);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(Runtime.getRuntime().availableProcessors(), kcpRttExampleServer,channelConfig,20003);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
    }

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp,int protocolType) {
        short curCount = buf.getShort(buf.readerIndex());
        System.out.println(Thread.currentThread().getName()+"  收到消息 "+curCount);
        kcp.writeOrderedReliableMessage(buf);
        if (curCount == -1) {
            kcp.notifyCloseEvent();
        }
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp  = new Snmp();
    }
}
