package test;

import threadPool.disruptor.DisruptorExecutorPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2020/7/17.
 */
public class RoomManager {
    Map<Integer,Room> playerRooms = new ConcurrentHashMap<>();

    DisruptorExecutorPool disruptorExecutorPool = new DisruptorExecutorPool(1);

    public void remove(Integer playerId){
        this.playerRooms.remove(playerId);

    }


    public Room getRoom(Integer playerId){
        return playerRooms.get(playerId);
    }



    public synchronized void joinRoom(Player player){
        Room room = null;
        for (Room value : playerRooms.values()) {
            if(value.getPlayers().size()==8)
            {
                continue;
            }
            if(room==null){
                room = value;
                continue;
            }
            if(room.getPlayers().size()>value.getPlayers().size()){
                room = value;
            }
        }
        if(room==null){
            room = new Room();
            room.setiMessageExecutor(disruptorExecutorPool.getIMessageExecutor());
            TimerThreadPool.scheduleWithFixedDelay(room,50);
        }
        playerRooms.put(player.getId(),room);
        room.getPlayers().put(player.getId(),player);
    }
}
