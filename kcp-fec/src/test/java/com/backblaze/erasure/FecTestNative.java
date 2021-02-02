package com.backblaze.erasure;

import com.backblaze.erasure.fec.Fec;
import com.backblaze.erasure.fec.FecPacket;
import com.backblaze.erasure.fec.Snmp;
import com.backblaze.erasure.fecNative.FecDecode;
import com.backblaze.erasure.fecNative.FecEncode;
import com.backblaze.erasure.fecNative.ReedSolomonNative;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by JinMiao
 * 2018/6/21.
 */
public class FecTestNative {



    static ByteBufAllocator pooledByteBufAllocator = new UnpooledByteBufAllocator(false);
    public static void main(String[] args) {
        //ByteBuf byteBuf = pooledByteBufAllocator.directBuffer(10);
        //byteBuf.duplicate();
        //System.out.println(byteBuf.refCnt());
        //ByteBuf newByteBuf = byteBuf.copy();
        //System.out.println(newByteBuf.refCnt());
        //System.out.println(byteBuf.refCnt());
        //newByteBuf.release();
        //System.out.println(newByteBuf.refCnt());
        //System.out.println(byteBuf.refCnt());


        new Thread(() -> {
           while (true){
               try {
                   Thread.sleep(1000);
                   getDirectBufSize();
               } catch (Throwable e) {
                   e.printStackTrace();
               }
           }
        }).start();


        //testOOM();
        //encodeOOM();
        new Thread(() -> runtask()).start();
    }
    static  Field maxMemoryField =  null;
    static Field reservedMemoryField = null;

    public static void getDirectBufSize() throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        Class bitClass =null;
        if(maxMemoryField==null){
            bitClass= Class.forName("java.nio.Bits");
            maxMemoryField = bitClass.getDeclaredField("maxMemory");
            reservedMemoryField = bitClass.getDeclaredField("reservedMemory");
            maxMemoryField.setAccessible(true);
            reservedMemoryField.setAccessible(true);
        }
        System.out.println("maxDirectbuf: "+maxMemoryField.get(bitClass));

        System.out.println("usedDirectbuf: "+reservedMemoryField.get(bitClass));
    }




    private static void runtask(){
        int data = 10;
        int part = 3;
        int mtu = 1500;

        ReedSolomonNative reedSolomon = new ReedSolomonNative(data,part);

        FecEncode fecEncode = new FecEncode(0,reedSolomon,mtu);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon,mtu);

        FecDecode fecDecode1 = new FecDecode((data+part)*3,reedSolomon,mtu);
        int j = 0;
        while (true){
            j++;
                //System.out.println(j);
                List<ByteBuf> byteBufs = buildBytebuf(data,mtu);

                List<ByteBuf> byteBufList = new ArrayList<>();
                for (int i = 0; i < byteBufs.size(); i++) {
                    ByteBuf[] encodeResult = fecEncode.encode(byteBufs.get(i));
                    if(encodeResult!=null){
                        for (int i1 = 0; i1 < encodeResult.length; i1++) {
                            byteBufList.add(encodeResult[i1]);
                        }
                    }
                    byteBufList.add(byteBufs.get(i));
                }
                //是否能恢复
                boolean canDecode = random.nextBoolean();
                int dropCount = 0;

                if(canDecode){
                    dropCount = random.nextInt(part);
                }else{
                    dropCount = part+1+(random.nextInt(data));
                }
                //随机丢包
                randomDrop(byteBufList,dropCount);
                //乱序
                randomSort(byteBufList);



                List<ByteBuf> dencodeResult = null;
                int dataSize = 0;
                for (ByteBuf byteBuf : byteBufList) {
                    FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                    if(fecPacket.getFlag()== Fec.typeData){
                        dataSize++;
                    }
                    dencodeResult = fecDecode.decode(fecPacket);
                    if(dencodeResult!=null)
                        break;
                }

                if(dataSize!=data){
                   //如果能恢复
                    if(canDecode){

                    }else{
                        //不能恢复
                        if(dencodeResult!=null){
                            for (ByteBuf byteBuf : byteBufList) {
                                byteBuf.readerIndex(0);
                                FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                                dencodeResult = fecDecode1.decode(fecPacket);
                            }
                            System.out.println("异常"+j);
                        }


                    }



                    if(dencodeResult!=null&&!canDecode)
                    {
                        System.out.println("异常"+j);
                    }

                    if(dencodeResult==null&&canDecode)
                    {

                    }
                }else{
                    System.out.println();
                }
                for (ByteBuf byteBuf : byteBufList) {
                    byteBuf.release();
                }

                if(dencodeResult!=null){
                    for (ByteBuf byteBuf : dencodeResult) {
                        byteBuf.release();
                    }

                }
            }

            //fecEncode.release();
            //fecDecode.release();

    }


    static final Random random = new Random();

    private static void randomSort(List<ByteBuf> byteBufs){
        for (int i = 0; i < byteBufs.size(); i++) {
            int first = random.nextInt(byteBufs.size());
            int second = random.nextInt(byteBufs.size());
            ByteBuf temp = byteBufs.get(first);
            byteBufs.set(first,byteBufs.get(second));
            byteBufs.set(second,temp);
        }
    }
    private static void randomDrop(List<ByteBuf> byteBufs,int dropCount){
        for (int i = 0; i < dropCount; i++) {
            int dropIndex = random.nextInt(byteBufs.size());
            byteBufs.get(dropIndex).release();
            byteBufs.remove(dropIndex);
        }

    }



    private static List<ByteBuf> buildBytebuf(int dataShardCount,int mtu){
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
