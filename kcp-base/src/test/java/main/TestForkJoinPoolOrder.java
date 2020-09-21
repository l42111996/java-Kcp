package main;

import threadPool.order.IOrderTask;
import threadPool.order.OrderedThreadPoolExecutor;
import threadPool.order.OrderedThreadSession;
import threadPool.order.forkjoin.ForkJoinPoolExecutor;
import threadPool.order.forkjoin.ForkJoinThreadSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 测试乱序
 * Created by JinMiao
 * 2020/6/24.
 */
public class TestForkJoinPoolOrder {


    AtomicInteger product = new AtomicInteger();
    AtomicInteger total = new AtomicInteger();
    AtomicInteger id = new AtomicInteger();

    ForkJoinPoolExecutor orderedThreadPoolExecutor = new ForkJoinPoolExecutor();


    public static void main(String[] args) throws InterruptedException {
        TestForkJoinPoolOrder testThreadLoop = new TestForkJoinPoolOrder();

        int sessionNumber = 100;
        int productNumber = 4;
        int taskNumber = 10000_0000;
        ForkJoinThreadSession[] orderedThreadSessions = new ForkJoinThreadSession[sessionNumber];
        for (int i = 0; i < sessionNumber; i++) {
            orderedThreadSessions[i] = new ForkJoinThreadSession();
        }

        taskNumber-=taskNumber%productNumber;

        int task = taskNumber/productNumber;

        start(testThreadLoop,productNumber,task,sessionNumber,orderedThreadSessions);


        for (;;){
            int total = 0;
            for (ForkJoinThreadSession orderedThreadSession : orderedThreadSessions) {
                total+=orderedThreadSession.getId();
            }
            Thread.sleep(1000);
            System.out.println("生产数 "+taskNumber+" 消费数 "+total+"  "+testThreadLoop.total.get());

            if(total==taskNumber){
                for (ForkJoinThreadSession orderedThreadSession : orderedThreadSessions) {
                    orderedThreadSession.setId(0);
                }
                testThreadLoop.product.set(0);
                testThreadLoop.total.set(0);

                start(testThreadLoop,productNumber,task,sessionNumber,orderedThreadSessions);

            }
        }
    }


    private static void start(TestForkJoinPoolOrder testThreadLoop, int productNumber, int task, int sessionNumber, ForkJoinThreadSession[] orderedThreadSessions){

        for (int i = 0; i < productNumber; i++) {
            ForkJoinThreadSession orderedThreadSession = orderedThreadSessions[i%sessionNumber];
            System.out.println(orderedThreadSession.hashCode());
            testThreadLoop.product(orderedThreadSession,i,task);
        }
    }





    public void product(ForkJoinThreadSession orderedThreadSession,int index,int taskNumber){
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < taskNumber; i++) {
                offer(orderedThreadSession,index,taskNumber,startTime);
                product.incrementAndGet();
            }
            System.out.println("生产者"+index+" 生产耗时 "+(System.currentTimeMillis()-startTime));
        },"product"+index).start();
    }


    public void offer(ForkJoinThreadSession orderedThreadSession,int index,int taskNumber,long startTime){
        Runnable iOrderTask = new Runnable() {
            @Override
            public void run() {
                total.incrementAndGet();
                orderedThreadSession.setId(orderedThreadSession.getId()+1);

                if(orderedThreadSession.getId()==taskNumber){
                    System.out.println("消费者 "+index+"  消耗任务"+taskNumber+" 耗时 "+(System.currentTimeMillis()-startTime));
                }
            }
        };
        orderedThreadPoolExecutor.execute(orderedThreadSession,iOrderTask);
    }
}
