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
    public static void main(String[] args) {
        new Thread(() -> runtask()).start();
    }


    private static void runtask(){
        int data = 3;
        int part = 10;

        ReedSolomon reedSolomon = ReedSolomon.create(data,part);

        FecEncode fecEncode = new FecEncode(0,reedSolomon);
        FecDecode fecDecode = new FecDecode((data+part)*3,reedSolomon);

        FecDecode fecDecode1 = new FecDecode((data+part)*3,reedSolomon);
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
            ByteBuf byteBuf = pooledByteBufAllocator.buffer(Fec.mtuLimit);
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
