package kcp;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.util.Collection;


/**
 * Created by JinMiao
 * 2019/10/16.
 */
public interface IChannelManager {

    Ukcp get(Channel channel, DatagramPacket msg);

    void New(Channel channel, Ukcp ukcp);

    void del(Ukcp ukcp);

    Collection<Ukcp> getAll();
}
