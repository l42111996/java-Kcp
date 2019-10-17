package kcp;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2019/10/17.
 */
public class ServerAddressChannelManager implements IChannelManager {
    private Map<SocketAddress,Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(Channel channel, DatagramPacket msg) {
        return ukcpMap.get(msg.sender());
    }

    @Override
    public void New(Channel channel, Ukcp ukcp) {
        ukcpMap.put(channel.localAddress(),ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.user().getRemoteAddress());
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
