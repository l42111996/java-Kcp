package test;

import io.netty.buffer.ByteBuf;

/**
 * Created by JinMiao
 * 2019-01-04.
 */
public class DelayPacket {
    private long ts;
    private ByteBuf ptr;


    public void init(ByteBuf src) {
        this.ptr = src.retainedSlice();

    }


    public void release(){
        ptr.release();
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public ByteBuf getPtr() {
        return ptr;
    }

    public void setPtr(ByteBuf ptr) {
        this.ptr = ptr;
    }
}
