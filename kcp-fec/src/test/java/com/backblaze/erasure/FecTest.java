package com.backblaze.erasure;

import com.backblaze.erasure.fec.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

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
public class FecTest {



    static PooledByteBufAllocator pooledByteBufAllocator = new PooledByteBufAllocator(true);
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


        testOOM();
        //encodeOOM();
        //new Thread(() -> runtask()).start();
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

    public static void encodeOOM(){
        int data = 10;
        int part = 3;
        ReedSolomon reedSolomon = ReedSolomon.create(data,part);
        FecEncode fecEncode = new FecEncode(0,reedSolomon,1500);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon,1500);
        while(true){
            List<ByteBuf> byteBufList = new ArrayList<>();
            List<ByteBuf> byteBufs = buildBytebuf(data);
            for (int i = 0; i < byteBufs.size(); i++) {
                ByteBuf[] encodeResult = fecEncode.encode(byteBufs.get(i));
                if(encodeResult!=null){
                    for (int i1 = 0; i1 < encodeResult.length; i1++) {
                        byteBufList.add(encodeResult[i1]);
                    }
                }
                byteBufList.add(byteBufs.get(i));
            }
            for (ByteBuf byteBuf : byteBufList) {
                byteBuf.release();
            }
        }
    }


    public static void decodeOOM(){

    }


    public static void testOOM(){
        int data = 10;
        int part = 3;

        ReedSolomon reedSolomon = ReedSolomon.create(data,part);
        FecEncode fecEncode = new FecEncode(0,reedSolomon,1500);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon,1500);

        ArrayBlockingQueue<ByteBuf> queue = new ArrayBlockingQueue<>(150);
        AtomicLong atomicLong = new AtomicLong();
        //生产
        new Thread(() -> {
            try {
                while(true){
                    List<ByteBuf> byteBufList = new ArrayList<>();
                    List<ByteBuf> byteBufs = buildBytebuf(data);
                    for (int i = 0; i < byteBufs.size(); i++) {
                        ByteBuf[] encodeResult = fecEncode.encode(byteBufs.get(i));
                        if(encodeResult!=null){
                            for (int i1 = 0; i1 < encodeResult.length; i1++) {
                                byteBufList.add(encodeResult[i1]);
                            }
                        }
                        byteBufList.add(byteBufs.get(i));
                    }

                    int drop = new Random().nextInt(data+part);
                            //new Random().nextInt(data+part);

                    //模拟丢数据
                    for (int i = 0; i < drop; i++) {
                        int dropIndex = new Random().nextInt(byteBufList.size());
                        byteBufList.get(dropIndex).release();
                        byteBufList.remove(dropIndex);
                    }
                    //打乱顺序
                    Collections.shuffle(byteBufList);
                    for (ByteBuf byteBuf : byteBufList) {
                        if(!queue.offer(byteBuf)){
                            byteBuf.release();
                        }
                    }
                }
            }catch (Throwable t){
                System.out.println(atomicLong.get());
                t.printStackTrace();
            }
        }).start();

        //消费
        new Thread(() -> {
            try {
                int i =0;
                while(true){
                    List<ByteBuf> byteBufs = new ArrayList<>();
                    while (true){
                        ByteBuf byteBuf = queue.poll();
                        if(byteBuf==null){
                            break;
                        }
                        byteBufs.add(byteBuf);
                    }
                    if(byteBufs.isEmpty())
                    {
                        continue;
                    }
                    try {
                        Thread.sleep(10);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    Collections.shuffle(byteBufs);

                    for (ByteBuf byteBuf : byteBufs) {
                        i++;

                        if(i%(100000)==0){
                            System.out.println(Snmp.snmp.toString());
                        }

                        FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                        atomicLong.set(fecPacket.getSeqid());
                        List<ByteBuf> dencodeResult = fecDecode.decode(fecPacket);
                        if(dencodeResult!=null){
                            for (ByteBuf buf : dencodeResult) {
                                //System.out.println("refa "+buf.refCnt());
                                buf.release();
                            }
                        }
                        //System.out.println("ref "+byteBuf.refCnt());
                        byteBuf.release();
                    }
                }
            }catch (Throwable t){
                System.out.println(atomicLong.get());
                t.printStackTrace();
            }
        }).start();
    }


    private static void runtask(){
        int data = 3;
        int part = 10;

        ReedSolomon reedSolomon = ReedSolomon.create(data,part);

        FecEncode fecEncode = new FecEncode(0,reedSolomon,1500);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon,1500);

        FecDecode fecDecode1 = new FecDecode((data+part)*3,reedSolomon,1500);
        int j = 0;
        while (true){
            j++;
                //System.out.println(j);
                List<ByteBuf> byteBufs = buildBytebuf(data);

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
                boolean canDecode = new Random().nextBoolean();
                int dropCount = 0;

                if(canDecode){
                    dropCount = new Random().nextInt(part);
                }else{
                    dropCount = part+1+(new Random().nextInt(data));
                }

                for (int i = 0; i < dropCount; i++) {
                    int dropIndex = new Random().nextInt(byteBufList.size());
                    byteBufList.get(dropIndex).release();
                    byteBufList.remove(dropIndex);
                }

                List<ByteBuf> dencodeResult = null;
                int dataSize = 0;
                for (ByteBuf byteBuf : byteBufList) {
                    FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                    if(fecPacket.getFlag()==Fec.typeData){
                        dataSize++;
                    }
                    dencodeResult = fecDecode.decode(fecPacket);
                    if(dencodeResult!=null)
                        break;
                }

            System.out.println(j +"  "+canDecode);
                if(dataSize!=data){
                    if(dencodeResult!=null&&!canDecode)
                    {
                        System.out.println("异常"+j);
                    }

                    if(dencodeResult==null&&canDecode)
                    {
                        for (ByteBuf byteBuf : byteBufList) {
                            byteBuf.readerIndex(0);
                            FecPacket fecPacket =  FecPacket.newFecPacket(byteBuf);
                            dencodeResult = fecDecode1.decode(fecPacket);
                        }
                        System.out.println("异常"+j);
                    }
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
            ByteBuf byteBuf = pooledByteBufAllocator.directBuffer(1500);
            byteBuf.writeBytes(new byte[Fec.fecHeaderSizePlus2]);
            //ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(Fec.mtuLimit);
            int size = new Random().nextInt(200)+Fec.fecHeaderSizePlus2+1;
            for (int i1 = 0; i1 < size; i1++) {
                byteBuf.writeByte(new Random().nextInt(127));
            }
            byteBufs.add(byteBuf);
        }

        return byteBufs;
    }
}
