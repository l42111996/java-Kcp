package threadPool.thread;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import threadPool.task.ITask;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 {@link #Disruptor} 的单线程队列实现
 * @author King
 *
 */
public class DisruptorSingleExecutor implements IMessageExecutor {

	//65536条消息
	int ringBufferSize = 2<<15;
	
	private WaitStrategy strategy = new BlockingWaitStrategy();
	
	private Disruptor<DistriptorHandler> disruptor = null;

	private RingBuffer<DistriptorHandler> buffer = null;
	
	private DistriptorEventFactory eventFactory = new DistriptorEventFactory();
	
	private static final DistriptorEventHandler handler = new DistriptorEventHandler();
	
	private AtomicBoolean istop = new AtomicBoolean();
	

	/**线程名字**/
	private String threadName;



	public DisruptorSingleExecutor(String threadName)
	{
		this.threadName = threadName;
	}
	

	@SuppressWarnings("unchecked")
	public void start() {
//		disruptor = new Disruptor<DistriptorHandler>(eventFactory, ringBufferSize, executor, ProducerType.MULTI, strategy);
		disruptor = new Disruptor<>(eventFactory, ringBufferSize, (r)->{return new DisruptorThread(r, threadName,this);});
		buffer = disruptor.getRingBuffer();
		disruptor.handleEventsWith(DisruptorSingleExecutor.handler);
		disruptor.start();
	}
	

	
	/**主线程工厂**/
	private class LoopThreadfactory implements ThreadFactory {

		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName(threadName);
			return thread;
		}
	}
	

	static int num = 1;
	static long start = System.currentTimeMillis();
	static long lastNum = 0;


	public void stop() {
		if(istop.get())
			return;
		disruptor.shutdown();

		istop.set(true);
	}
	

	public AtomicBoolean getIstop() {
		return istop;
	}
	

	public boolean isFull() {
		return !buffer.hasAvailableCapacity(1);
	}

	@Override
	public void execute(ITask iTask) {
		//		if(buffer.hasAvailableCapacity(1))
//		{
//			System.out.println("没有容量了");
//		}
		long next = buffer.next();
		DistriptorHandler testEvent = buffer.get(next);
		testEvent.setTask(iTask);
		buffer.publish(next);
	}
}
