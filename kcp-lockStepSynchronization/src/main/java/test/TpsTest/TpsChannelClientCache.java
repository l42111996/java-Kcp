package test.TpsTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class TpsChannelClientCache {

    private int index;
    private Map<Integer,Long> packetTime = new HashMap<>();

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Map<Integer, Long> getPacketTime() {
        return packetTime;
    }

    public void setPacketTime(Map<Integer, Long> packetTime) {
        this.packetTime = packetTime;
    }
}
