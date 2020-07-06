package kcp;

import com.backblaze.erasure.ReedSolomon;
import com.backblaze.erasure.fec.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.shaded.org.jctools.queues.MpscChunkedArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import threadPool.thread.IMessageExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ukcp{

    private static final InternalLogger log = InternalLoggerFactory.getInstance(Ukcp.class);


    private final Kcp kcp;

    private boolean fastFlush = true;

    private long tsUpdate = -1;

    private boolean active;

    private FecEncode fecEncode = null;
    private FecDecode fecDecode = null;

    private final Queue<ByteBuf> writeQueue;

    private final Queue<ByteBuf> readQueue;

    private final IMessageExecutor iMessageExecutor;

    private final KcpListener kcpListener;

    private final ChannelConfig channelConfig;

    private final IChannelManager channelManager;

    private AtomicBoolean writeProcessing = new AtomicBoolean(false);

    private AtomicBoolean readProcessing = new AtomicBoolean(false);

    /**
     * 上次收到消息时间
     **/
    private long lastRecieveTime = System.currentTimeMillis();


    /**
     * Creates a new instance.
     *
     * @param output output for kcp
     */
    public Ukcp(KcpOutput output, KcpListener kcpListener, IMessageExecutor iMessageExecutor,ReedSolomon reedSolomon,ChannelConfig channelConfig,IChannelManager channelManager) {
        this.channelConfig = channelConfig;
        this.kcp = new Kcp(channelConfig.getConv(), output);
        this.active = true;
        this.kcpListener = kcpListener;
        this.iMessageExecutor = iMessageExecutor;
        this.channelManager = channelManager;
        //默认2<<16   可以修改
        writeQueue = new MpscLinkedQueue<>();
        readQueue = new MpscChunkedArrayQueue<>(2<<11);

        int headerSize = 0;
        //init fec
        if (reedSolomon != null) {
            KcpOutput kcpOutput = kcp.getOutput();
            fecEncode = new FecEncode(headerSize, reedSolomon,channelConfig.getMtu());
            fecDecode = new FecDecode(3 * reedSolomon.getTotalShardCount(), reedSolomon,channelConfig.getMtu());
            kcpOutput = new FecOutPut(kcpOutput, fecEncode);
            kcp.setOutput(kcpOutput);
            headerSize+= Fec.fecHeaderSizePlus2;
        }

        kcp.setReserved(headerSize);
        intKcpConfig(channelConfig);
    }


    private void intKcpConfig(ChannelConfig channelConfig){
        kcp.nodelay(channelConfig.isNodelay(),channelConfig.getInterval(),channelConfig.getFastresend(),channelConfig.isNocwnd());
        kcp.setSndWnd(channelConfig.getSndwnd());
        kcp.setRcvWnd(channelConfig.getRcvwnd());
        kcp.setMtu(channelConfig.getMtu());
        kcp.setStream(channelConfig.isStream());
        kcp.setAckNoDelay(channelConfig.isAckNoDelay());
        kcp.setAckMaskSize(channelConfig.getAckMaskSize());
        this.fastFlush = channelConfig.isFastFlush();
    }


    /**
     * Receives ByteBufs.
     *
     * @param bufList received ByteBuf will be add to the list
     */
    protected void receive(List<ByteBuf> bufList) {
        kcp.recv(bufList);
    }


    protected ByteBuf mergeReceive() {
        return kcp.mergeRecv();
    }


    protected void input(ByteBuf data,long current) throws IOException {
        //lastRecieveTime = System.currentTimeMillis();
        Snmp.snmp.InPkts.increment();
        Snmp.snmp.InBytes.add(data.readableBytes());

        if (fecDecode != null) {
            FecPacket fecPacket = FecPacket.newFecPacket(data);
            if (fecPacket.getFlag() == Fec.typeData) {
                data.skipBytes(2);
                input(data, true,current);
            }
            if (fecPacket.getFlag() == Fec.typeData || fecPacket.getFlag() == Fec.typeParity) {
                List<ByteBuf> byteBufs = fecDecode.decode(fecPacket);
                if (byteBufs != null) {
                    for (ByteBuf byteBuf : byteBufs) {
                        input(byteBuf, false,current);
                        byteBuf.release();
                    }
                }
            }
        } else {
            input(data, true,current);
        }
    }

    private void input(ByteBuf data, boolean regular,long current) throws IOException {
        int ret = kcp.input(data, regular,current);
        switch (ret) {
            case -1:
                throw new IOException("No enough bytes of head");
            case -2:
                throw new IOException("No enough bytes of data");
            case -3:
                throw new IOException("Mismatch cmd");
            case -4:
                throw new IOException("Conv inconsistency");
            default:
                break;
        }
    }


    /**
     * Sends a Bytebuf.
     *
     * @param buf
     * @throws IOException
     */
    void send(ByteBuf buf) throws IOException {
        int ret = kcp.send(buf);
        switch (ret) {
            case -2:
                throw new IOException("Too many fragments");
            default:
                break;
        }
    }


    /**
     * Returns {@code true} if there are bytes can be received.
     *
     * @return
     */
    protected boolean canRecv() {
        return kcp.canRecv();
    }



    public long getLastRecieveTime() {
        return lastRecieveTime;
    }

    protected void setLastRecieveTime(long lastRecieveTime) {
        this.lastRecieveTime = lastRecieveTime;
    }

    /**
     * Returns {@code true} if the kcp can send more bytes.
     *
     * @param curCanSend last state of canSend
     * @return {@code true} if the kcp can send more bytes
     */
    protected boolean canSend(boolean curCanSend) {
        int max = kcp.getSndWnd() * 2;
        int waitSnd = kcp.waitSnd();
        if (curCanSend) {
            return waitSnd < max;
        } else {
            int threshold = Math.max(1, max / 2);
            return waitSnd < threshold;
        }
    }

    /**
     * Udpates the kcp.
     *
     * @param current current time in milliseconds
     * @return the next time to update
     */
    protected long update(long current) {
        kcp.update(current);
        long nextTsUp = check(current);
        setTsUpdate(nextTsUp);

        return nextTsUp;
    }

    protected long flush(long current){
        return kcp.flush(false,current);
    }

    /**
     * Determines when should you invoke udpate.
     *
     * @param current current time in milliseconds
     * @return
     * @see Kcp#check(long)
     */
    protected long check(long current) {
        return kcp.check(current);
    }

    /**
     * Returns {@code true} if the kcp need to flush.
     *
     * @return {@code true} if the kcp need to flush
     */
    protected boolean checkFlush() {
        return kcp.checkFlush();
    }

    /**
     * Sets params of nodelay.
     *
     * @param nodelay  {@code true} if nodelay mode is enabled
     * @param interval protocol internal work interval, in milliseconds
     * @param resend   fast retransmission mode, 0 represents off by default, 2 can be set (2 ACK spans will result
     *                 in direct retransmission)
     * @param nc       {@code true} if turn off flow control
     */
    public void nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        kcp.nodelay(nodelay, interval, resend, nc);
    }

    /**
     * Returns conv of kcp.
     *
     * @return conv of kcp
     */
    public int getConv() {
        return kcp.getConv();
    }

    /**
     * Set the conv of kcp.
     *
     * @param conv the conv of kcp
     */
    public void setConv(int conv) {
        kcp.setConv(conv);
    }

    /**
     * Returns {@code true} if and only if nodelay is enabled.
     *
     * @return {@code true} if and only if nodelay is enabled
     */
    public boolean isNodelay() {
        return kcp.isNodelay();
    }

    /**
     * Sets whether enable nodelay.
     *
     * @param nodelay {@code true} if enable nodelay
     * @return this object
     */
    public Ukcp setNodelay(boolean nodelay) {
        kcp.setNodelay(nodelay);
        return this;
    }

    /**
     * Returns update interval.
     *
     * @return update interval
     */
    public int getInterval() {
        return kcp.getInterval();
    }

    /**
     * Sets update interval
     *
     * @param interval update interval
     * @return this object
     */
    public Ukcp setInterval(int interval) {
        kcp.setInterval(interval);
        return this;
    }

    /**
     * Returns the fastresend of kcp.
     *
     * @return the fastresend of kcp
     */
    public int getFastResend() {
        return kcp.getFastresend();
    }

    /**
     * Sets the fastresend of kcp.
     *
     * @param fastResend
     * @return this object
     */
    public Ukcp setFastResend(int fastResend) {
        kcp.setFastresend(fastResend);
        return this;
    }

    public boolean isNocwnd() {
        return kcp.isNocwnd();
    }

    public Ukcp setNocwnd(boolean nocwnd) {
        kcp.setNocwnd(nocwnd);
        return this;
    }

    public int getMinRto() {
        return kcp.getRxMinrto();
    }

    public Ukcp setMinRto(int minRto) {
        kcp.setRxMinrto(minRto);
        return this;
    }

    public int getMtu() {
        return kcp.getMtu();
    }

    public Ukcp setMtu(int mtu) {
        kcp.setMtu(mtu);
        return this;
    }

    public boolean isStream() {
        return kcp.isStream();
    }

    public Ukcp setStream(boolean stream) {
        kcp.setStream(stream);
        return this;
    }

    public int getDeadLink() {
        return kcp.getDeadLink();
    }


    /**
     * Sets the {@link ByteBufAllocator} which is used for the kcp to allocate buffers.
     *
     * @param allocator the allocator is used for the kcp to allocate buffers
     * @return this object
     */
    public Ukcp setByteBufAllocator(ByteBufAllocator allocator) {
        kcp.setByteBufAllocator(allocator);
        return this;
    }

    public boolean isFastFlush() {
        return fastFlush;
    }


    public Ukcp setAckNoDelay(boolean ackNoDelay) {
        this.kcp.setAckNoDelay(ackNoDelay);
        return this;
    }


    protected void read(ByteBuf byteBuf) {
        if(this.readQueue.offer(byteBuf)){
            notifyReadEvent();
        }else{
            byteBuf.release();
            log.error("conv "+kcp.getConv()+" recieveList is full");
        }
    }

    /**
     * 发送有序可靠消息
     * 线程安全的
     * @param byteBuf 发送后需要手动释放
     * @return
     */
    public void writeMessage(ByteBuf byteBuf) {
        byteBuf = byteBuf.retainedDuplicate();
        if (!writeQueue.offer(byteBuf)) {
            log.error("conv "+kcp.getConv()+" sendList is full");
            byteBuf.release();
            notifyCloseEvent();
        }
        notifyWriteEvent();
    }





    /**
     * 主动关闭连接调用
     */
    public void notifyCloseEvent() {
        this.iMessageExecutor.execute(() -> close());
    }

    private void notifyReadEvent() {
        if(readProcessing.compareAndSet(false,true)){
            ReadTask readTask = ReadTask.New(this);
            this.iMessageExecutor.execute(readTask);
        }
    }

    protected void notifyWriteEvent() {
        if(writeProcessing.compareAndSet(false,true)){
            WriteTask writeTask = WriteTask.New(this);
            this.iMessageExecutor.execute(writeTask);
        }
    }


    public long getTsUpdate() {
        return tsUpdate;
    }

    protected Queue<ByteBuf> getReadQueue() {
        return readQueue;
    }

    protected Ukcp setTsUpdate(long tsUpdate) {
        this.tsUpdate = tsUpdate;
        return this;
    }

    public int getState() {
        return kcp.getState();
    }


    protected Queue<ByteBuf> getWriteQueue() {
        return writeQueue;
    }

    protected KcpListener getKcpListener() {
        return kcpListener;
    }

    public boolean isActive() {
        return active;
    }


    void close() {
        if(!active){
            return;
        }
        kcpListener.handleClose(this);
        this.active = false;
        //抛回网络线程处理连接删除
        user().getChannel().eventLoop().execute(() -> channelManager.del(this));
        release();
    }

    void release() {
        kcp.setState(-1);
        kcp.release();
        for (; ; ) {
            ByteBuf byteBuf = writeQueue.poll();
            if (byteBuf == null) {
                break;
            }
            byteBuf.release();
        }
        for (; ; ) {
            ByteBuf byteBuf = readQueue.poll();
            if (byteBuf == null) {
                break;
            }
            byteBuf.release();
        }
        if (this.fecEncode != null) {
            this.fecEncode.release();
        }

        if (this.fecDecode != null) {
            this.fecDecode.release();
        }
    }

    protected AtomicBoolean getWriteProcessing() {
        return writeProcessing;
    }


    protected AtomicBoolean getReadProcessing() {
        return readProcessing;
    }


    public ChannelConfig getChannelConfig() {
        return channelConfig;
    }

    @SuppressWarnings("unchecked")
    public User user() {
        return (User) kcp.getUser();
    }

    public Ukcp user(User user) {
        kcp.setUser(user);
        return this;
    }

    @Override
    public String toString() {
        return "Ukcp(" +
                "getConv=" + kcp.getConv() +
                ", state=" + kcp.getState() +
                ", active=" + active +
                ')';
    }
}
