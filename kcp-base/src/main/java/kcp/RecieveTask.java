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
        CodecOutputList<ByteBuf> bufList = null;
        try {
            //Thread.sleep(1000);
            //查看连接状态
            if(!kcp.isActive()){
                return;
            }
            boolean hasRevieveMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> recieveList = kcp.getRecieveList();
            for(;;) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                hasRevieveMessage = true;
                kcp.input(byteBuf,current);
                byteBuf.release();
            }
            if(!hasRevieveMessage){
                return;
            }
            bufList =  CodecOutputList.newInstance();
            while (kcp.canRecv()) {
                kcp.receive(bufList);
            }
            for (ByteBuf buf : bufList) {
                try {
                    kcp.getKcpListener().handleReceive(buf, kcp);
                }catch (Throwable throwable){
                    kcp.getKcpListener().handleException(throwable,kcp);
                }
                buf.release();
            }
            //判断写事件
            if(kcp.canSend(false)){
                kcp.notifyWriteEvent();
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            release();
            if(bufList!=null)
                bufList.recycle();
        }
    }


    public void release(){
        kcp = null;
        recyclerHandle.recycle(this);
    }

}
