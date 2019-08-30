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
public class RecieveTask implements ITask {

    private final Recycler.Handle<RecieveTask> recyclerHandle;

    private Ukcp kcp;

    private static final Recycler<RecieveTask> RECYCLER = new Recycler<RecieveTask>(2<<16) {
        @Override
        protected RecieveTask newObject(Handle<RecieveTask> handle) {
            return new RecieveTask(handle);
        }
    };

    private RecieveTask(Recycler.Handle<RecieveTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    public static RecieveTask newRecieveTask(Ukcp kcp) {
        RecieveTask recieveTask = RECYCLER.get();
        recieveTask.kcp = kcp;
        return recieveTask;
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
            boolean hasRevieveMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = kcp.getRecieveList();
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                hasRevieveMessage = true;
                kcp.input(byteBuf, current);
                byteBuf.release();
            }
            if (!hasRevieveMessage) {
                return;
            }
            if (kcp.isStream()) {
                while (kcp.canRecv()) {
                    if (bufList == null) {
                        bufList = CodecOutputList.newInstance();
                    }
                    kcp.receive(bufList);
                }
                int size = bufList.size();
                for (int i = 0; i < size; i++) {
                    ByteBuf byteBuf = bufList.getUnsafe(i);
                    readBytebuf(byteBuf);
                }
            } else {
                while (kcp.canRecv()) {
                    ByteBuf recvBuf = kcp.mergeReceive();
                    readBytebuf(recvBuf);
                }
            }
            //判断写事件
            if (!kcp.getSendList().isEmpty()&&kcp.canSend(false)) {
                kcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            release();
            if (bufList != null)
                bufList.recycle();
        }
    }


    private void readBytebuf(ByteBuf buf) {
        try {
            kcp.getKcpListener().handleReceive(buf, kcp);
        } catch (Throwable throwable) {
            kcp.getKcpListener().handleException(throwable, kcp);
        }
        buf.release();

    }

    public void release() {
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
