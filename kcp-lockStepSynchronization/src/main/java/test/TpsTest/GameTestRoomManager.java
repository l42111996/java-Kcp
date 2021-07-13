package test.TpsTest;

import kcp.Ukcp;
import test.TimerThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class GameTestRoomManager {

    private Map<Integer,TestRoom> rooms = new ConcurrentHashMap<>();


    public synchronized void addClient(Ukcp ukcp){
        boolean isAdd = false;
        for (TestRoom room : rooms.values()) {
            if(room.size()==8){
                continue;
            }
            room.getUkcps().add(ukcp);
            isAdd = true;
        }

        if(!isAdd){
            TestRoom testRoom = new TestRoom();
            testRoom.getUkcps().add(ukcp);
            rooms.put(testRoom.getRoomId(),testRoom);
            TimerThreadPool.scheduleWithFixedDelay(testRoom,50);
        }
    }


    public Map<Integer, TestRoom> getRooms() {
        return rooms;
    }

    public synchronized void remove(Ukcp ukcp){
        for (TestRoom room : rooms.values()) {
            if(room.getUkcps().remove(ukcp)){
                if(room.getUkcps().isEmpty()){
                    rooms.remove(room.getRoomId());
                }
                return;
            }
        }
    }
}
