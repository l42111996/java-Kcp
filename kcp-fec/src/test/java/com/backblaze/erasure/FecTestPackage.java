package com.backblaze.erasure;

import io.netty.buffer.ByteBuf;

/**
 * Created by JinMiao
 * 2020/12/25.
 */
public class FecTestPackage {
    private ByteBuf byteBuf;
    private boolean drop = false;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public FecTestPackage(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public void setByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public boolean isDrop() {
        return drop;
    }

    public void setDrop(boolean drop) {
        this.drop = drop;
    }

    @Override
    public String toString() {
        return "FecTestPackage{" +
                ", drop=" + drop +
                ", id=" + id +
                '}';
    }
}
