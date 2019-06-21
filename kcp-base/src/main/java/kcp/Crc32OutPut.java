package kcp;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * crc32校验
 * Created by JinMiao
 * 2019-06-21.
 */
public class Crc32OutPut implements KcpOutput {
    private KcpOutput output;
    private CRC32 crc32 = new CRC32();
    private int headerOffset;

    protected Crc32OutPut(KcpOutput output,int headerOffset) {
        this.output = output;
        this.headerOffset = headerOffset;
    }
    @Override
    public void out(ByteBuf data, Kcp kcp) {
        ByteBuffer byteBuffer = data.nioBuffer(headerOffset+Ukcp.HEADER_CRC,data.readableBytes()-headerOffset-Ukcp.HEADER_CRC);
        crc32.reset();
        crc32.update(byteBuffer);
        long checksum = crc32.getValue();
        data.setInt(headerOffset, (int) checksum);
        output.out(data,kcp);
    }
}
