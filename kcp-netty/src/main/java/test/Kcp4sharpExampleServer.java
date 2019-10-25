package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

/**
 * Created by JinMiao
 * 2019-07-23.
 */
public class Kcp4sharpExampleServer implements KcpListener {

    public static void main(String[] args) {

        Kcp4sharpExampleServer kcpRttExampleServer = new Kcp4sharpExampleServer();

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,10,2,true);
        channelConfig.setSndwnd(300);
        channelConfig.setRcvwnd(300);
        channelConfig.setMtu(512);
        channelConfig.setAckNoDelay(true);
        channelConfig.setTimeoutMillis(10000);
        channelConfig.setAutoSetConv(true);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //c# crc32未实现
        channelConfig.setCrc32Check(false);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(Runtime.getRuntime().availableProcessors(), kcpRttExampleServer,channelConfig,10009);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
    }

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        byte[] bytes = new  byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(),bytes);
        System.out.println("收到消息: "+new String(bytes));
        kcp.write(buf);
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
