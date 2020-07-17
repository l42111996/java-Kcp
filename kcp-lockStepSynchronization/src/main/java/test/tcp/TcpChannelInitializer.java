package test.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Created by JinMiao
 * 2020/7/17.
 */
public class TcpChannelInitializer extends ChannelInitializer<Channel> {

    private SimpleChannelInboundHandler simpleChannelInboundHandler;

    public TcpChannelInitializer(SimpleChannelInboundHandler simpleChannelInboundHandler) {
        this.simpleChannelInboundHandler = simpleChannelInboundHandler;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("frameDecoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4));
        pipeline.addLast("frameEncoder",new LengthFieldPrepender(4,false));
        pipeline.addLast("handler",simpleChannelInboundHandler);
    }
}
