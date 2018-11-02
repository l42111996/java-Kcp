package kcp;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;


/**
 * Created by JinMiao
 * 2018/9/21.
 */
public class KcpOutPutImp implements KcpOutput {
    @Override
    public void out(ByteBuf data, Kcp kcp) {
        Snmp.snmp.OutPkts.incrementAndGet();
        Snmp.snmp.OutBytes.addAndGet(data.writerIndex());
        User user = (User) kcp.getUser();
        //InetSocketAddress inetSocketAddress = channel.remoteAddress();

        DatagramPacket temp = new DatagramPacket(data,user.getRemoteAddress(), user.getLocalAddress());
        user.getChannelHandlerContext().channel().writeAndFlush(temp);
    }
}
