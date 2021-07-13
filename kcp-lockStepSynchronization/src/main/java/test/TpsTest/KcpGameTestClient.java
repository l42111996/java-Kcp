package test.TpsTest;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.*;
import test.TimerThreadPool;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * 客户端50ms发送一个自增的包 +20字节自定义消息
 * 服务器8个人一个房间  收到消息之后缓存下来  50ms一次把消息广播下去
 * 客户端收到回来的消息计算tps
 * 5秒做一次tps打印
 * 为朋友做测试用的
 * Created by JinMiao
 */
public class KcpGameTestClient implements KcpListener
{

    public static void main(String[] args) {
        String ip = "127.0.0.1";
        if(args.length>0){
            ip = args[0];
        }
        int number= 100;
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
        KcpGameTestClient kcpGameTestClient = new KcpGameTestClient();

        for (int i = 0; i < number; i++) {
            kcpClient.connect(new InetSocketAddress(ip, 10019), channelConfig, kcpGameTestClient);
        }

        TimerThreadPool.scheduleWithFixedDelay(() -> {
            //long inSegs = Snmp.snmp.InSegs.longValue();
            //if(inSegs==0){
            //    inSegs = 1;
            //}
            //System.out.println("每秒收包"+ (Snmp.snmp.InBytes.longValue()/1024.0/1024.0*8.0)+" M"+" 丢包率 "+((double)Snmp.snmp.LostSegs.longValue()/inSegs));
            //System.out.println("每秒发包"+ (Snmp.snmp.OutBytes.longValue()/1024.0/1024.0*8.0)+" M");
            //System.out.println(Snmp.snmp.toString());
            //System.out.println();
            //
            //Snmp.snmp = new Snmp();
            kcpGameTestClient.tpsCounter.count();
        },5000);

    }

    TpsCounter tpsCounter = new TpsCounter();

    @Override
    public void onConnected(Ukcp ukcp)
    {
        TpsChannelClientCache tpsChannelCache = new TpsChannelClientCache();
        ukcp.user().setCache(tpsChannelCache);

        //模拟按键事件
        TimerThreadPool.scheduleWithFixedDelay(() -> {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
            int packetId = tpsChannelCache.getIndex();
            packetId++;
            byteBuf.writeInt(packetId);
            byteBuf.writeBytes(new byte[40]);
            tpsChannelCache.setIndex(packetId);
            tpsChannelCache.getPacketTime().put(packetId,System.currentTimeMillis());
            tpsCounter.add(0);
            ukcp.write(byteBuf);
            byteBuf.release();
        },100);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp)
    {
        int size = byteBuf.readShort();
        for (int i = 0; i < size; i++) {
            int packetId = byteBuf.readInt();
            TpsChannelClientCache tpsChannelCache = ukcp.user().getCache();
            Map<Integer, Long> packetTime =  tpsChannelCache.getPacketTime();
            tpsCounter.set(0, (int) (System.currentTimeMillis()-packetTime.remove(packetId)));
        }

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
