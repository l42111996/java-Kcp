package com.backblaze.erasure.fec;

import com.backblaze.erasure.ReedSolomon;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by JinMiao
 * 2018/6/6.
 */
public class FecEncode {


    /**消息包长度**/
    private int dataShards;
    /**冗余包长度**/
    private int parityShards;
    /** dataShards+parityShards **/
    private int shardSize;
    //Protect Against Wrapped Sequence numbers
    private int paws;
    // next seqid
    private int next;
    //count the number of datashards collected
    private int shardCount;
    // record maximum data length in datashard
    private int maxSize;
    // FEC header offset
    private int headerOffset;
    // FEC payload offset
    private int payloadOffset;

    //private byte[][] shardCache;
    //private byte[][] encodeCache;

    //用完需要手动release
    private ByteBuf[] shardCache;
    private ByteBuf[] encodeCache;


    private ByteBuf zeros;

    private ReedSolomon codec;

    public FecEncode(int headerOffset, ReedSolomon codec) {
        this.dataShards = codec.getDataShardCount();
        this.parityShards = codec.getParityShardCount();
        this.shardSize = this.dataShards + this.parityShards;
        this.paws = (Integer.MAX_VALUE/shardSize - 1) * shardSize;
        this.headerOffset = headerOffset;
        this.payloadOffset = headerOffset + Fec.fecHeaderSize;
        this.codec =codec;

        this.shardCache = new ByteBuf[shardSize];
        this.encodeCache = new ByteBuf[parityShards];
        //for (int i = 0; i < shardCache.length; i++) {
        //    shardCache[i] = ByteBufAllocator.DEFAULT.buffer(Fec.mtuLimit);
        //}
        zeros = ByteBufAllocator.DEFAULT.buffer(Fec.mtuLimit);
        zeros.writeBytes(new byte[Fec.mtuLimit]);
    }


    /**
     *
     *  使用方法:
     *  1，入bytebuf后 把bytebuf发送出去,并释放bytebuf
     *  2，判断返回值是否为null，如果不为null发送出去并释放它
     *
     *  headerOffset +6字节fectHead +  2字节bodylenth(lenth-headerOffset-6)
     *
     * 1,对数据写入头标记为数据类型  markData
     * 2,写入消息长度
     * 3,获得缓存数据中最大长度，其他的缓存进行扩容到同样长度
     * 4,去掉头长度，进行fec编码
     * 5,对冗余字节数组进行标记为fec  makefec
     * 6,返回完整长度
     *
     *  注意: 传入的bytebuf如果需要释放在传入后手动释放。
     *  返回的bytebuf 也需要自己释放
     * @param byteBuf
     * @return
     */
    public ByteBuf[] encode(ByteBuf byteBuf){
        markData(byteBuf,headerOffset);
        int sz = byteBuf.writerIndex();
        byteBuf.setShort(payloadOffset,sz- Fec.fecHeaderSizePlus2);
        this.shardCache[shardCount] = byteBuf.retainedDuplicate();
        this.shardCount ++;
        if (sz > this.maxSize) {
            this.maxSize = sz;
        }
        if(shardCount!=dataShards) {
            return null;
        }
        //填充parityShards
        for (int i = 0; i < parityShards; i++) {
            ByteBuf parityByte = ByteBufAllocator.DEFAULT.buffer(this.maxSize);
            shardCache[i+dataShards]  = parityByte;
            encodeCache[i] = parityByte;
            markParity(parityByte,headerOffset);
            parityByte.writerIndex(Fec.fecHeaderSize);
        }

        //按着最大长度不足补充0
        for (int i = 0; i < this.shardSize; i++) {
            ByteBuf shard = shardCache[i];
            int left = this.maxSize-shard.writerIndex();
            if(left<=0)
                continue;
            //是否需要扩容  会出现吗？？
            //if(shard.capacity()<this.maxSize){
            //    ByteBuf newByteBuf = ByteBufAllocator.DEFAULT.buffer(this.maxSize);
            //    newByteBuf.writeBytes(shard);
            //    shard.release();
            //    shard = newByteBuf;
            //    shardCache[i] = shard;
            //}
            shard.writeBytes(zeros,left);
            zeros.readerIndex(0);
        }
        codec.encodeParity(shardCache,payloadOffset,this.maxSize-payloadOffset);
        //释放dataShards
        for (int i = 0; i < dataShards; i++) {
            this.shardCache[i].release();
        }
        this.shardCount = 0;
        this.maxSize = 0;
        return this.encodeCache;
    }



    public void release(){

        System.out.println("releaseed");
        this.dataShards=0;
        this.parityShards=0;
        this.shardSize=0;
        this.paws=0;
        this.next=0;
        this.shardCount=0;
        this.maxSize=0;
        this.headerOffset=0;
        this.payloadOffset=0;
        ByteBuf byteBuf = null;
        for (int i = 0; i < dataShards; i++) {
            byteBuf = this.shardCache[i];
            if(byteBuf!=null)
                byteBuf.release();
        }
        zeros.release();
        codec=null;
    }

    public static void main(String[] args) {
        //ReedSolomon codec = new ReedSolomon(10,3,null);
        //FecEncode fecEncoder = new FecEncode(2,codec);
        //for (int i = 0; i < 10; i++) {
        //    ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(10000);
        //    byteBuf.writeInt(i);
        //    if(i>4){
        //        for (int i1 = i; i1 < 10; i1++) {
        //            byteBuf.writeInt(i);
        //        }
        //    }
        //    ByteBuf[] byteBufs = fecEncoder.encode(byteBuf);
        //    if(byteBufs!=null){
        //        System.out.println();
        //    }
        //}
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(10);
        //byteBuf.capacity(10000);
        //byteBuf.writeInt(55);

        ByteBuf b1 = byteBuf.retainedDuplicate();


        ByteBuf writeBuf = ByteBufAllocator.DEFAULT.buffer(1000);
        writeBuf.writeByte(22);

        //copy 拷贝  全部不相同  cap不相同
        //duplicate  引用  值相同  readerindex writerindex不相同 cap相同
        //slice  引用 值相同 readerindex writerindex不相同  cap不同
        //retained  原引用计encoen数+1


        ByteBuf nByteBuf = byteBuf.copy();




        nByteBuf.setInt(0,11);
        System.out.println(byteBuf.getInt(0));
        System.out.println(nByteBuf.getInt(0));

        //byteBuf.writeByte(55);

        //System.out.println(byteBuf.writerIndex());
        //System.out.println(nByteBuf.writerIndex());


        //
        //System.out.println(byteBuf.writerIndex());
        //System.out.println(nByteBuf.writerIndex());
        System.out.println(byteBuf.capacity());
        System.out.println(nByteBuf.capacity());


        //nByteBuf.release();
        System.out.println(byteBuf.refCnt());
        System.out.println(nByteBuf.refCnt());


        System.out.println(writeBuf.readerIndex());
        //writeBuf.readByte();
        byteBuf.writeBytes(writeBuf,1);
        //byteBuf.setBytes(0,writeBuf,1);

        //byteBuf.writeBytes(writeBuf);
        System.out.println(byteBuf.readInt());
        System.out.println(byteBuf.readByte());
        //System.out.println(writeBuf.writerIndex());
    }


    public void markData(ByteBuf byteBuf,int offset){
        byteBuf.setInt(offset, this.next);
        byteBuf.setShort(offset+4, Fec.typeData);
        this.next++;
    }

    public void markParity(ByteBuf byteBuf, int offset){
        byteBuf.setInt(offset, this.next);
        byteBuf.setShort(offset+4,Fec.typeParity);
        this.next = (this.next + 1) % this.paws;
    }
}
