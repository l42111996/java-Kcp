package test.TpsTest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.Ukcp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class TestRoom implements Runnable{
    private static final AtomicInteger roomIdInc = new AtomicInteger();

    private int roomId;

    public TestRoom() {
        this.roomId = roomIdInc.incrementAndGet();
    }

    private CopyOnWriteArrayList<Ukcp> ukcps = new CopyOnWriteArrayList<>();

    public int size(){
        return ukcps.size();
    }

    public int getRoomId() {
        return roomId;
    }


    @Override
    public void run() {
        int packIdCount = 0;
        for (Ukcp ukcp : ukcps) {
            TpsChannelServerCache tpsChannelServerCache = ukcp.user().getCache();
            packIdCount+=tpsChannelServerCache.size();
        }
        packIdCount= packIdCount*44;

        for (Ukcp ukcp : ukcps) {
            TpsChannelServerCache tpsChannelServerCache = ukcp.user().getCache();
            List<Integer> packIds = tpsChannelServerCache.getSendPackIds();
            int size = packIds.size();
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(packIdCount+4*size+2);
            byteBuf.writeShort(size);
            for (Integer packId : packIds) {
                byteBuf.writeInt(packId);
            }
            byteBuf.writeBytes(new byte[packIdCount]);

            ukcp.write(byteBuf);
            byteBuf.release();
        }
    }

    public CopyOnWriteArrayList<Ukcp> getUkcps() {
        return ukcps;
    }

    public void setUkcps(CopyOnWriteArrayList<Ukcp> ukcps) {
        this.ukcps = ukcps;
    }
}
