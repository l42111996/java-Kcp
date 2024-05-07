package com.backblaze.erasure;

import com.backblaze.erasure.fec.*;
import com.backblaze.erasure.fecNative.FecDecode;
import com.backblaze.erasure.fecNative.FecEncode;
import com.backblaze.erasure.fecNative.*;
import io.netty.buffer.*;

import java.util.*;

public class FecCorrect
{
    static ByteBufAllocator pooledByteBufAllocator = new UnpooledByteBufAllocator(false);

    static final Random random = new Random();

    public static void main(String[] args) {
        int data = 10;
        int part = 3;
        int mtu = 1500;
        ReedSolomonNative reedSolomon = new ReedSolomonNative(data,part);
        FecEncode fecEncode = new FecEncode(0,reedSolomon,mtu);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon,mtu);


        ReedSolomon codec = ReedSolomon.create(data, part);
        com.backblaze.erasure.fec.FecEncode fecEncodeJava = new com.backblaze.erasure.fec.FecEncode(0,codec,mtu);
        com.backblaze.erasure.fec.FecDecode fecDecodeJava = new com.backblaze.erasure.fec.FecDecode((data+part)*3,codec,mtu);

        for(;;){
            List<ByteBuf> byteBufs = buildBytebuf(data,mtu);
            List<ByteBuf> nativeEncodeBytes = new ArrayList<>();
            for (int i = 0; i < byteBufs.size(); i++) {
                ByteBuf[] encodeResult = fecEncode.encode(byteBufs.get(i));
                if(encodeResult!=null){
                    for (int i1 = 0; i1 < encodeResult.length; i1++) {
                        nativeEncodeBytes.add(encodeResult[i1]);
                    }
                }
                nativeEncodeBytes.add(byteBufs.get(i));
            }
            List<ByteBuf> javaEncodeBytes = new ArrayList<>();
            for (int i = 0; i < byteBufs.size(); i++) {
                ByteBuf[] encodeResult = fecEncodeJava.encode(byteBufs.get(i));
                if(encodeResult!=null){
                    for (int i1 = 0; i1 < encodeResult.length; i1++) {
                        javaEncodeBytes.add(encodeResult[i1]);
                    }
                }
                javaEncodeBytes.add(byteBufs.get(i));
            }


            //是否能恢复
            boolean canDecode = random.nextBoolean();
            int dropCount = 0;

            if(true){
                dropCount = random.nextInt(part);
            }else{
                dropCount = part+1+(random.nextInt(data));
            }
            //随机丢包
            randomDrop(dropCount,nativeEncodeBytes,javaEncodeBytes);
            //乱序
            randomSort(nativeEncodeBytes,javaEncodeBytes);

            compare(nativeEncodeBytes,javaEncodeBytes);


            //恢复

            List<ByteBuf> nativeDecodeBytes = null;
            List<ByteBuf> javaDecodeBytes = null;

            for (ByteBuf nativeEncodeByte : nativeEncodeBytes) {
                nativeEncodeByte.readerIndex(0);
                FecPacket fecPacket =  FecPacket.newFecPacket(nativeEncodeByte);
                nativeDecodeBytes = fecDecode.decode(fecPacket);
                if(nativeDecodeBytes!=null){
                    break;
                }
            }
            for (ByteBuf byteBuf : javaEncodeBytes) {
                byteBuf.readerIndex(0);
                FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                javaDecodeBytes = fecDecodeJava.decode(fecPacket);
                if(javaDecodeBytes!=null){
                    break;
                }
            }
            if(nativeDecodeBytes!=null&&javaDecodeBytes!=null){
                compare(nativeDecodeBytes,javaDecodeBytes);
            }

            if((nativeDecodeBytes!=null&&javaDecodeBytes==null)||(nativeDecodeBytes==null&&javaDecodeBytes!=null)){
                System.out.println();
            }







            //释放资源
            for (ByteBuf nativeEncodeByte : nativeEncodeBytes) {
                nativeEncodeByte.release();
            }
            for (ByteBuf javaEncodeByte : javaEncodeBytes) {
                if(javaEncodeByte.refCnt()>0){
                    javaEncodeByte.release();
                }
            }
            if (nativeDecodeBytes != null) {
                for (ByteBuf nativeDecodeByte : nativeDecodeBytes) {
                    if(nativeDecodeByte.refCnt()>0){
                        nativeDecodeByte.release();
                    }
                }
                for (ByteBuf javaDecodeByte : javaDecodeBytes) {
                    if(javaDecodeByte.refCnt()>0){
                        javaDecodeByte.release();
                    }
                }
            }
        }

    }


    private static void compare(List<ByteBuf> nativeEncodeBytes,List<ByteBuf> javaEncodeBytes){
        if(nativeEncodeBytes.size()!=javaEncodeBytes.size()){
            System.out.println("异常111");
        }
        for (int i = 0; i < nativeEncodeBytes.size(); i++) {
            ByteBuf nativeEncodeByte = nativeEncodeBytes.get(i);
            ByteBuf javaEncodeByte = javaEncodeBytes.get(i);
            if(nativeEncodeByte.readableBytes()!=javaEncodeByte.readableBytes()){
                System.out.println("异常222   "+nativeEncodeByte.readableBytes()+"   "+javaEncodeByte.readableBytes());
            }


//            System.out.println("nativeEncodeByte.readableBytes()=" + nativeEncodeByte.readableBytes());
//            System.out.println("javaEncodeByte.readableBytes()=" + javaEncodeByte.readableBytes());
            for (int i1 = 0; i1 < javaEncodeByte.readableBytes(); i1++) {
                if(nativeEncodeByte.getByte(i1)!=javaEncodeByte.getByte(i1)){
                    System.out.println();
                }
            }
        }
    }


    private static void randomSort(List<ByteBuf>... byteBufs){
        List<ByteBuf> temp = byteBufs[0];
        for (int i = 0; i < temp.size(); i++){
            int first = random.nextInt(temp.size());
            int second = random.nextInt(temp.size());
            for (List<ByteBuf> byteBuf : byteBufs) {
                ByteBuf byteBuf1 = byteBuf.get(first);
                byteBuf.set(first,byteBuf.get(second));
                byteBuf.set(second,byteBuf1);
            }
        }
    }
    private static void randomDrop(int dropCount,List<ByteBuf>... byteBufs){
        List<ByteBuf> first = byteBufs[0];
        for (int i = 0; i < dropCount; i++) {
            int dropIndex = random.nextInt(first.size());
            for (List<ByteBuf> byteBuf : byteBufs) {
                if(byteBuf.get(dropIndex).refCnt()>0){
                    byteBuf.get(dropIndex).release();
                }
                byteBuf.remove(dropIndex);

            }
        }
    }


    private static List<ByteBuf> buildBytebuf(int dataShardCount, int mtu){
        List<ByteBuf> byteBufs = new ArrayList<>();
        for (int i = 0; i < dataShardCount; i++) {
            ByteBuf byteBuf = pooledByteBufAllocator.directBuffer(mtu);
            byteBuf.writeBytes(new byte[Fec.fecHeaderSizePlus2]);
            //ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(Fec.mtuLimit);
            int size = random.nextInt(mtu-Fec.fecHeaderSizePlus2)+1;
            for (int i1 = 0; i1 < size; i1++) {
                byteBuf.writeByte(random.nextInt(127));
            }
            byteBufs.add(byteBuf);
        }

        return byteBufs;
    }

}
