package main;

import threadPool.order.IOrderTask;
import threadPool.order.OrderedThreadPoolExecutor;
import threadPool.order.OrderedThreadSession;
import threadPool.disruptor.DisruptorExecutorPool;
import threadPool.IMessageExecutor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试性能
 * Created by JinMiao
 * 2020/6/19.
 */
public class TestThreadLoop {


    AtomicInteger product = new AtomicInteger();
    AtomicInteger total = new AtomicInteger();
    private long lastTime;

    AtomicInteger id = new AtomicInteger();

    OrderedThreadPoolExecutor orderedThreadPoolExecutor = new OrderedThreadPoolExecutor(5,5,1000000, TimeUnit.HOURS,r -> new Thread(r,"executor  "+id.incrementAndGet()));
    DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool(5);


    public static void main(String[] args) {
        TestThreadLoop testThreadLoop = new TestThreadLoop();
        new Thread(() -> {
            for (;;){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                testThreadLoop.print();
            }
        }).start();

        //IMessageExecutor iMessageExecutor= threadLoopTest.disruptorExecutorPool.createDisruptorProcessor("test");
        //threadLoopTest.distuprorProduct(iMessageExecutor,1);
        //threadLoopTest.distuprorProduct(iMessageExecutor,2);
        //threadLoopTest.distuprorProduct(iMessageExecutor,3);
        //threadLoopTest.distuprorProduct(iMessageExecutor,4);
        //threadLoopTest.distuprorProduct(iMessageExecutor,5);
        //threadLoopTest.distuprorProduct(iMessageExecutor,6);

        OrderedThreadSession orderedThreadSession = new OrderedThreadSession();
        OrderedThreadSession orderedThreadSession1 = new OrderedThreadSession();
        orderedThreadSession.setId(1);
        orderedThreadSession1.setId(2);
        testThreadLoop.product(orderedThreadSession,1);
        testThreadLoop.product(orderedThreadSession,2);
        testThreadLoop.product(orderedThreadSession1,3);
        testThreadLoop.product(orderedThreadSession1,4);

    }


    public void distuprorProduct(IMessageExecutor iMessageExecutor,int index){
        new Thread(() -> {
            for (;;){
                //offer
                iMessageExecutor.execute(() -> consumer());
                product.incrementAndGet();
            }
        },"product"+index).start();
    }


    public void product(OrderedThreadSession orderedThreadSession,int index){
        new Thread(() -> {
            for (;;){
                //System.out.println("生产"+Thread.currentThread().getName());
                //offer
                offer(orderedThreadSession);
                product.incrementAndGet();
            }
        },"product"+index).start();
    }


    public void print(){
        long now =System.currentTimeMillis();
        System.out.println("product : "+product+" consumer: "+ total +" time: "+(now-lastTime) );
        lastTime = now;
        product.set(0);
        total.set(0);
    }

    public void offer(OrderedThreadSession orderedThreadSession){
        IOrderTask iOrderTask = new IOrderTask() {
            @Override
            public OrderedThreadSession getSession() {
                return orderedThreadSession;
            }
            @Override
            public void run() {
                //System.out.println("消费"+Thread.currentThread().getName());
                consumer();
            }
        };
        orderedThreadPoolExecutor.execute(iOrderTask);
    }

    private int add=0;
    private AtomicInteger atomicAdd = new AtomicInteger();



    public void consumer(){
        total.incrementAndGet();
        //try {
        //    Thread.sleep(100);
        //    System.out.println(Thread.currentThread().getName());
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        int result = atomicAdd.addAndGet(1);
        add++;
        if(result!=add){
            System.out.println(add+" "+result);
        }
    }
}
