package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import threadPool.ITask;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class WriteTask implements ITask {

    private final Recycler.Handle<WriteTask> recyclerHandle;

    private Ukcp ukcp;

    private static final Recycler<WriteTask> RECYCLER = new Recycler<WriteTask>() {
        @Override
        protected WriteTask newObject(Handle<WriteTask> handle) {
            return new WriteTask(handle);
        }
    };


    private WriteTask(Recycler.Handle<WriteTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    static WriteTask New(Ukcp ukcp) {
        WriteTask writeTask = RECYCLER.get();
        writeTask.ukcp = ukcp;
        return writeTask;
    }

    @Override
    public void execute() {
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态
            if(!ukcp.isActive()){
                return;
            }
            //从发送缓冲区到kcp缓冲区
            Queue<ByteBuf> queue = ukcp.getWriteQueue();
            while(ukcp.canSend(false)){
                ByteBuf byteBuf = queue.poll();
                if(byteBuf==null){
                    break;
                }
                try {
                    ukcp.send(byteBuf);
                    byteBuf.release();
                } catch (IOException e) {
                    ukcp.getKcpListener().handleException(e, ukcp);
                    return;
                }
            }
            //如果有发送 则检测时间
            if(!ukcp.canSend(false)||(ukcp.checkFlush()&& ukcp.isFastFlush())){
                long now =System.currentTimeMillis();
                long next = ukcp.flush(now);
                ukcp.setTsUpdate(now+next);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            release();
        }
    }


    public void release(){
        ukcp.getWriteProcessing().set(false);
        ukcp = null;
        recyclerHandle.recycle(this);
    }


}
