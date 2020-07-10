package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

/**
 * 重复新连接进入断开测试内存泄漏服务器
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpDisconnectExampleServer implements KcpListener {

    public static void main(String[] args) {

        KcpDisconnectExampleServer kcpRttExampleServer = new KcpDisconnectExampleServer();
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true, 40, 2, true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        //channelConfig.setAckNoDelay(true);
        //channelConfig.setCrc32Check(true);
        channelConfig.setUseConvChannel(true);
        channelConfig.setTimeoutMillis(5000);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(Runtime.getRuntime().availableProcessors(), kcpRttExampleServer, channelConfig, 10031);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来 " + Thread.currentThread().getName() + ukcp.user().getRemoteAddress());
    }



    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        kcp.write(buf);
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了 "+kcp.getConv());
    }
}