import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.KcpServer;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.Ukcp;

/**
 *
 * Created by JinMiao
 * 2018/11/2.
 */
public class KcpServerRttExample implements KcpListener {

    public static void main(String[] args) {
        new KcpServerRttExample().init();
    }


    public void init(){
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setFastresend(2);
        channelConfig.setSndwnd(512);
        channelConfig.setRcvwnd(512);
        channelConfig.setMtu(1400);
        channelConfig.setFecDataShardCount(10);
        channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setInterval(40);
        channelConfig.setNocwnd(true);
        channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);
        KcpServer abstractKcpServer = new KcpServer(2,this,channelConfig,10003);
    }

    @Override
    public void onConnected(Ukcp ukcp) {
        ukcp.setConv(10);
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
    }

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        short curCount = buf.getShort(buf.readerIndex());
        System.out.println(Thread.currentThread().getName()+"  收到消息 "+curCount);
        ByteBuf sendBytebuf = buf.retainedDuplicate();

        kcp.write(sendBytebuf);
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
