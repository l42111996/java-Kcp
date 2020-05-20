package threadPool.thread;

import io.netty.util.internal.PlatformDependent;
import threadPool.task.ITask;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by JinMiao
 * 2020/5/20.
 */
public class MpScMessageExecutor implements IMessageExecutor,Runnable{

    private BlockingQueue<ITask> taskQueue;

    private Thread thread;

    private String threadName;

    @Override
    public void start() {
        taskQueue = (BlockingQueue) PlatformDependent.newMpscQueue(Integer.MAX_VALUE);
        thread = new Thread(this,threadName);
        thread.start();
    }





    @Override
    public void stop() {

    }

    @Override
    public boolean isFull() {
        return taskQueue.size()==Integer.MAX_VALUE;
    }

    @Override
    public void execute(ITask iTask) {
        this.taskQueue.offer(iTask);
    }

    @Override
    public void run() {
        BlockingQueue<ITask> taskQueue = this.taskQueue;
        for(;;){
            ITask task = null;
            try {
                task = taskQueue.take();
            } catch (InterruptedException e) {
            }
            if(task!=null){
                task.execute();
            }
        }
    }


}
