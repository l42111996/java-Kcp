package kcp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import org.jctools.queues.MpscArrayQueue;
import threadPool.task.ITask;

import java.io.IOException;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class SendTask implements ITask {

    private final Recycler.Handle<SendTask> recyclerHandle;

    private Ukcp kcp;

    private static final Recycler<SendTask> RECYCLER = new Recycler<SendTask>() {
        @Override
        protected SendTask newObject(Handle<SendTask> handle) {
            return new SendTask(handle);
        }
    };


    private SendTask(Recycler.Handle<SendTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    static SendTask newSendTask(Ukcp kcp) {
        SendTask sendTask = RECYCLER.get();
        sendTask.kcp = kcp;
        return sendTask;
    }

    @Override
    public void execute() {
        try {
            //查看连接状态
            if(!kcp.isActive()){
                release();
                return;
            }
            //从发送缓冲区到kcp缓冲区
            MpscArrayQueue<ByteBuf> queue = kcp.getSendList();
            boolean hasSend  = false;
            while(kcp.canSend(false)){
                ByteBuf byteBuf = queue.poll();
                if(byteBuf==null){
                    break;
                }
                try {
                    hasSend = true;
                    this.kcp.send(byteBuf);
                } catch (IOException e) {
                    kcp.getKcpListener().handleException(e,kcp);
                    release();
                    return;
                }
            }
            //如果有发送 则检测时间
            if(hasSend){
                long now =System.currentTimeMillis();
                if(kcp.isFastFlush()){
                    kcp.update(now);
                }else{
                    kcp.setTsUpdate(-1);
                }
            }
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
