package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据conv确定一个sesion
 *  这里conv是从消息第一个字节读取的
 * Created by JinMiao
 * 2019/10/17.
 */
public class ConvChannelManager implements IChannelManager {

    private int convIndex;

    public ConvChannelManager(int convIndex) {
        this.convIndex = convIndex;
    }

    private Map<Integer,Ukcp> ukcpMap = new ConcurrentHashMap<>();
    @Override
    public Ukcp get(Channel channel, DatagramPacket msg) {
        ByteBuf byteBuf = msg.content();
        int conv =byteBuf.getInt(convIndex);
        return ukcpMap.get(conv);
    }

    @Override
    public void New(Channel channel, Ukcp ukcp) {
        ukcpMap.put(ukcp.getConv(),ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.getConv());
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
