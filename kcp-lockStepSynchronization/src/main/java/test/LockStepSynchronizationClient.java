package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;
import threadPool.thread.DisruptorExecutorPool;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2019-06-25.
 */
public class LockStepSynchronizationClient implements KcpListener
{

    public static void main(String[] args) {
        String ip = "127.0.0.1";
        if(args.length>0){
            ip = args[0];
        }
        int number= 1000;
        if(args.length>1){
            number = Integer.parseInt(args[1]);
        }

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(Runtime.getRuntime().availableProcessors());

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setFastresend(2);
        channelConfig.setSndwnd(300);
        channelConfig.setRcvwnd(300);
        channelConfig.setMtu(500);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setInterval(40);
        channelConfig.setNocwnd(true);
        channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);

        LockStepSynchronizationClient lockStepSynchronizationClient = new LockStepSynchronizationClient();

        for (int i = 0; i < number; i++) {
            kcpClient.connect(new InetSocketAddress(ip, 10009), channelConfig, lockStepSynchronizationClient);
        }

        DisruptorExecutorPool.scheduleWithFixedDelay(() -> {
            System.out.println("每秒收包"+ (Snmp.snmp.InBytes.get()/1024.0/1024.0*8.0)+" M");
            System.out.println("每秒发包"+ (Snmp.snmp.OutBytes.get()/1024.0/1024.0*8.0)+" M");
            System.out.println();

            Snmp.snmp = new Snmp();
        },1000);

    }




    @Override
    public void onConnected(Ukcp ukcp)
    {
        //模拟按键事件
        DisruptorExecutorPool.scheduleWithFixedDelay(() -> {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(20);
            byteBuf.writeBytes(new byte[20]);
            ukcp.write(byteBuf);
            byteBuf.release();
        },50);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //System.out.println("收到数据"+byteBuf.readableBytes());
    }

    @Override
    public void handleException(Throwable ex, Ukcp ukcp) {

    }

    @Override
    public void handleClose(Ukcp ukcp) {
        System.out.println("连接断开了"+ukcp.user().getRemoteAddress());
    }
}
