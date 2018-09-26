package kcp;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecEncode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by JinMiao
 * 2018/7/27.
 */
public class FecOutPut implements  KcpOutput{

    private KcpOutput output;

    private FecEncode fecEncode;

    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    public FecOutPut(KcpOutput output, FecEncode fecEncode) {
        this.output = output;
        this.fecEncode = fecEncode;
    }

    @Override
    public void out(ByteBuf msg, Kcp kcp) {
        //TODO 把fec头部分拷贝出来
        ByteBuf byteBuf = byteBufAllocator.ioBuffer(Fec.fecHeaderSizePlus2+msg.writerIndex());
        byteBuf.setBytes(Fec.fecHeaderSizePlus2,msg);
        byteBuf.writerIndex(Fec.fecHeaderSizePlus2+msg.writerIndex());
        msg.release();

        //byte[] test = new byte[byteBuf.writerIndex()];
        //byteBuf.getBytes(0,test);
        ByteBuf[] byteBufs = fecEncode.encode(byteBuf);
        output.out(byteBuf,kcp);
        if(byteBufs==null)
            return;
        for (int i = 0; i < byteBufs.length; i++) {
            ByteBuf parityByteBuf = byteBufs[i];
            output.out(parityByteBuf,kcp);
        }
    }
}
