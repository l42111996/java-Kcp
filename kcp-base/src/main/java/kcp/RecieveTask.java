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


    public static RecieveTask New(Ukcp kcp) {
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
            boolean hasKcpMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = kcp.getRecieveList();
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                //区分udp还是kcp消息
                if (kcp.getChannelConfig().KcpTag && byteBuf.readByte() == Ukcp.UDP_PROTOCOL) {
                    readBytebuf(byteBuf, current,Ukcp.UDP_PROTOCOL);
                }
                else{
                    hasKcpMessage = true;
                    kcp.input(byteBuf, current);
                    byteBuf.release();
                }
            }
            if (!hasKcpMessage) {
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
                    readBytebuf(byteBuf,current,Ukcp.KCP_PROTOCOL);
                }
            } else {
                while (kcp.canRecv()) {
                    ByteBuf recvBuf = kcp.mergeReceive();
                    readBytebuf(recvBuf,current,Ukcp.KCP_PROTOCOL);
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


    private void readBytebuf(ByteBuf buf,long current,int protocolType) {
        kcp.setLastRecieveTime(current);
        try {
            kcp.getKcpListener().handleReceive(buf, kcp,protocolType);
        } catch (Throwable throwable) {
            kcp.getKcpListener().handleException(throwable, kcp);
        }finally {
            buf.release();
        }
    }

    public void release() {
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
