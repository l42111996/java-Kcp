package kcp;

import internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import threadPool.ITask;

import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class ReadTask implements ITask {

    private final Recycler.Handle<ReadTask> recyclerHandle;

    private Ukcp ukcp;

    private static final Recycler<ReadTask> RECYCLER = new Recycler<ReadTask>() {
        @Override
        protected ReadTask newObject(Handle<ReadTask> handle) {
            return new ReadTask(handle);
        }
    };

    private ReadTask(Recycler.Handle<ReadTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    protected static ReadTask New(Ukcp kcp) {
        ReadTask readTask = RECYCLER.get();
        readTask.ukcp = kcp;
        return readTask;
    }


    @Override
    public void execute() {
        CodecOutputList<ByteBuf> bufList = null;
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态
            if (!ukcp.isActive()) {
                return;
            }
            boolean hasKcpMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = ukcp.getReadBuffer();
            int readCount =0;
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                readCount++;
                hasKcpMessage = true;
                ukcp.input(byteBuf, current);
                byteBuf.release();
            }
            if (!hasKcpMessage) {
                return;
            }
            ukcp.getReadBufferIncr().addAndGet(readCount);
            if (ukcp.isStream()) {
                int size =0;
                while (ukcp.canRecv()) {
                    if (bufList == null) {
                        bufList = CodecOutputList.newInstance();
                    }
                    ukcp.receive(bufList);
                    size= bufList.size();
                }
                for (int i = 0; i < size; i++) {
                    ByteBuf byteBuf = bufList.getUnsafe(i);
                    readBytebuf(byteBuf,current,ukcp);
                }
            } else {
                while (ukcp.canRecv()) {
                    ByteBuf recvBuf = ukcp.mergeReceive();
                    readBytebuf(recvBuf,current,ukcp);
                }
            }
            //判断写事件
            if (!ukcp.getWriteBuffer().isEmpty()&& ukcp.canSend(false)) {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            ukcp.internalClose();
            e.printStackTrace();
        } finally {
            release();
            if (bufList != null) {
                bufList.recycle();
            }
        }
    }


    private void readBytebuf(ByteBuf buf,long current,Ukcp ukcp) {
        ukcp.setLastRecieveTime(current);
        try {
            ukcp.getKcpListener().handleReceive(buf, ukcp);
        } catch (Throwable throwable) {
            ukcp.getKcpListener().handleException(throwable, ukcp);
        }finally {
            buf.release();
        }
    }

    public void release() {
        ukcp.getReadProcessing().set(false);
        ukcp = null;
        recyclerHandle.recycle(this);
    }

}
