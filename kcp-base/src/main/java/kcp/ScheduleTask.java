package kcp;

import threadPool.task.ITask;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by JinMiao
 * 2018/10/24.
 */
public class ScheduleTask implements ITask,Runnable {

    private IMessageExecutor disruptorSingleExecutor;

    private Ukcp ukcp;
    private Map<SocketAddress,Ukcp> ukcpMap;


    public ScheduleTask(IMessageExecutor disruptorSingleExecutor, Ukcp ukcp, Map<SocketAddress, Ukcp> ukcpMap) {
        this.disruptorSingleExecutor = disruptorSingleExecutor;
        this.ukcp = ukcp;
        this.ukcpMap = ukcpMap;
    }

    /**
     * 是否执行完了
     **/
    private AtomicBoolean isExecute = new AtomicBoolean(true);


    //flush策略
    //1,在send调用后检查缓冲区如果可以发送直接调用update得到时间并存在ukcp内
    //2，定时任务到了检查ukcp的时间和自己的定时 如果可以发送则直接发送  时间延后则重新定时
    //定时任务发送成功后检测缓冲区  是否触发发送时间
    //3，读时间触发后检测检测缓冲区触发写事件
    //问题: 精准大量的flush触发会导致ack重复发送   流量增大？  不会的 ack只会发送一次
    @Override
    public void execute() {
        try {
            long now = System.currentTimeMillis();
            //判断连接是否关闭
            if (ukcp.getCloseTime() != 0 && now + ukcp.getCloseTime() > ukcp.getLastRecieveTime()) {
                ukcp.close();
            }
            if(!ukcp.isActive()){
                User user = ukcp.user();
                //抛回网络线程处理连接删除
                user.getChannel().eventLoop().execute(()-> ukcpMap.remove(user.getRemoteAddress()));
                return;
            }
            long timeLeft = ukcp.getTsUpdate()-now;
            //判断执行时间是否到了
            if(timeLeft>0){
                //System.err.println(timeLeft);
                DisruptorExecutorPool.schedule(this, timeLeft);
                return;
            }
            long next = ukcp.flush(now);
            //System.err.println(next);
            DisruptorExecutorPool.schedule(this, next);
            //检测写缓冲区 如果能写则触发写事件
            if(ukcp.canSend(false)){
                ukcp.notifyWriteEvent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        isExecute.set(false);
        this.disruptorSingleExecutor.execute(this);
    }
}
