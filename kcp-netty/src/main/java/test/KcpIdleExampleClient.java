package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import kcp.*;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2019-07-10.
 */
public class KcpIdleExampleClient implements KcpListener {

    public static void main(String[] args) {

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,2,true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(false);
        channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(Runtime.getRuntime().availableProcessors(),channelConfig);


        for (int i = 0; i < 3; i++) {
            if(i%1000==0){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            KcpIdleExampleClient kcpIdleExampleClient = new KcpIdleExampleClient();
            //kcpClient.connect(new InetSocketAddress("10.60.100.191", 10020), channelConfig, kcpIdleExampleClient);
            kcpClient.connect(new InetSocketAddress("127.0.0.1", 10020), channelConfig, kcpIdleExampleClient);
        }

    }
    int i =0;

    @Override
    public void onConnected(Ukcp ukcp) {
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(124);
        byteBuf.writeInt(i++);
        byte[] bytes = new byte[120];
        byteBuf.writeBytes(bytes);
        ukcp.writeKcpMessage(byteBuf);
        byteBuf.release();
    }
    //int j =0;

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp,int protocolType) {
        //ukcp.write(byteBuf);
        //int id = byteBuf.getInt(0);
        ////if(j-id%10!=0){
        ////    System.out.println("id"+id +"  j" +j);
        ////}
        //
        //j++;
        //if(j%100000==0){
        //    System.out.println(Snmp.snmp.toString());
        //    System.out.println("收到了 返回回去"+j);
        //}
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了");
    }


}