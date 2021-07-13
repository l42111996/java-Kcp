package test.TpsTest;

import javax.print.attribute.standard.PrinterURI;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by JinMiao
 * 2021/7/13.
 */
public class Tps {

    private int cmd ;
    private LongAdder send;
    private LongAdder recieve;
    private float succRate;
    private AtomicLong maxDelay;
    private LongAdder delay;
    private AtomicLong minDelay;
    private LongAdder sumDelay;


    public Tps(int cmd, LongAdder send, LongAdder recieve, float succRate, AtomicLong maxDelay, LongAdder delay, AtomicLong minDelay, LongAdder sumDelay) {
        this.cmd = cmd;
        this.send = send;
        this.recieve = recieve;
        this.succRate = succRate;
        this.maxDelay = maxDelay;
        this.maxDelay.set(Integer.MIN_VALUE);
        this.delay = delay;
        this.minDelay = minDelay;
        this.minDelay.set(Integer.MAX_VALUE);
        this.sumDelay = sumDelay;
    }


    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public LongAdder getSend() {
        return send;
    }

    public void setSend(LongAdder send) {
        this.send = send;
    }

    public LongAdder getRecieve() {
        return recieve;
    }

    public void setRecieve(LongAdder recieve) {
        this.recieve = recieve;
    }

    public float getSuccRate() {
        return succRate;
    }

    public void setSuccRate(float succRate) {
        this.succRate = succRate;
    }


    public AtomicLong getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(AtomicLong maxDelay) {
        this.maxDelay = maxDelay;
    }

    public LongAdder getDelay() {
        return delay;
    }

    public void setDelay(LongAdder delay) {
        this.delay = delay;
    }

    public AtomicLong getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(AtomicLong minDelay) {
        this.minDelay = minDelay;
    }

    public LongAdder getSumDelay() {
        return sumDelay;
    }

    public void setSumDelay(LongAdder sumDelay) {
        this.sumDelay = sumDelay;
    }

    @Override
    public String toString() {
        return "Tps{" +
                "cmd=" + cmd +
                ", send=" + send +
                ", recieve=" + recieve +
                ", succRate=" + succRate +
                ", maxDelay=" + maxDelay +
                ", delay=" + delay +
                ", minDelay=" + minDelay +
                ", sumDelay=" + sumDelay +
                '}';
    }
}
