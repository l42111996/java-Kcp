package threadPool.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于disruptor的线程池
 * @author jinmiao
 * 2014-9-12 上午9:51:09
 */
public class DisruptorExecutorPool
{
	private static final Logger log = LoggerFactory.getLogger(DisruptorExecutorPool.class);

	protected List<IMessageExecutor> executor = new Vector<>();
	
	protected AtomicInteger index = new AtomicInteger();

    /**定时线程池**/
    private static final ScheduledThreadPoolExecutor scheduled  = new ScheduledThreadPoolExecutor(1,new TimerThreadFacotry());


    public static ScheduledThreadPoolExecutor getScheduled() {
        return scheduled;
    }


    /**定时器线程工厂**/
    private static class TimerThreadFacotry implements ThreadFactory
    {
        private AtomicInteger timeThreadName=new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r,"TimerThread "+timeThreadName.addAndGet(1));
            return thread;
        }
    }


    public static ScheduledFuture<?> schedule(Runnable command,long milliseconds){
        return scheduled.schedule(command,milliseconds, TimeUnit.MILLISECONDS);
    }

	/**
	 * 创造一个线程对象
	 * @param threadName
	 * @return
	 */
	public IMessageExecutor createDisruptorProcessor(String threadName)
	{
		IMessageExecutor singleProcess = new DisruptorSingleExecutor(threadName);
		executor.add(singleProcess);
        singleProcess.start();
		return singleProcess;
	}



	public void stop()
	{
		for(IMessageExecutor process:executor)
		{
			process.stop();
		}

		if(!scheduled.isShutdown())
			scheduled.shutdown();
	}
	
	
	
	/**
	 * 从线程池中按算法获得一个线程对象
	 * @return
	 */
	public IMessageExecutor getAutoDisruptorProcessor()
	{
		int index = this.index.incrementAndGet();
		return executor.get(index%executor.size());
	}

}
