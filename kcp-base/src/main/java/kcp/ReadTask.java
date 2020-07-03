package kcp;

import internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import threadPool.task.ITask;

import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class ReadTask implements ITask {

    private final Recycler.Handle<ReadTask> recyclerHandle;

    private Ukcp kcp;

    private static final Recycler<ReadTask> RECYCLER = new Recycler<ReadTask>(2<<16) {
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
        readTask.kcp = kcp;
        return readTask;
    }


    @Override
    public void execute() {
        CodecOutputList<ByteBuf> bufList = null;
        try {
            //Thread.sleep(1000);
            //查看连接状态
            if (!kcp.isActive()) {
                return;
            }
            boolean hasKcpMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = kcp.getReadQueue();
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                hasKcpMessage = true;
                kcp.input(byteBuf, current);
                byteBuf.release();
            }
            if (!hasKcpMessage) {
                return;
            }
            if (kcp.isStream()) {
                int size =0;
                while (kcp.canRecv()) {
                    if (bufList == null) {
                        bufList = CodecOutputList.newInstance();
                    }
                    kcp.receive(bufList);
                    size= bufList.size();
                }
                for (int i = 0; i < size; i++) {
                    ByteBuf byteBuf = bufList.getUnsafe(i);
                    readBytebuf(byteBuf,current);
                }
            } else {
                while (kcp.canRecv()) {
                    ByteBuf recvBuf = kcp.mergeReceive();
                    readBytebuf(recvBuf,current);
                }
            }
            //判断写事件
            if (!kcp.getWriteQueue().isEmpty()&&kcp.canSend(false)) {
                kcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            kcp.close();
            e.printStackTrace();
        } finally {
            release();
            if (bufList != null) {
                bufList.recycle();
            }
        }
    }


    private void readBytebuf(ByteBuf buf,long current) {
        kcp.setLastRecieveTime(current);
        try {
            kcp.getKcpListener().handleReceive(buf, kcp);
        } catch (Throwable throwable) {
            kcp.getKcpListener().handleException(throwable, kcp);
        }finally {
            buf.release();
        }
    }

    public void release() {
        kcp.getReadProcessing().set(false);
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
