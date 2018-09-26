package threadPool.thread;

import kcp.Ukcp;
import threadPool.task.ITask;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by JinMiao
 * 2018/9/25.
 */
public class KcpEventLoop implements ITask,Runnable {

    private Map<SocketAddress, Ukcp> ukcpMap;

    private DisruptorSingleExecutor disruptorSingleExecutor;


    @Override
    public void run() {

    }

    @Override
    public void execute() {

    }
}
