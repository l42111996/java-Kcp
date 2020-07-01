package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import threadPool.task.ITask;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class WriteTask implements ITask {

    private final Recycler.Handle<WriteTask> recyclerHandle;

    private Ukcp kcp;

    private static final Recycler<WriteTask> RECYCLER = new Recycler<WriteTask>(2<<16) {
        @Override
        protected WriteTask newObject(Handle<WriteTask> handle) {
            return new WriteTask(handle);
        }
    };


    private WriteTask(Recycler.Handle<WriteTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    static WriteTask New(Ukcp kcp) {
        WriteTask writeTask = RECYCLER.get();
        writeTask.kcp = kcp;
        return writeTask;
    }

    @Override
    public void execute() {
        try {
            //查看连接状态
            if(!kcp.isActive()){
                return;
            }
            //从发送缓冲区到kcp缓冲区
            Queue<ByteBuf> queue = kcp.getWriteQueue();
            while(kcp.canSend(false)){
                ByteBuf byteBuf = queue.poll();
                if(byteBuf==null){
                    break;
                }
                try {
                    this.kcp.send(byteBuf);
                    byteBuf.release();
                } catch (IOException e) {
                    kcp.getKcpListener().handleException(e,kcp);
                    return;
                }
            }
            //如果有发送 则检测时间
            if(!kcp.canSend(false)||(kcp.checkFlush()&&kcp.isFastFlush())){
                long now =System.currentTimeMillis();
                long next = kcp.flush(now);
                kcp.setTsUpdate(now+next);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            release();
        }
    }


    public void release(){
        kcp.getWriteProcessing().set(false);
        kcp = null;
        recyclerHandle.recycle(this);
    }


}
