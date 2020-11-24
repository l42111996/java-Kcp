package threadPool.netty;

import io.netty.channel.EventLoop;
import threadPool.IMessageExecutor;
import threadPool.ITask;

/**
 * Created by JinMiao
 * 2020/11/24.
 */
public class NettyMessageExecutor implements IMessageExecutor {

    private EventLoop eventLoop;


    public NettyMessageExecutor(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public void execute(ITask iTask) {
        this.eventLoop.execute(() -> iTask.execute());
    }
}
