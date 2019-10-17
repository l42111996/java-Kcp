package kcp;


import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2019/10/16.
 */
public class ClientAddressChannelManager implements IChannelManager {
    private Map<SocketAddress,Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(Channel channel, DatagramPacket msg) {
        return ukcpMap.get(msg.recipient());
    }

    @Override
    public void New(Channel channel, Ukcp ukcp) {
        ukcpMap.put(channel.localAddress(),ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.user().getLocalAddress());
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
