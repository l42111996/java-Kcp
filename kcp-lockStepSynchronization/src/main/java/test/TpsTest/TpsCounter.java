package test.TpsTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class TpsCounter {
    private static  final Logger logger = LoggerFactory.getLogger(TpsCounter.class);

    private Map<Integer, Tps> cmds = new ConcurrentHashMap<>();


    public void add(int cmd) {
        Tps tps = cmds.computeIfAbsent(cmd, integer -> new Tps(cmd, new LongAdder(), new LongAdder(), 0, new AtomicLong(), new LongAdder(), new AtomicLong(), new LongAdder()));
        tps.getSend().increment();
        tps.setCmd(cmd);
    }



    public void set(int cmd,int delay){
        Tps tps = cmds.get(cmd);
        tps.getRecieve().increment();
        tps.getMaxDelay().getAndUpdate(operand -> Math.max(delay,operand));
        tps.getSumDelay().add(delay);
        tps.getMinDelay().getAndUpdate(operand -> Math.min(delay,operand));
    }



    public void count(){
        for (Map.Entry<Integer, Tps> tpsEntry : cmds.entrySet()) {
            int cmd = tpsEntry.getKey();
            Tps tps = tpsEntry.getValue();
            long receive = tps.getRecieve().sum();
            long send = tps.getSend().sum();
            if(receive>0){
                long xDelay = tps.getSumDelay().sum() / receive;
                float rate = receive *1F/send;
                float tpss = 1000;
                if(xDelay!=0){
                    tpss = 1000/xDelay;
                }
                logger.error("Tps 命令号:{} 发包数:{} 收包数:{} 成功率:{}  延时（最大:{}ms、平均:{}ms、最小:{}ms）tps:{}",cmd,receive,send,rate,tps.getMaxDelay().get(),xDelay,tps.getMinDelay().get(),tpss);
            }
            //if(send==receive){
            //    cmds.remove(cmd);
            //}else{
            //}
            cmds.put(cmd,new Tps(cmd, new LongAdder(), new LongAdder(), 0, new AtomicLong(), new LongAdder(), new AtomicLong(), new LongAdder()));
        }

    }
}
