package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据conv确定一个session
 * Created by JinMiao
 * 2019/10/17.
 */
public class ServerConvChannelManager implements IChannelManager {

    private int convIndex;

    public ServerConvChannelManager(int convIndex) {
        this.convIndex = convIndex;
    }

    private Map<Integer, Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(DatagramPacket msg) {
        int conv = getConv(msg);
        return ukcpMap.get(conv);
    }


    private int getConv(DatagramPacket msg) {
        ByteBuf byteBuf = msg.content();
        return byteBuf.getIntLE(byteBuf.readerIndex() + convIndex);
    }

    @Override
    public void New(SocketAddress socketAddress, Ukcp ukcp, DatagramPacket msg) {
        int conv = ukcp.getConv();
        if (msg != null) {
            conv = getConv(msg);
            ukcp.setConv(conv);
        }

        ukcpMap.put(conv, ukcp);
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
