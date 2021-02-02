package com.backblaze.erasure;

import com.backblaze.erasure.fec.FecPacket;
import com.backblaze.erasure.fecNative.FecDecode;
import com.backblaze.erasure.fecNative.FecEncode;
import com.backblaze.erasure.fecNative.ReedSolomonNative;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.List;
import java.util.Random;

/**
 * Created by JinMiao
 * 2020/12/25.
 */
public class BytebufBenchmarkNative {

    private static final int DATA_COUNT = 17;
    private static final int PARITY_COUNT = 3;
    private static final int TOTAL_COUNT = DATA_COUNT + PARITY_COUNT;
    private static final int BUFFER_SIZE = 200 * 1000;
    private static final int PROCESSOR_CACHE_SIZE = 10 * 1024 * 1024;
    private static final Random RANDOM = new Random();
    public static void main(String[] args) {
        ReedSolomonNative reedSolomon = new ReedSolomonNative(DATA_COUNT,PARITY_COUNT);
        FecDecode fecDecode = new FecDecode(TOTAL_COUNT*3,reedSolomon,BUFFER_SIZE);
        FecEncode fecEncode = new FecEncode(0,reedSolomon,BUFFER_SIZE);

        ByteBuf[] byteBufs = new ByteBuf[DATA_COUNT];
        for (int i = 0; i < DATA_COUNT; i++) {
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(BUFFER_SIZE);
            for (int i1 = 0; i1 < BUFFER_SIZE; i1++) {
                byteBuf.writeByte((byte) RANDOM.nextInt(256));
            }
            byteBufs[i] = byteBuf;
        }

        long start = System.currentTimeMillis();
        double size = 0;

        for(;;){
            long now = System.currentTimeMillis();
            if(now-start>=1000){

                System.out.println("时间"+(now-start)+"   "+((size)/1024.0/1024.0)+" MB");
                start=now;
                size=0;
            }
            size+=(BUFFER_SIZE*TOTAL_COUNT);
            ByteBuf[] byteBufs1 = null;
            for (ByteBuf byteBuf : byteBufs) {
                byteBufs1 = fecEncode.encode(byteBuf);
                if(byteBufs1!=null){
                    break;
                }
            }
            int dropIndex = RANDOM.nextInt(20)+1;


            for (ByteBuf byteBuf : byteBufs1) {
                if(dropIndex>1&&dropIndex<4){
                    byteBuf.release();
                    continue;
                }


                FecPacket fecPacket = FecPacket.newFecPacket(byteBuf);
                byteBuf.release();
                List<ByteBuf> byteBufList =  fecDecode.decode(fecPacket);
                if(byteBufList!=null&&byteBufList.size()!=0){
                    System.out.println();
                    for (ByteBuf buf : byteBufList) {
                        buf.release();
                    }
                }

            }
            //System.out.println(now-start);
        }






    }
}
