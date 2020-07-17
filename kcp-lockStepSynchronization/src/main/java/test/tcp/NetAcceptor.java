package test.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * 网络监听
 * @author King
 *
 */
public class NetAcceptor
{
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap b;
	private Channel channel;
	
	public NetAcceptor(TcpChannelInitializer tcpChannelInitializer,InetSocketAddress address)
	{
		this.b = new ServerBootstrap();
		boolean epoll = Epoll.isAvailable();

		// handler
		this.bossGroup = epoll?new EpollEventLoopGroup():new NioEventLoopGroup();
		this.workerGroup = epoll?new EpollEventLoopGroup():new NioEventLoopGroup();

		b.childOption(ChannelOption.TCP_NODELAY, true);
		b.childOption(ChannelOption.SO_SNDBUF, 2048);
		b.childOption(ChannelOption.SO_RCVBUF, 8096);
		b.childOption(ChannelOption.SO_KEEPALIVE, true);
		b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		Class<? extends ServerSocketChannel> serverSocketChannelClass = epoll?EpollServerSocketChannel.class:NioServerSocketChannel.class;
		
		b.group(bossGroup, workerGroup).channel(serverSocketChannelClass)
				.childHandler(tcpChannelInitializer);
		try {
			this.channel = b.bind(address).sync().channel();
		} catch (Exception e) {
			e.printStackTrace();
			 this.workerGroup.shutdownGracefully();
	         this.bossGroup.shutdownGracefully();
	         throw new RuntimeException("error",e);
		}
	}
	
	/**
	 * 关闭服务器用
	 */
	public void shutdown()
	{
		this.channel.close();
		this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
	}

	public Channel getChannel() {
		return channel;
	}

}
