package test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import threadPool.task.ITask;
import threadPool.thread.IMessageExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2019-06-26.
 */
public class Room implements Runnable, ITask {
    Map<Integer,Player> players = new ConcurrentHashMap<>();

    private IMessageExecutor iMessageExecutor;

    private volatile boolean executed;


    public Map<Integer, Player> getPlayers() {
        return players;
    }

    public void setPlayers(Map<Integer, Player> players) {
        this.players = players;
    }

    public IMessageExecutor getiMessageExecutor() {
        return iMessageExecutor;
    }

    public void setiMessageExecutor(IMessageExecutor iMessageExecutor) {
        this.iMessageExecutor = iMessageExecutor;
    }

    public void execute() {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(2048);
        boolean needSend = false;
        for (Player player : players.values()) {
            for (ByteBuf message : player.getMessages()) {
                needSend = true;
                byteBuf.writeBytes(message);
                message.release();
            }
            player.getMessages().clear();
        }
        if(!needSend){
            byteBuf.release();
            return;
        }
        //System.out.println("发送"+byteBuf.writerIndex()+"房间人数"+ players.size());
        for (Player player : players.values()) {
            player.getUkcp().writeOrderedReliableMessage(byteBuf);
        }
        byteBuf.release();
    }

    @Override
    public void run() {
        iMessageExecutor.execute(this);
    }
}
