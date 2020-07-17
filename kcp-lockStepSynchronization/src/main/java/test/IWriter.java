package test;

import io.netty.buffer.ByteBuf;

/**
 * Created by JinMiao
 * 2020/7/17.
 */
public interface IWriter {
    void write(ByteBuf byteBuf);
}
