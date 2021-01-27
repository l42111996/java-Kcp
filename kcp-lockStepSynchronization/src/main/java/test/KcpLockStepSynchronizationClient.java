package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;

import java.net.InetSocketAddress;

/**
 * 模拟帧同步
 * 50ms一帧
 * 20字节一个包
 * Created by JinMiao
 * 2019-06-25.
 */
public class KcpLockStepSynchronizationClient implements KcpListener
{

    public static void main(String[] args) {
        String ip = "49.232.119.183";
        if(args.length>0){
            ip = args[0];
        }
        int number= 1;
        if(args.length>1){
            number = Integer.parseInt(args[1]);
        }

        KcpClient kcpClient = new KcpClient();


        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,2,true);
        channelConfig.setSndwnd(300);
        channelConfig.setRcvwnd(300);
        channelConfig.setMtu(500);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);

        kcpClient.init(channelConfig);
        KcpLockStepSynchronizationClient lockStepSynchronizationClient = new KcpLockStepSynchronizationClient();

        for (int i = 0; i < number; i++) {
            kcpClient.connect(new InetSocketAddress(ip, 10009), channelConfig, lockStepSynchronizationClient);
        }

        TimerThreadPool.scheduleWithFixedDelay(() -> {
            long inSegs = Snmp.snmp.InSegs.longValue();
            if(inSegs==0){
                inSegs = 1;
            }
            System.out.println("每秒收包"+ (Snmp.snmp.InBytes.longValue()/1024.0/1024.0*8.0)+" M"+" 丢包率 "+((double)Snmp.snmp.LostSegs.longValue()/inSegs));
            System.out.println("每秒发包"+ (Snmp.snmp.OutBytes.longValue()/1024.0/1024.0*8.0)+" M");
            System.out.println(Snmp.snmp.toString());
            System.out.println();

            Snmp.snmp = new Snmp();
        },1000);

    }




    @Override
    public void onConnected(Ukcp ukcp)
    {
        //模拟按键事件
        TimerThreadPool.scheduleWithFixedDelay(() -> {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(20);
            byteBuf.writeBytes(new byte[20]);
            ukcp.write(byteBuf);
            byteBuf.release();
        },50);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {

    }

    @Override
    public void handleException(Throwable ex, Ukcp ukcp) {
        ex.printStackTrace();

    }

    @Override
    public void handleClose(Ukcp ukcp) {
        System.out.println("连接断开了"+ukcp.user().getRemoteAddress());
    }
}
