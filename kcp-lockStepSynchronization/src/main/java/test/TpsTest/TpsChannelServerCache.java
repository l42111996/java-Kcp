package test.TpsTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class TpsChannelServerCache {
    private List<Integer> sendPackIds = new ArrayList<>();


    public synchronized void addPackId(int packId){
        this.sendPackIds.add(packId);
    }
    public synchronized int size(){
        return sendPackIds.size();
    }

    public synchronized List<Integer> getSendPackIds(){
        List<Integer> sendPackIds = this.sendPackIds;
        this.sendPackIds = new ArrayList<>();
        return sendPackIds;
    }

}
