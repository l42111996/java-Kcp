package threadPool;

import io.netty.channel.DefaultEventLoop;
import io.netty.util.HashedWheelTimer;
import io.netty.util.TimerTask;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内部定时器
 * Created by JinMiao
 * 2020/11/24.
 */
public class TimerThreadPool {
    /**定时线程池**/
    //private static final ScheduledThreadPoolExecutor scheduled  = new ScheduledThreadPoolExecutor(1,new TimerThreadFactory());
    private static final DefaultEventLoop EVENT_EXECUTORS = new DefaultEventLoop();


    private static final HashedWheelTimer HASHED_WHEEL_TIMER = new HashedWheelTimer(new TimerThreadFactory(),1, TimeUnit.MILLISECONDS);

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long milliseconds){
        return EVENT_EXECUTORS.scheduleWithFixedDelay(command,milliseconds,milliseconds, TimeUnit.MILLISECONDS);
    }

    public static void scheduleHashedWheel(TimerTask timerTask, long milliseconds){
        HASHED_WHEEL_TIMER.newTimeout(timerTask,milliseconds,TimeUnit.MILLISECONDS);
    }


    /**定时器线程工厂**/
    private static class TimerThreadFactory implements ThreadFactory
    {
        private AtomicInteger timeThreadName=new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r,"TimerThread "+timeThreadName.addAndGet(1));
            return thread;
        }
    }


    public void stop()
    {
        HASHED_WHEEL_TIMER.stop();
        if(!EVENT_EXECUTORS.isShuttingDown()){
            EVENT_EXECUTORS.shutdownGracefully();
        }
    }
}
