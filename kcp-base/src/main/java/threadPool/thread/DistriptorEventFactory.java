package threadPool.thread;

import com.lmax.disruptor.EventFactory;

public class DistriptorEventFactory implements EventFactory<DistriptorHandler>
{

	public DistriptorHandler newInstance() {
		return new DistriptorHandler();
	}

}
