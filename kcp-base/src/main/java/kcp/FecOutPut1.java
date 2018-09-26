package kcp;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecEncode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Created by JinMiao
 * 2018/7/27.
 */
public class FecOutPut1 implements  KcpOutput{

    private KcpOutput output;

    private FecEncode fecEncode;

    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    protected FecOutPut1(KcpOutput output, FecEncode fecEncode) {
        this.output = output;
        this.fecEncode = fecEncode;
    }

    @Override
    public void out(ByteBuf msg, Kcp kcp) {
        ByteBuf byteBuf = byteBufAllocator.ioBuffer(Fec.fecHeaderSizePlus2);
        byteBuf.writerIndex(Fec.fecHeaderSizePlus2);
        ByteBuf newByteBuf = Unpooled.wrappedBuffer(byteBuf,msg);
        newByteBuf.writerIndex(Fec.fecHeaderSizePlus2+msg.writerIndex());
        ByteBuf[] byteBufs = fecEncode.encode(newByteBuf);
        //out之后会自动释放你内存
        output.out(newByteBuf,kcp);
        if(byteBufs==null)
            return;
        for (int i = 0; i < byteBufs.length; i++) {
            ByteBuf parityByteBuf = byteBufs[i];
            output.out(parityByteBuf,kcp);
        }
    }
}
