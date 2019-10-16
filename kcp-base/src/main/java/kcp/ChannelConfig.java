package kcp;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
public class ChannelConfig {
    private int conv;
    private boolean nodelay;
    private int interval = Kcp.IKCP_INTERVAL;
    private int fastresend;
    private boolean nocwnd;
    private int sndwnd = Kcp.IKCP_WND_SND;
    private int rcvwnd = Kcp.IKCP_WND_RCV;
    private int mtu = Kcp.IKCP_MTU_DEF;
    //超时时间 超过一段时间没收到消息断开连接
    private long timeoutMillis;
    //TODO 可能有bug还未测试
    private boolean stream;

    //下面为新增参数
    private int fecDataShardCount;
    private int fecParityShardCount;
    //收到包立刻回传ack包
    private boolean ackNoDelay = false;
    //发送包立即调用flush 延迟低一些  cpu增加  如果interval值很小 建议关闭该参数
    private boolean fastFlush = true;
    //crc32校验
    private boolean crc32Check = false;

    private boolean autoSetConv = false;
    //增加ack包回复成功率 填 /8/16/32
    private int ackMaskSize = 0;


    public void nodelay(boolean nodelay, int interval, int resend, boolean nc){
        this.nodelay = nodelay;
        this.interval = interval;
        this.fastresend = resend;
        this.nocwnd=nc;
    }


    public boolean isNodelay() {
        return nodelay;
    }


    public void setNodelay(boolean nodelay) {
        this.nodelay = nodelay;
    }

    public int getConv() {
        return conv;
    }

    public void setConv(int conv) {
        this.conv = conv;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getFastresend() {
        return fastresend;
    }

    public void setFastresend(int fastresend) {
        this.fastresend = fastresend;
    }

    public boolean isNocwnd() {
        return nocwnd;
    }

    public void setNocwnd(boolean nocwnd) {
        this.nocwnd = nocwnd;
    }

    public int getSndwnd() {
        return sndwnd;
    }

    public void setSndwnd(int sndwnd) {
        this.sndwnd = sndwnd;
    }

    public int getRcvwnd() {
        return rcvwnd;
    }

    public boolean isAutoSetConv() {
        return autoSetConv;
    }

    public void setAutoSetConv(boolean autoSetConv) {
        this.autoSetConv = autoSetConv;
    }

    public void setRcvwnd(int rcvwnd) {
        this.rcvwnd = rcvwnd;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public int getFecDataShardCount() {
        return fecDataShardCount;
    }

    public void setFecDataShardCount(int fecDataShardCount) {
        this.fecDataShardCount = fecDataShardCount;
    }

    public int getFecParityShardCount() {
        return fecParityShardCount;
    }

    public void setFecParityShardCount(int fecParityShardCount) {
        this.fecParityShardCount = fecParityShardCount;
    }

    public boolean isAckNoDelay() {
        return ackNoDelay;
    }

    public void setAckNoDelay(boolean ackNoDelay) {
        this.ackNoDelay = ackNoDelay;
    }

    public boolean isFastFlush() {
        return fastFlush;
    }

    public void setFastFlush(boolean fastFlush) {
        this.fastFlush = fastFlush;
    }

    public boolean isCrc32Check() {
        return crc32Check;
    }

    public int getAckMaskSize() {
        return ackMaskSize;
    }

    public void setAckMaskSize(int ackMaskSize) {
        this.ackMaskSize = ackMaskSize;
    }

    public void setCrc32Check(boolean crc32Check) {
        this.crc32Check = crc32Check;
    }
}
