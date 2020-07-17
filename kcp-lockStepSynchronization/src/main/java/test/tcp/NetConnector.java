package test.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * 网络连接器
 * 
 * @author King
 * 
 */
public class NetConnector
{
	private EventLoopGroup group;
	private Bootstrap b;
	private Channel channel;
	
	
	public NetConnector(TcpChannelInitializer tcpChannelInitializer)
	{
		// netty connector初始化
		this.group = new NioEventLoopGroup();
		this.b = new Bootstrap();
		this.b.group(group).channel(NioSocketChannel.class).handler(tcpChannelInitializer);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_SNDBUF, 2048);
		b.option(ChannelOption.SO_RCVBUF, 8096);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
	}
	
	
	public Channel connect(InetSocketAddress... address) throws InterruptedException
	{
		for(InetSocketAddress addr:address)
		{
			ChannelFuture future = b.connect(addr);
			future.sync();
			return future.channel();
		}
		return null;
	}
	

	/**
	 * 关闭
	 */
	public void shutdown() {
		if(this.channel!=null)
			this.channel.close();
		if(this.group!=null)
			this.group.shutdownGracefully();
	}

	public Channel getChannel() {
		return channel;
	}
}
