package com.backblaze.erasure;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecDecode;
import com.backblaze.erasure.fec.FecEncode;
import com.backblaze.erasure.fec.FecPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by JinMiao
 * 2018/6/21.
 */
public class FecTest {



    static PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator(true);
    static   ReedSolomon reedSolomon = ReedSolomon.create(2,1);
    public static void main(String[] args) {


        new Thread(() -> runtask()).start();

        new Thread(() -> runtask()).start();


        new Thread(() -> runtask()).start();
        //System.out.println();

        //long start =System.currentTimeMillis();
        //int j = 0;
        //int count = 0;
        //List<ByteBuf> byteBufs = buildBytebuf(2);
        //try {
        //    while (true){
        //        count++;
        //        //long now = System.currentTimeMillis();
        //        //if(now-start>1000){
        //        //    System.out.println(j/1024/1024);
        //        //    System.out.println("count:  "+count);
        //        //    j=0;
        //        //    start = now;
        //        //    count = 0;
        //        //}
        //
        //
        //
        //
        //
        //        ByteBuf[] encodeBytes = null;
        //        ByteBuf[] result = null;
        //        for (ByteBuf byteBuf : byteBufs) {
        //            j +=byteBuf.readableBytes();
        //            result = fecEncode.encode(byteBuf);
        //            //byteBuf.release();
        //        }
        //
        //
        //        List<byte[]> encodeList = new ArrayList<>();
        //        for (ByteBuf byteBuf : result) {
        //            byte[] bytes = new byte[byteBuf.writerIndex()];
        //            byteBuf.getBytes(0,bytes);
        //            encodeList.add(bytes);
        //        }
        //
        //
        //        List<ByteBuf> decodeList  = null;
        //        for (int i = 0; i < result.length; i++) {
        //            ByteBuf byteBuf = result[i];
        //            if(i==1){
        //                continue;
        //            }
        //           FecPacket fecPacket =  fecDecode.newFecPacket(byteBuf);
        //            decodeList = fecDecode.decode(fecPacket);
        //        }
        //
        //        //ByteBuf old = byteBufs.get(1);
        //        //ByteBuf news = decodeList.get(0);
        //        //old.skipBytes(Fec.fecHeaderSize);
        //        //news.skipBytes(Fec.fecHeaderSize);
        //        //
        //        //int avable = old.readableBytes();
        //
        //        //for (int i = Fec.fecHeaderSize-1; i < avable; i++) {
        //        //    byte a = old.readByte();
        //        //    byte b = decodeList.get(0).readByte();
        //        //    if(a!= b){
        //        //        System.out.println("index "+i);
        //        //    }
        //        //}
        //        for (ByteBuf byteBuf : result) {
        //            byteBuf.release();
        //
        //        }
        //
        //        for (ByteBuf byteBuf : decodeList) {
        //            byteBuf.release();
        //        }
        //        for (ByteBuf byteBuf : result) {
        //            //System.out.println(byteBuf.refCnt());
        //        }
        //        //System.out.println("次数");
        //        //Thread.sleep(1);
        //    }
        //}catch (Throwable e){
        //    e.printStackTrace();
        //}
    }


    private static void runtask(){
        int lostIndex = 1;
        while (true){
            FecEncode fecEncode = new FecEncode(0,reedSolomon);
            FecDecode fecDecode = new FecDecode(3*3,reedSolomon);
            for (int k =0 ;k<10;k++) {

                //System.out.println(j);
                List<ByteBuf> byteBufs = buildBytebuf(2);

                List<byte[]> arrayList = new ArrayList<>();
                for (ByteBuf byteBuf : byteBufs) {
                    byte[] bytes = new byte[byteBuf.writerIndex()];
                    byteBuf.getBytes(0,bytes);
                    for (byte aByte : bytes) {
                        //System.out.print(aByte);
                    }
                    //System.out.println();
                    arrayList.add(bytes);
                }

                ByteBuf[] result = new ByteBuf[byteBufs.size()+1];
                for (int i = 0; i < byteBufs.size(); i++) {
                    ByteBuf[] encodeResult = fecEncode.encode(byteBufs.get(i));
                    if(encodeResult!=null){
                        result[i+1] = encodeResult[0];
                    }
                    result[i] = byteBufs.get(i);
                }


                List<byte[]> encodeList = new ArrayList<>();
                for (ByteBuf byteBuf : result) {
                    byte[] bytes = new byte[byteBuf.writerIndex()];
                    byteBuf.getBytes(0,bytes);
                    for (byte aByte : bytes) {
                        //System.out.print(aByte);
                    }
                    //System.out.println();
                    encodeList.add(bytes);
                }
                List<ByteBuf> dencodeResult = new ArrayList<>();
                for (int i = 0; i < result.length; i++) {
                    ByteBuf byteBuf = result[i];
                    if(i==lostIndex)
                        continue;
                    FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                    dencodeResult = fecDecode.decode(fecPacket);
                }

                for (ByteBuf byteBuf : result) {
                    byteBuf.release();
                }

                List<byte[]> decodeList = new ArrayList<>();
                for (ByteBuf byteBuf : dencodeResult) {
                    byte[] bytes = new byte[byteBuf.writerIndex()];
                    byteBuf.getBytes(0,bytes);
                    decodeList.add(bytes);
                }

                for (ByteBuf byteBuf : dencodeResult) {
                    byteBuf.release();
                }


                //for (int i = 0; i < decodeList.size(); i++) {
                //    byte[] encodeBytes = encodeList.get(lostIndex);
                //    for (int i1 = 7; i1 < encodeBytes.length; i1++) {
                //        if(encodeBytes[i1]!=decodeList.get(i)[i1]){
                //            System.out.println();
                //        }
                //    }
                //}
            }

            fecEncode.release();
            fecDecode.release();
        }

    }



    private void ranSort(ByteBuf[] encodeBytes){
        for (int i = 0; i < encodeBytes.length; i++) {
            int first = new Random().nextInt(encodeBytes.length);
            int second = new Random().nextInt(encodeBytes.length);
            ByteBuf temp = encodeBytes[first];
            encodeBytes[first] = encodeBytes[second];
            encodeBytes[second] = temp;
        }
    }
    private void ranRemove(ByteBuf[] encodeBytes){
        int index = new Random().nextInt(encodeBytes.length);
        for (int i = 0; i < encodeBytes.length; i++) {
            if(i==index)
                continue;





        }


    }



    private static List<ByteBuf> buildBytebuf(int dataShardCount){
        List<ByteBuf> byteBufs = new ArrayList<>();
        for (int i = 0; i < dataShardCount; i++) {
            ByteBuf byteBuf = pooledByteBufAllocator.buffer(Fec.mtuLimit);
            byteBuf.writeBytes(new byte[Fec.fecHeaderSizePlus2]);
            //ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(Fec.mtuLimit);
            int size = new Random().nextInt(10)+Fec.fecHeaderSizePlus2+1;
            for (int i1 = 0; i1 < size; i1++) {
                byteBuf.writeByte(new Random().nextInt(127));
            }
            byteBufs.add(byteBuf);
        }

        return byteBufs;
    }
}
