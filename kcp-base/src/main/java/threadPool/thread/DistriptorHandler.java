package threadPool.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.task.ITask;

public class DistriptorHandler
{

	protected static final Logger logger = LoggerFactory.getLogger(DistriptorHandler.class);
	private ITask task;


	public void execute()
	{
		try {
			this.task.execute();
		} catch (Throwable throwable) {
			logger.error("error",throwable);
		}
	}


	public void setTask(ITask task) {
		this.task = task;
	}
}
