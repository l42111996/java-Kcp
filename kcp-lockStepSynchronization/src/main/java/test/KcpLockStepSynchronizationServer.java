package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

/**
 * 模拟帧同步测试吞吐和流量
 * 50ms一帧
 * Created by JinMiao
 * 2019-06-25.
 */
public class KcpLockStepSynchronizationServer implements KcpListener
{

    private RoomManager roomManager;


    public static void main(String[] args) {
        KcpLockStepSynchronizationServer kcpLockStepSynchronizationServer = new KcpLockStepSynchronizationServer();
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
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(kcpLockStepSynchronizationServer, channelConfig, 10009);

        kcpLockStepSynchronizationServer.roomManager = new RoomManager();


        TimerThreadPool.scheduleWithFixedDelay(() -> {
            try {
                long inSegs = Snmp.snmp.InSegs.longValue();
                if(inSegs==0){
                    inSegs = 1;
                }
                System.out.println("每秒收包"+ (Snmp.snmp.InBytes.longValue()/1024.0/1024.0*8.0)+" M"+" 丢包率 "+((double)Snmp.snmp.LostSegs.longValue()/inSegs));
                System.out.println("每秒发包"+ (Snmp.snmp.OutBytes.longValue()/1024.0/1024.0*8.0)+" M");
                System.out.println(Snmp.snmp.toString());
                System.out.println();
                Snmp.snmp = new Snmp();

            }catch (Exception e){
                e.printStackTrace();
            }
        },1000);
    }







    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+ukcp.user());

        Player player = new Player(new IWriter() {
            @Override
            public void write(ByteBuf byteBuf) {
                ukcp.write(byteBuf);
            }
        });
        ukcp.user().setCache(player);
        roomManager.joinRoom(player);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //System.out.println("收到消息"+ukcp.user());
        Player player = ukcp.user().getCache();
        Room room = roomManager.getRoom(player.getId());
        ByteBuf byteBufAllocator = ByteBufAllocator.DEFAULT.directBuffer(20);
        byteBuf.readBytes(byteBufAllocator);
        byteBufAllocator.readerIndex(0);
        byteBufAllocator.writerIndex(20);
        room.getiMessageExecutor().execute(() ->{
                    player.getMessages().add(byteBufAllocator);
                }
        );
    }

    @Override
    public void handleException(Throwable ex, Ukcp ukcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp ukcp) {
        Player player = ukcp.user().getCache();
        roomManager.remove(player.getId());
        System.out.println("连接断开了"+ukcp.user().getRemoteAddress());
    }
}
