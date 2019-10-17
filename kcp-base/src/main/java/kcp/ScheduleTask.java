package kcp;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import threadPool.task.ITask;
import threadPool.thread.DisruptorExecutorPool;
import threadPool.thread.IMessageExecutor;

/**
 * Created by JinMiao
 * 2018/10/24.
 */
public class ScheduleTask implements ITask, Runnable, TimerTask {

    private IMessageExecutor disruptorSingleExecutor;

    private Ukcp ukcp;
    private IChannelManager channelManager;


    public ScheduleTask(IMessageExecutor disruptorSingleExecutor, Ukcp ukcp, IChannelManager channelManager) {
        this.disruptorSingleExecutor = disruptorSingleExecutor;
        this.ukcp = ukcp;
        this.channelManager = channelManager;
    }

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
            if (ukcp.getTimeoutMillis() != 0 && now - ukcp.getTimeoutMillis() > ukcp.getLastRecieveTime()) {
                ukcp.close();
            }
            if (!ukcp.isActive()) {
                User user = ukcp.user();
                //抛回网络线程处理连接删除
                user.getChannel().eventLoop().execute(() -> channelManager.del(ukcp));
                ukcp.release();
                return;
            }
            long timeLeft = ukcp.getTsUpdate() - now;
            //判断执行时间是否到了
            if (timeLeft > 0) {
                //System.err.println(timeLeft);
                DisruptorExecutorPool.scheduleHashedWheel(this, timeLeft);
                return;
            }

            //long start = System.currentTimeMillis();
            long next = ukcp.flush(now);
            //System.err.println(next);
            //System.out.println("耗时  "+(System.currentTimeMillis()-start));
            DisruptorExecutorPool.scheduleHashedWheel(this, next);


            //检测写缓冲区 如果能写则触发写事件
            if (!ukcp.getSendList().isEmpty() && ukcp.canSend(false)
            ) {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        this.disruptorSingleExecutor.execute(this);
    }

    @Override
    public void run(Timeout timeout) {
        run();
    }
}
