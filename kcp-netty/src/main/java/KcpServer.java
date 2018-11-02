import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.AbstractKcpServer;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.Ukcp;

/**
 * Created by JinMiao
 * 2018/11/2.
 */
public class KcpServer implements KcpListener {

    public static void main(String[] args) {
        new KcpServer().init();


    }


    public void init(){
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setFastresend(0);
        channelConfig.setSndwnd(512);
        channelConfig.setRcvwnd(512);
        channelConfig.setMtu(300);
        channelConfig.setFecDataShardCount(10);
        channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setInterval(40);
        channelConfig.setNocwnd(true);
        AbstractKcpServer abstractKcpServer = new AbstractKcpServer(2,this,channelConfig,10001);
    }

    @Override
    public void onConnected(Ukcp ukcp) {
        ukcp.setConv(10);
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
    }

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        short curCount = buf.getShort(buf.readerIndex());

        kcp.write(buf);
        if (curCount == -1) {
            kcp.notifyCloseEvent();
        }
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {

    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp  = new Snmp();
    }
}
