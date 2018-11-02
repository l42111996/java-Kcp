package kcp;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecEncode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created by JinMiao
 * 2018/10/23.
 */
@Deprecated
public class FecChannelHandler extends MessageToMessageEncoder<DatagramPacket> {


    private FecEncode fecEncode;

    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    public void setFecEncode(FecEncode fecEncode) {
        this.fecEncode = fecEncode;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket datagramPacket, List<Object> out) {
        ByteBuf msg = datagramPacket.content();
        ByteBuf byteBuf = byteBufAllocator.ioBuffer(Fec.fecHeaderSizePlus2);
        byteBuf.writerIndex(Fec.fecHeaderSizePlus2);
        ByteBuf newByteBuf = Unpooled.wrappedBuffer(byteBuf, msg);
        newByteBuf.writerIndex(Fec.fecHeaderSizePlus2 + msg.writerIndex());
        ByteBuf[] byteBufs = fecEncode.encode(newByteBuf);
        //out之后会自动释放你内存
        out.add(newByteBuf);
        if (byteBufs == null)
            return;
        for (int i = 0; i < byteBufs.length; i++) {
            ByteBuf parityByteBuf = byteBufs[i];
            out.add(parityByteBuf);
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {




    }
}