package threadPool.order.forkjoin;

import java.util.Queue;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by JinMiao
 * 2020/8/3.
 */
public class ForkJoinPoolExecutor extends ForkJoinPool {

    public void execute(ForkJoinThreadSession session,Runnable event){

        Queue<Runnable> tasksQueue = session.getQueue();


        boolean offerSession = tasksQueue.offer(event);
        //没有投递成功
        if (!offerSession) {
            //TODO 队列满了应该怎么办？ 丢弃该消息?
            return;
        }
        if (session.getProcessingCompleted().compareAndSet(true, false)) {
            super.execute(session);
        }
    }
}
