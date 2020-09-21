package internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by JinMiao
 * 2020/7/20.
 */
public class ByteBufUtils {

    public static final void writeByte(ByteBuf byteBuf , int value){
        byteBuf.writeByte(value);
    }

    public static final void writeShort(ByteBuf byteBuf , int value){
        writeInt(byteBuf,value);
    }


    public static final void writeInt(ByteBuf byteBuf , int value){

        if (value >= 0) {
            while (true) {
                if ((value & ~0x7F) == 0) {
                    writeByte(byteBuf,(byte) value);
                    return;
                } else {
                    writeByte(byteBuf,(byte) ((value & 0x7F) | 0x80));
                    value >>>= 7;
                }
            }
        } else {
            // Must sign-extend.
            //writeUInt64NoTag(value);
        }



    }


    public static final void writeLong(ByteBuf byteBuf , long value){
        while (true) {
            if ((value & ~0x7FL) == 0) {
                byteBuf.writeByte((byte) value);
                return;
            } else {
                byteBuf.writeByte((byte) (((int) value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }


    public static final byte readByte(ByteBuf byteBuf){
        return byteBuf.readByte();
    }

    public static final int readShort(ByteBuf byteBuf){
        return readInt(byteBuf);
    }

    public static final int readInt(ByteBuf byteBuf){
        fastpath:
        {
            int tempPos = byteBuf.readerIndex();
            int bufferSize = byteBuf.readableBytes();

            if (bufferSize == tempPos) {
                break fastpath;
            }

            int x;
            if ((x = byteBuf.getByte(tempPos++)) >= 0) {
                byteBuf.readerIndex(tempPos);
                return x;
            } else if (bufferSize - tempPos < 9) {
                break fastpath;
            } else if ((x ^= (byteBuf.getByte(tempPos++) << 7)) < 0) {
                x ^= (~0 << 7);
            } else if ((x ^= (byteBuf.getByte(tempPos++) << 14)) >= 0) {
                x ^= (~0 << 7) ^ (~0 << 14);
            } else if ((x ^= (byteBuf.getByte(tempPos++) << 21)) < 0) {
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
            } else {
                int y = byteBuf.getByte(tempPos++);
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                if (y < 0
                        && byteBuf.getByte(tempPos++) < 0
                        && byteBuf.getByte(tempPos++) < 0
                        && byteBuf.getByte(tempPos++) < 0
                        && byteBuf.getByte(tempPos++) < 0
                        && byteBuf.getByte(tempPos++) < 0) {
                    break fastpath; // Will throw malformedVarint()
                }
            }
            byteBuf.readerIndex(tempPos);
            return x;
        }
        return (int) readRawVarint64SlowPath(byteBuf);
    }

    public static final long readLong(ByteBuf byteBuf){
        fastpath:
        {
            int tempPos = byteBuf.readerIndex();
            int bufferSize = byteBuf.readableBytes();

            if (bufferSize == tempPos) {
                break fastpath;
            }

            long x;
            int y;
            if ((y = byteBuf.getByte(tempPos++)) >= 0) {
                byteBuf.readerIndex(tempPos);
                return y;
            } else if (bufferSize - tempPos < 9) {
                break fastpath;
            } else if ((y ^= (byteBuf.getByte(tempPos++) << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (byteBuf.getByte(tempPos++) << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (byteBuf.getByte(tempPos++) << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) byteBuf.getByte(tempPos++) << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) byteBuf.getByte(tempPos++) << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) byteBuf.getByte(tempPos++) << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) byteBuf.getByte(tempPos++) << 49)) < 0L) {
                x ^=
                        (~0L << 7)
                                ^ (~0L << 14)
                                ^ (~0L << 21)
                                ^ (~0L << 28)
                                ^ (~0L << 35)
                                ^ (~0L << 42)
                                ^ (~0L << 49);
            } else {
                x ^= ((long) byteBuf.getByte(tempPos++) << 56);
                x ^=
                        (~0L << 7)
                                ^ (~0L << 14)
                                ^ (~0L << 21)
                                ^ (~0L << 28)
                                ^ (~0L << 35)
                                ^ (~0L << 42)
                                ^ (~0L << 49)
                                ^ (~0L << 56);
                if (x < 0L) {
                    if (byteBuf.getByte(tempPos++) < 0L) {
                        break fastpath; // Will throw malformedVarint()
                    }
                }
            }
            byteBuf.readerIndex(tempPos);
            return x;
        }
        return readRawVarint64SlowPath(byteBuf);
    }

    private static long readRawVarint64SlowPath(ByteBuf byteBuf) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = readByte(byteBuf);
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new RuntimeException("parse int64 error");
    }


    public static final long readUInt(ByteBuf byteBuf){
        return byteBuf.readInt()&  0xFFFFFFFFL;
    }

    public static final int readUShort(ByteBuf byteBuf) {
        return readShort(byteBuf) & 0xFFFF;
    }

    public static final short readUByte(ByteBuf byteBuf){
        return (short) (byteBuf.readByte()&  0xFF);
    }


    public static void main(String[] args) {
        long a = ((long)Integer.MAX_VALUE)+1L;
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(100);
        //byteBuf.writeInt((int) a);

        writeInt(byteBuf, (int) a);
        long b =
        // byteBuf.readUnsignedInt();
                readUInt(byteBuf);
        System.out.println(b);

        System.out.println(b==a);

    }
}
