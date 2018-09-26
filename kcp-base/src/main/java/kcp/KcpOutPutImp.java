package kcp;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2018/9/21.
 */
public class KcpOutPutImp implements KcpOutput {
    @Override
    public void out(ByteBuf data, Kcp kcp) {
        Snmp.snmp.OutPkts.incrementAndGet();
        Snmp.snmp.OutBytes.addAndGet(data.writerIndex());
        Channel channel = (Channel) kcp.getUser();
        DatagramPacket temp = new DatagramPacket(data, (InetSocketAddress) channel.remoteAddress(), (InetSocketAddress)channel.localAddress());
        channel.writeAndFlush(temp);

    }
}
