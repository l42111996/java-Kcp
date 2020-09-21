package kcp;

import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import threadPool.task.ITask;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2020/7/29.
 */
public class Test1 implements ITask, TimerTask, Runnable {

    IMessageExecutor iMessageExecutor;

    EventLoop eventLoop;

    Random random = new Random();
    int next = random.nextInt(10)+1;

    static final List<Test1> taskList = new ArrayList<>();


    public static void main(String[] args) {

        DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptor" + i);
        }
        MultithreadEventLoopGroup eventExecutors = new NioEventLoopGroup();

        for (int i = 0; i < 3000; i++) {

            Test1 test = new Test1();
            taskList.add(test);

            long now = System.currentTimeMillis();

            IMessageExecutor iMessageExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
            test.iMessageExecutor = iMessageExecutor;

            EventLoop eventLoop = eventExecutors.next();
            test.eventLoop = eventLoop;

            //DisruptorExecutorPool.scheduleHashedWheel(test,10);

            eventLoop.schedule(test, 10, TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void execute() {
        DisruptorExecutorPool.scheduleHashedWheel(this, next);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        iMessageExecutor.execute(this);
        //eventLoop.execute(this);
    }

    @Override
    public void run() {
        //System.out.println(System.currentTimeMillis());
        eventLoop.schedule(this, next, TimeUnit.MILLISECONDS);
        //DisruptorExecutorPool.scheduleHashedWheel(this,10);
    }
}

