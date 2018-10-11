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

    private static final Recycler<RecieveTask> RECYCLER = new Recycler<RecieveTask>() {
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
        try {
            //查看连接状态
            if(!kcp.isActive()){
                release();
                return;
            }
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = kcp.getRecieveList();
            boolean hasRevieveMessage = false;
            for(;;) {
                try {
                    ByteBuf byteBuf = recieveList.poll();
                    if (byteBuf == null) {
                        break;
                    }
                    hasRevieveMessage = true;
                    kcp.input(byteBuf,current);
                } catch (Throwable throwable) {
                    kcp.getKcpListener().handleException(throwable, kcp);
                    release();
                    return;
                }
            }
            if(!hasRevieveMessage){
                release();
                return;
            }
            CodecOutputList<ByteBuf> bufList = CodecOutputList.newInstance();
            while (kcp.canRecv()) {
                kcp.receive(bufList);
            }
            for (ByteBuf buf : bufList) {
                kcp.getKcpListener().handleReceive(buf, kcp);
                buf.release();
            }
            bufList.recycle();
            //立刻刷新 并设置定时器的刷新时间
            kcp.setTsUpdate(-1);
            //kcp.update(System.currentTimeMillis());
            release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void release(){
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
