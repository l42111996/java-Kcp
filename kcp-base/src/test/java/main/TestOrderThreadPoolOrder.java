package main;

import threadPool.order.IOrderTask;
import threadPool.order.OrderedThreadPoolExecutor;
import threadPool.order.OrderedThreadSession;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 测试乱序
 * Created by JinMiao
 * 2020/6/24.
 */
public class TestOrderThreadPoolOrder {


    AtomicInteger product = new AtomicInteger();
    AtomicInteger total = new AtomicInteger();
    AtomicInteger id = new AtomicInteger();

    OrderedThreadPoolExecutor orderedThreadPoolExecutor = new OrderedThreadPoolExecutor(4,4,1000000, TimeUnit.HOURS, r -> new Thread(r,"executor  "+id.incrementAndGet()));


    public static void main(String[] args) throws InterruptedException {
        TestOrderThreadPoolOrder testThreadLoop = new TestOrderThreadPoolOrder();

        int sessionNumber = 4;
        int productNumber = 4;
        int taskNumber = 10000_0000;
        OrderedThreadSession[] orderedThreadSessions = new OrderedThreadSession[sessionNumber];
        for (int i = 0; i < sessionNumber; i++) {
            orderedThreadSessions[i] = new OrderedThreadSession();
        }

        taskNumber-=taskNumber%productNumber;

        int task = taskNumber/productNumber;

        start(testThreadLoop,productNumber,task,sessionNumber,orderedThreadSessions);


        for (;;){
            int total = 0;
            for (OrderedThreadSession orderedThreadSession : orderedThreadSessions) {
                total+=orderedThreadSession.getId();
            }
            Thread.sleep(1000);
            System.out.println("生产数 "+taskNumber+" 消费数 "+total+"  "+testThreadLoop.total.get());

            if(total==taskNumber){
                for (OrderedThreadSession orderedThreadSession : orderedThreadSessions) {
                    orderedThreadSession.setId(0);
                }
                testThreadLoop.product.set(0);
                testThreadLoop.total.set(0);

                start(testThreadLoop,productNumber,task,sessionNumber,orderedThreadSessions);

            }
        }
    }


    private static void start(TestOrderThreadPoolOrder testThreadLoop,int productNumber,int task,int sessionNumber,OrderedThreadSession[] orderedThreadSessions){

        for (int i = 0; i < productNumber; i++) {
            OrderedThreadSession orderedThreadSession = orderedThreadSessions[i%sessionNumber];
            System.out.println(orderedThreadSession.hashCode());
            testThreadLoop.product(orderedThreadSession,i,task);
        }
    }





    public void product(OrderedThreadSession orderedThreadSession,int index,int taskNumber){
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < taskNumber; i++) {
                offer(orderedThreadSession,index,taskNumber,startTime);
                product.incrementAndGet();
            }
            System.out.println("生产者"+index+" 生产耗时 "+(System.currentTimeMillis()-startTime));
        },"product"+index).start();
    }


    public void offer(OrderedThreadSession orderedThreadSession,int index,int taskNumber,long startTime){
        IOrderTask iOrderTask = new IOrderTask() {
            @Override
            public OrderedThreadSession getSession() {
                return orderedThreadSession;
            }
            @Override
            public void run() {
                total.incrementAndGet();
                orderedThreadSession.setId(orderedThreadSession.getId()+1);

                if(orderedThreadSession.getId()==taskNumber){
                    System.out.println("消费者 "+index+"  消耗任务"+taskNumber+" 耗时 "+(System.currentTimeMillis()-startTime));
                }
            }
        };
        orderedThreadPoolExecutor.execute(iOrderTask);
    }
}
