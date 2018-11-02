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
                    return;
                }
            }
            if(!hasRevieveMessage){
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
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //判断写事件
            if(kcp.canSend(false)){
                kcp.notifyWriteEvent();
            }
            release();
        }
    }


    public void release(){
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
