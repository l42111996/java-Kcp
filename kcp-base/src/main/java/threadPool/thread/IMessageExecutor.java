package threadPool.thread;

import threadPool.task.ITask;

/**
 * 消息处理器
 */
public interface IMessageExecutor{
	/**
	 * 启动消息处理器
	 */
	void start();

	/**
	 * 停止消息处理器
	 */
	void stop();



	/**
	 * 判断队列是否已经达到上限了
	 * @return
	 */
	boolean isFull();


	void execute(ITask iTask);
}