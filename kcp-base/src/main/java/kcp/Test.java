package kcp;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import threadPool.task.ITask;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2020/7/29.
 */
public class Test implements ITask , TimerTask,Runnable {
    IMessageExecutor iMessageExecutor;
    EventLoop eventLoop;


    private volatile long lastUpdate =System.currentTimeMillis();

    Random random = new Random();
    int next = random.nextInt(10)+1;

    public static void main(String[] args) {

        DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool();

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            disruptorExecutorPool.createDisruptorProcessor("disruptor"+i);
        }
        MultithreadEventLoopGroup eventExecutors = new NioEventLoopGroup();

        for (int i = 0; i < 10000; i++) {

            Test test = new Test();
            IMessageExecutor iMessageExecutor = disruptorExecutorPool.getAutoDisruptorProcessor();
            test.iMessageExecutor = iMessageExecutor;

            EventLoop eventLoop = eventExecutors.next();
            test.eventLoop = eventLoop;

            DisruptorExecutorPool.scheduleHashedWheel(test,10);

            //eventLoop.schedule(test,10,TimeUnit.MILLISECONDS);
        }
    }


    @Override
    public void execute() {
        //long now = System.currentTimeMillis();
        //long inv = now-lastUpdate;
        //if(inv>next+3){
        //    System.out.println(inv-next);
        //}
        //lastUpdate = now;


        DisruptorExecutorPool.scheduleHashedWheel(this,next);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        //long now = System.currentTimeMillis();
        //long inv = now-lastUpdate;
        //if(inv>next+3){
        //    System.out.println(inv-next);
        //}

        //lastUpdate = now;


        iMessageExecutor.execute(this);
        //start = System.currentTimeMillis();
        //eventLoop.execute(this);
    }

    volatile long start = System.currentTimeMillis();

    @Override
    public void run() {
        try {
            //long now = System.currentTimeMillis();
            //long inv = now-lastUpdate;
            //if(inv>3){
            //    //System.out.println(inv);
            //}
            //lastUpdate = now;


            DisruptorExecutorPool.scheduleHashedWheel(this,next);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
