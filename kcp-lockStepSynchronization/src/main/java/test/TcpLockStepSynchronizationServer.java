package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import test.tcp.NetAcceptor;
import test.tcp.TcpChannelInitializer;

import java.net.InetSocketAddress;

/**
 * Created by JinMiao
 * 2020/7/17.
 */
@ChannelHandler.Sharable
public class TcpLockStepSynchronizationServer extends SimpleChannelInboundHandler<ByteBuf> {


    private static final RoomManager roomManager = new RoomManager();
    public static void main(String[] args) {
        TcpLockStepSynchronizationServer tcpLockStepSynchronizationServer = new TcpLockStepSynchronizationServer();
        TcpChannelInitializer tcpChannelInitializer = new TcpChannelInitializer(tcpLockStepSynchronizationServer);
        new NetAcceptor(tcpChannelInitializer,new InetSocketAddress(11009));

        tcpLockStepSynchronizationServer.roomManager.init();
    }

    public static final AttributeKey<Player> playerAttributeKey = AttributeKey.newInstance("player");


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("有连接进来"+ctx.channel().remoteAddress());
        Player player = new Player(byteBuf -> ctx.channel().writeAndFlush(byteBuf));
        Attribute<Player> playerAttribute = ctx.channel().attr(playerAttributeKey);
        playerAttribute.set(player);
        roomManager.joinRoom(player);
    }


    private Player getPlayer(ChannelHandlerContext ctx){
        Attribute<Player> playerAttribute = ctx.channel().attr(playerAttributeKey);
        return playerAttribute.get();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Player player = getPlayer(ctx);
        roomManager.remove(player.getId());
        System.out.println("连接断开了"+ctx.channel().remoteAddress());

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        Player player = getPlayer(channelHandlerContext);
        Room room = roomManager.getRoom(player.getId());
        ByteBuf byteBuf1 = ByteBufAllocator.DEFAULT.directBuffer(20);
        byteBuf.readBytes(byteBuf1);
        byteBuf1.readerIndex(0);
        byteBuf1.writerIndex(20);
        room.getiMessageExecutor().execute(() ->{
                    player.getMessages().add(byteBuf1);
                }
        );
    }
}
