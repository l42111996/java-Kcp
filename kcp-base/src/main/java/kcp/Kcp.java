package kcp;

import com.backblaze.erasure.fec.Snmp;
import internal.ReItrLinkedList;
import internal.ReusableListIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Java implementation of <a href="https://github.com/skywind3000/kcp">KCP</a>
 *
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class Kcp {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(Kcp.class);

    /**
     * no delay min rto
     */
    public static final int IKCP_RTO_NDL = 30;

    /**
     * normal min rto
     */
    public static final int IKCP_RTO_MIN = 100;

    public static final int IKCP_RTO_DEF = 200;

    public static final int IKCP_RTO_MAX = 60000;

    /**
     * cmd: push data
     */
    public static final byte IKCP_CMD_PUSH = 81;

    /**
     * cmd: ack
     */
    public static final byte IKCP_CMD_ACK = 82;

    /**
     * cmd: window probe (ask)
     * 询问对方当前剩余窗口大小 请求
     */
    public static final byte IKCP_CMD_WASK = 83;

    /**
     * cmd: window size (tell)
     * 返回本地当前剩余窗口大小
     */
    public static final byte IKCP_CMD_WINS = 84;

    /**
     * need to send IKCP_CMD_WASK
     */
    public static final int IKCP_ASK_SEND = 1;

    /**
     * need to send IKCP_CMD_WINS
     */
    public static final int IKCP_ASK_TELL = 2;

    public static final int IKCP_WND_SND = 32;

    public static final int IKCP_WND_RCV = 32;

    public static final int IKCP_MTU_DEF = 1400;

    public static final int IKCP_INTERVAL = 100;

    public static final int IKCP_OVERHEAD = 24;

    public static final int IKCP_DEADLINK = 20;

    public static final int IKCP_THRESH_INIT = 2;

    public static final int IKCP_THRESH_MIN = 2;

    /**头跳过长度**/
    public static final int IKCP_SKIP_HEADSIZE = 8;

    /**
     * 7 secs to probe window size
     */
    public static final int IKCP_PROBE_INIT = 7000;

    /**
     * up to 120 secs to probe window
     */
    public static final int IKCP_PROBE_LIMIT = 120000;
    /**会话id**/
    private int conv;
    /**最大传输单元**/
    private int mtu = IKCP_MTU_DEF;

    private int mss = this.mtu - IKCP_OVERHEAD;
    /**状态**/
    private int state;
    /**已发送但未确认**/
    private long sndUna;
    /**下次发送下标**/
    private long sndNxt;
    /**下次接收下标**/
    private long rcvNxt;
    /**上次ack时间**/
    private long tsLastack;
    /**慢启动门限**/
    private int ssthresh = IKCP_THRESH_INIT;
    /**RTT(Round Trip Time)**/
    private int rxRttval;
    /**SRTT平滑RTT*/
    private int rxSrtt;
    /**RTO重传超时*/
    private int rxRto = IKCP_RTO_DEF;
    /**MinRTO最小重传超时*/
    private int rxMinrto = IKCP_RTO_MIN;
    /**发送窗口**/
    private int sndWnd = IKCP_WND_SND;
    /**接收窗口**/
    private int rcvWnd = IKCP_WND_RCV;
    /**当前对端可接收窗口**/
    private int rmtWnd = IKCP_WND_RCV;
    /**拥塞控制窗口**/
    private int cwnd;
    /**探测标志位**/
    private int probe;
    ///**当前时间**/
    //private long current;
    /**间隔**/
    private int interval = IKCP_INTERVAL;
    /**发送**/
    private long tsFlush = IKCP_INTERVAL;
    /**是否无延迟 0不启用；1启用**/
    private boolean nodelay;
    /**状态是否已更新**/
    private boolean updated;
    /**探测时间**/
    private long tsProbe;
    /**探测等待**/
    private int probeWait;
    /**死连接 重传达到该值时认为连接是断开的**/
    private int deadLink = IKCP_DEADLINK;
    /**拥塞控制增量**/
    private int incr;
    /**收到包立即回ack**/
    private boolean ackNoDelay;


    private LinkedList<Segment> sndQueue = new LinkedList<>();
    /**收到后有序的队列**/
    private ReItrLinkedList<Segment> rcvQueue = new ReItrLinkedList<>();

    private ReItrLinkedList<Segment> sndBuf = new ReItrLinkedList<>();
    /**收到的消息 无序的**/
    private ReItrLinkedList<Segment> rcvBuf = new ReItrLinkedList<>();

    private ReusableListIterator<Segment> rcvQueueItr = rcvQueue.listIterator();

    private ReusableListIterator<Segment> sndBufItr = sndBuf.listIterator();

    private ReusableListIterator<Segment> rcvBufItr = rcvBuf.listIterator();

    private long[] acklist = new long[8];

    private int ackcount;

    private Object user;
    /**是否快速重传 默认0关闭，可以设置2（2次ACK跨越将会直接重传）**/
    private int fastresend;
    /**是否关闭拥塞控制窗口**/
    private boolean nocwnd;
    /**是否流传输**/
    private boolean stream;

    private KcpOutput output;

    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    /**
     * automatically set conv
     */
    private boolean autoSetConv;

    private static long long2Uint(long n) {
        return n & 0x00000000FFFFFFFFL;
    }

    private static int ibound(int lower, int middle, int upper) {
        return Math.min(Math.max(lower, middle), upper);
    }

    private static long ibound(long lower, long middle, long upper) {
        return Math.min(Math.max(lower, middle), upper);
    }

    private static int itimediff(int later, int earlier) {
        return later - earlier;
    }

    private static int itimediff(long later, long earlier) {
        return (int) (later - earlier);
    }

    private static void output(ByteBuf data, Kcp kcp) {
        if (log.isDebugEnabled()) {
            log.debug("{} [RO] {} bytes", kcp, data.readableBytes());
        }
        if (data.readableBytes() == 0) {
            return;
        }
        kcp.output.out(data, kcp);
    }

    private static int encodeSeg(ByteBuf buf, Segment seg) {
        int offset = buf.writerIndex();

        buf.writeIntLE(seg.conv);
        buf.writeByte(seg.cmd);
        buf.writeByte(seg.frg);
        buf.writeShortLE(seg.wnd);
        buf.writeIntLE((int) seg.ts);
        buf.writeIntLE((int) seg.sn);
        buf.writeIntLE((int) seg.una);
        buf.writeIntLE(seg.data.readableBytes());

        Snmp.snmp.OutSegs.incrementAndGet();
        return buf.writerIndex() - offset;
    }

    private static class Segment {

        private final Recycler.Handle<Segment> recyclerHandle;
        /**会话id**/
        private int conv;
        /**命令**/
        private byte cmd;
        /**message中的segment分片ID（在message中的索引，由大到小，0表示最后一个分片）**/
        private short frg;
        /**剩余接收窗口大小(接收窗口大小-接收队列大小)**/
        private int wnd;
        /**message发送时刻的时间戳**/
        private long ts;
        /**message分片segment的序号**/
        private long sn;
        /**待接收消息序号(接收滑动窗口左端)**/
        private long una;
        /**下次超时重传的时间戳**/
        private long resendts;
        /**该分片的超时重传等待时间**/
        private int rto;
        /**收到ack时计算的该分片被跳过的累计次数，即该分片后的包都被对方收到了，达到一定次数，重传当前分片**/
        private int fastack;
        /***发送分片的次数，每发送一次加一**/
        private int xmit;

        private ByteBuf data;

        private static final Recycler<Segment> RECYCLER = new Recycler<Segment>() {

            @Override
            protected Segment newObject(Handle<Segment> handle) {
                return new Segment(handle);
            }

        };

        private Segment(Recycler.Handle<Segment> recyclerHandle) {
            this.recyclerHandle = recyclerHandle;
        }

        void recycle(boolean releaseBuf) {
            conv = 0;
            cmd = 0;
            frg = 0;
            wnd = 0;
            ts = 0;
            sn = 0;
            una = 0;
            resendts = 0;
            rto = 0;
            fastack = 0;
            xmit = 0;
            if (releaseBuf) {
                data.release();
            }
            data = null;

            recyclerHandle.recycle(this);
        }

        static Segment createSegment(ByteBufAllocator byteBufAllocator, int size) {
            Segment seg = RECYCLER.get();
            if (size == 0) {
                seg.data = byteBufAllocator.ioBuffer(0, 0);
            } else {
                seg.data = byteBufAllocator.ioBuffer(size);
            }
            return seg;
        }

        static Segment createSegment(ByteBuf buf) {
            Segment seg = RECYCLER.get();
            seg.data = buf;
            return seg;
        }

    }

    public Kcp(int conv, KcpOutput output) {
        this.conv = conv;
        this.output = output;
    }

    public void release() {
        release(sndBuf);
        release(rcvBuf);
        release(sndQueue);
        release(rcvQueue);
    }

    private void release(List<Segment> segQueue) {
        for (Segment seg : segQueue) {
            seg.recycle(true);
        }
    }

    private ByteBuf createByteBuf() {
        ByteBuf byteBuf = byteBufAllocator.ioBuffer((this.mtu + IKCP_OVERHEAD) * 3);
        return byteBuf;
    }


    /**
     * 1，判断是否有完整的包，如果有就抛给下一层
     * 2，整理消息接收队列，判断下一个包是否已经收到 收到放入rcvQueue
     * 3，判断接收窗口剩余是否改变，如果改变记录需要通知
     * @param bufList
     * @return
     */
    public int recv(List<ByteBuf> bufList) {
        if (rcvQueue.isEmpty()) {
            return -1;
        }
        int peekSize = peekSize();

        if (peekSize < 0) {
            return -2;
        }
        //接收队列长度大于接收窗口？比如接收窗口是32个包，目前已经满32个包了，需要在恢复的时候告诉对方
        boolean recover = false;
        if (rcvQueue.size() >= rcvWnd) {
            recover = true;
        }

        // merge fragment
        int count = 0;
        int len = 0;
        for (Iterator<Segment> itr = rcvQueueItr.rewind(); itr.hasNext(); ) {
            Segment seg = itr.next();
            len += seg.data.readableBytes();
            bufList.add(seg.data);

            int fragment = seg.frg;

            // log
            if (log.isDebugEnabled()) {
                log.debug("{} recv sn={}", this, seg.sn);
            }

            itr.remove();
            seg.recycle(false);

            if (fragment == 0) {
                break;
            }
        }

        assert len == peekSize;

        // move available data from rcv_buf -> rcv_queue
        moveRcvData();

        // fast recover接收队列长度小于接收窗口，说明还可以接数据，已经恢复了，在下次发包的时候告诉对方本方的窗口
        if (rcvQueue.size() < rcvWnd && recover) {
            // ready to send back IKCP_CMD_WINS in ikcp_flush
            // tell remote my window size
            probe |= IKCP_ASK_TELL;
        }

        return len;
    }

    /**
     * check the size of next message in the recv queue
     * 检查接收队列里面是否有完整的一个包，如果有返回该包的字节长度
     * @return -1 没有完整包， >0 一个完整包所含字节
     */
    public int peekSize() {
        if (rcvQueue.isEmpty()) {
            return -1;
        }

        Segment seg = rcvQueue.peek();
        //第一个包是一条应用层消息的最后一个分包？一条消息只有一个包的情况？
        if (seg.frg == 0) {
            return seg.data.readableBytes();
        }
        //接收队列长度小于应用层消息分包数量？接收队列空间不够用于接收完整的一个消息？
        if (rcvQueue.size() < seg.frg + 1) { // Some segments have not arrived yet
            return -1;
        }

        int len = 0;
        for (Iterator<Segment> itr = rcvQueueItr.rewind(); itr.hasNext(); ) {
            Segment s = itr.next();
            len += s.data.readableBytes();
            if (s.frg == 0) {
                break;
            }
        }

        return len;
    }

    /**
     * 判断一条消息是否完整收全了
     * @return
     */
    public boolean canRecv() {
        if (rcvQueue.isEmpty()) {
            return false;
        }

        Segment seg = rcvQueue.peek();
        if (seg.frg == 0) {
            return true;
        }

        if (rcvQueue.size() < seg.frg + 1) { // Some segments have not arrived yet
            return false;
        }

        return true;
    }


    public int send(ByteBuf buf) {
        assert mss > 0;

        int len = buf.readableBytes();
        if (len == 0) {
            return -1;
        }

        // append to previous segment in streaming mode (if possible)
        if (stream) {
            if (!sndQueue.isEmpty()) {
                Segment last = sndQueue.peekLast();
                if (len < mss) {
                    ByteBuf lastData = last.data;
                    int capacity = mss - lastData.readableBytes();
                    int extend = len < capacity ? len : capacity;
                    if (lastData.maxWritableBytes() < extend) { // extend
                        ByteBuf newBuf = byteBufAllocator.ioBuffer(lastData.readableBytes() + extend);
                        newBuf.writeBytes(lastData);
                        lastData.release();
                        lastData = last.data = newBuf;
                    }
                    lastData.writeBytes(buf, extend);

                    len = buf.readableBytes();
                    if (len == 0) {
                        return 0;
                    }
                }
            }
        }

        int count = 0;
        if (len <= mss) {
            count = 1;
        } else {
            count = (len + mss - 1) / mss;
        }

        if (count > 255) { // Maybe don't need the conditon in stream mode
            return -2;
        }

        if (count == 0) { // impossible
            count = 1;
        }

        // segment
        for (int i = 0; i < count; i++) {
            int size = len > mss ? mss : len;
            Segment seg = Segment.createSegment(buf.readRetainedSlice(size));
            seg.frg = (short) (stream ? 0 : count - i - 1);
            sndQueue.add(seg);
            len = buf.readableBytes();
        }

        return 0;
    }

    /**
     * update ack.
     * parse ack根据RTT计算SRTT和RTO即重传超时
     * @param rtt
     */
    private void updateAck(int rtt) {
        if (rxSrtt == 0) {
            rxSrtt = rtt;
            rxRttval = rtt / 2;
        } else {
            int delta = rtt - rxSrtt;
            rxSrtt += delta>>3;
            delta = Math.abs(delta);
            if (rtt < rxSrtt - rxRttval) {
                rxRttval += ( delta - rxRttval)>>5;
            } else {
                rxRttval += (delta - rxRttval) >>2;
            }
            //int delta = rtt - rxSrtt;
            //if (delta < 0) {
            //    delta = -delta;
            //}
            //rxRttval = (3 * rxRttval + delta) / 4;
            //rxSrtt = (7 * rxSrtt + rtt) / 8;
            //if (rxSrtt < 1) {
            //    rxSrtt = 1;
            //}
        }
        int rto = rxSrtt + Math.max(interval, 4 * rxRttval);
        rxRto = ibound(rxMinrto, rto, IKCP_RTO_MAX);
    }

    private void shrinkBuf() {
        if (sndBuf.size() > 0) {
            Segment seg = sndBuf.peek();
            sndUna = seg.sn;
        } else {
            sndUna = sndNxt;
        }
    }

    private void parseAck(long sn) {
        if (itimediff(sn, sndUna) < 0 || itimediff(sn, sndNxt) >= 0) {
            return;
        }

        for (Iterator<Segment> itr = sndBufItr.rewind(); itr.hasNext(); ) {
            Segment seg = itr.next();
            if (sn == seg.sn) {
                itr.remove();
                seg.recycle(true);
                break;
            }
            if (itimediff(sn, seg.sn) < 0) {
                break;
            }
        }
    }

    private void parseUna(long una) {
        for (Iterator<Segment> itr = sndBufItr.rewind(); itr.hasNext(); ) {
            Segment seg = itr.next();
            if (itimediff(una, seg.sn) > 0) {
                itr.remove();
                seg.recycle(true);
            } else {
                break;
            }
        }
    }

    private void parseFastack(long sn) {
        if (itimediff(sn, sndUna) < 0 || itimediff(sn, sndNxt) >= 0) {
            return;
        }

        for (Iterator<Segment> itr = sndBufItr.rewind(); itr.hasNext(); ) {
            Segment seg = itr.next();
            if (itimediff(sn, seg.sn) < 0) {
                break;
            } else if (sn != seg.sn) {
                seg.fastack++;
            }
        }
    }

    private void ackPush(long sn, long ts) {
        int newSize = 2 * (ackcount + 1);

        if (newSize > acklist.length) {
            int newCapacity = acklist.length << 1; // double capacity

            if (newCapacity < 0) {
                throw new OutOfMemoryError();
            }

            long[] newArray = new long[newCapacity];
            System.arraycopy(acklist, 0, newArray, 0, acklist.length);
            this.acklist = newArray;
        }

        acklist[2 * ackcount] =  sn;
        acklist[2 * ackcount + 1] =  ts;
        ackcount++;
    }

    private void parseData(Segment newSeg) {
        long sn = newSeg.sn;

        if (itimediff(sn, rcvNxt + rcvWnd) >= 0 || itimediff(sn, rcvNxt) < 0) {
            newSeg.recycle(true);
            return;
        }

        boolean repeat = false;
        boolean findPos = false;
        ListIterator<Segment> listItr = null;
        if (rcvBuf.size() > 0) {
            listItr = rcvBufItr.rewind(rcvBuf.size());
            while (listItr.hasPrevious()) {
                Segment seg = listItr.previous();
                if (seg.sn == sn) {
                    repeat = true;
                    Snmp.snmp.RepeatSegs.incrementAndGet();
                    break;
                }
                if (itimediff(sn, seg.sn) > 0) {
                    findPos = true;
                    break;
                }
            }
        }

        if (repeat) {
            newSeg.recycle(true);
        } else if (listItr == null) {
            rcvBuf.add(newSeg);
        } else {
            if (findPos) {
                listItr.next();
            }
            listItr.add(newSeg);
        }

        // move available data from rcv_buf -> rcv_queue
        moveRcvData(); // Invoke the method only if the segment is not repeat?
    }

    private void moveRcvData() {
        for (Iterator<Segment> itr = rcvBufItr.rewind(); itr.hasNext(); ) {
            Segment seg = itr.next();
            if (seg.sn == rcvNxt && rcvQueue.size() < rcvWnd) {
                itr.remove();
                rcvQueue.add(seg);
                rcvNxt++;
            } else {
                break;
            }
        }
    }

    @Deprecated
    private int input1(ByteBuf data,long current) {
        long oldSndUna = sndUna;
        long maxack = 0;
        boolean flag = false;

        if (log.isDebugEnabled()) {
            log.debug("{} [RI] {} bytes", this, data.readableBytes());
        }

        if (data == null || data.readableBytes() < IKCP_OVERHEAD) {
            return -1;
        }

        while (true) {
            int conv, len, wnd;
            long ts, sn, una;
            byte cmd;
            short frg;
            Segment seg;

            if (data.readableBytes() < IKCP_OVERHEAD) {
                break;
            }

            conv = data.readIntLE();
            if (conv != this.conv && !(this.conv == 0 && autoSetConv)) {
                return -4;
            }

            cmd = data.readByte();
            frg = data.readUnsignedByte();
            wnd = data.readUnsignedShortLE();
            ts = data.readUnsignedIntLE();
            sn = data.readUnsignedIntLE();
            una = data.readUnsignedIntLE();
            len = data.readIntLE();

            if (data.readableBytes() < len) {
                return -2;
            }

            if (cmd != IKCP_CMD_PUSH && cmd != IKCP_CMD_ACK && cmd != IKCP_CMD_WASK && cmd != IKCP_CMD_WINS) {
                return -3;
            }

            if (this.conv == 0 && autoSetConv) { // automatically set conv
                this.conv = conv;
            }

            this.rmtWnd = wnd;
            parseUna(una);
            shrinkBuf();

            boolean readed = false;
            long uintCurrent = long2Uint(current);
            switch (cmd) {
                case IKCP_CMD_ACK: {
                    int rtt = itimediff(uintCurrent, ts);
                    if (rtt >= 0) {
                        updateAck(rtt);
                    }
                    parseAck(sn);
                    shrinkBuf();
                    if (!flag) {
                        flag = true;
                        maxack = sn;
                    } else {
                        if (itimediff(sn, maxack) > 0) {
                            maxack = sn;
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("{} input ack: sn={}, rtt={}, rto={}", this, sn, rtt, rxRto);
                    }
                    break;
                }
                case IKCP_CMD_PUSH: {
                    if (itimediff(sn, rcvNxt + rcvWnd) < 0) {
                        ackPush(sn, ts);
                        if (itimediff(sn, rcvNxt) >= 0) {
                            seg = Segment.createSegment(byteBufAllocator, len);
                            seg.conv = conv;
                            seg.cmd = cmd;
                            seg.frg = frg;
                            seg.wnd = wnd;
                            seg.ts = ts;
                            seg.sn = sn;
                            seg.una = una;

                            if (len > 0) {
                                seg.data.writeBytes(data, len);
                                readed = true;
                            }

                            parseData(seg);
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("{} input push: sn={}, una={}, ts={}", this, sn, una, ts);
                    }
                    break;
                }
                case IKCP_CMD_WASK: {
                    // ready to send back IKCP_CMD_WINS in ikcp_flush
                    // tell remote my window size
                    probe |= IKCP_ASK_TELL;
                    if (log.isDebugEnabled()) {
                        log.debug("{} input ask", this);
                    }
                    break;
                }
                case IKCP_CMD_WINS: {
                    // do nothing
                    if (log.isDebugEnabled()) {
                        log.debug("{} input tell: {}", this, wnd);
                    }
                    break;
                }
                default:
                    return -3;
            }

            if (!readed) {
                data.skipBytes(len);
            }
        }

        if (flag) {
            parseFastack(maxack);
        }

        if (itimediff(sndUna, oldSndUna) > 0) {
            if (cwnd < rmtWnd) {
                int mss = this.mss;
                if (cwnd < ssthresh) {
                    cwnd++;
                    incr += mss;
                } else {
                    if (incr < mss) {
                        incr = mss;
                    }
                    incr += (mss * mss) / incr + (mss / 16);
                    if ((cwnd + 1) * mss <= incr) {
                        cwnd++;
                    }
                }
                if (cwnd > rmtWnd) {
                    cwnd = rmtWnd;
                    incr = rmtWnd * mss;
                }
            }
        }

        return 0;
    }

    public int input(ByteBuf data, boolean regular,long current) {
        long oldSndUna = sndUna;
        long maxack = 0;
        boolean flag = false;
        long inSegs = 0;

        if (log.isDebugEnabled()) {
            log.debug("{} [RI] {} bytes", this, data.readableBytes());
        }

        if (data == null || data.readableBytes() < IKCP_OVERHEAD) {
            return -1;
        }

        long lastackts=0;
        long uintCurrent = long2Uint(current);

        while (true) {
            int conv, len, wnd;
            long ts, sn, una;
            byte cmd;
            short frg;
            Segment seg;

            if (data.readableBytes() < IKCP_OVERHEAD) {
                break;
            }

            conv = data.readIntLE();
            if (conv != this.conv && !(this.conv == 0 && autoSetConv)) {
                return -4;
            }

            cmd = data.readByte();
            frg = data.readUnsignedByte();
            wnd = data.readUnsignedShortLE();
            ts = data.readUnsignedIntLE();
            sn = data.readUnsignedIntLE();
            una = data.readUnsignedIntLE();
            len = data.readIntLE();

            if (data.readableBytes() < len) {
                return -2;
            }

            if (cmd != IKCP_CMD_PUSH && cmd != IKCP_CMD_ACK && cmd != IKCP_CMD_WASK && cmd != IKCP_CMD_WINS) {
                return -3;
            }

            if (this.conv == 0 && autoSetConv) { // automatically set conv
                this.conv = conv;
            }

            //最后收到的来计算远程窗口大小
            if (regular) {
                this.rmtWnd = wnd ;//更新远端窗口大小删除已确认的包，una以前的包对方都收到了，可以把本地小于una的都删除掉
            }

            //this.rmtWnd = wnd;
            parseUna(una);
            shrinkBuf();

            boolean readed = false;
            switch (cmd) {
                case IKCP_CMD_ACK: {
                    int rtt = itimediff(uintCurrent, ts);
                    //if (rtt >= 0) {
                    //    updateAck(rtt);
                    //}
                    parseAck(sn);
                    shrinkBuf();
                    if (!flag) {
                        flag = true;
                        maxack = sn;
                        lastackts = ts;
                    } else if (itimediff(sn, maxack) > 0) {
                        maxack = sn;
                        lastackts = ts;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("{} input ack: sn={}, rtt={}, rto={} ,regular={}", this, sn, rtt, rxRto,regular);
                    }
                    break;
                }
                case IKCP_CMD_PUSH: {
                    if (itimediff(sn, rcvNxt + rcvWnd) < 0) {
                        ackPush(sn, ts);
                        if (itimediff(sn, rcvNxt) >= 0) {
                            if (len > 0) {
                                seg = Segment.createSegment(data.readRetainedSlice(len));
                                readed = true;
                            } else {
                                seg = Segment.createSegment(byteBufAllocator, 0);
                            }
                            seg.conv = conv;
                            seg.cmd = cmd;
                            seg.frg = frg;
                            seg.wnd = wnd;
                            seg.ts = ts;
                            seg.sn = sn;
                            seg.una = una;

                            parseData(seg);
                        }else{
                            Snmp.snmp.RepeatSegs.incrementAndGet();
                        }
                    }else{
                        Snmp.snmp.RepeatSegs.incrementAndGet();
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("{} input push: sn={}, una={}, ts={},regular={}", this, sn, una, ts,regular);
                    }
                    break;
                }
                case IKCP_CMD_WASK: {
                    // ready to send back IKCP_CMD_WINS in ikcp_flush
                    // tell remote my window size
                    probe |= IKCP_ASK_TELL;
                    if (log.isDebugEnabled()) {
                        log.debug("{} input ask", this);
                    }
                    break;
                }
                case IKCP_CMD_WINS: {
                    // do nothing
                    if (log.isDebugEnabled()) {
                        log.debug("{} input tell: {}", this, wnd);
                    }
                    break;
                }
                default:
                    return -3;
            }

            if (!readed) {
                data.skipBytes(len);
            }
            inSegs++;
        }

        Snmp.snmp.InSegs.addAndGet(inSegs);

        if (flag && regular) {
            parseFastack(maxack);
            int rtt = itimediff(uintCurrent, lastackts);
            if (rtt >= 0) {
                updateAck(rtt);//收到ack包，根据ack包的时间计算srtt和rto
            }

        }

        if (itimediff(sndUna, oldSndUna) > 0) {
            if (cwnd < rmtWnd) {
                int mss = this.mss;
                if (cwnd < ssthresh) {
                    cwnd++;
                    incr += mss;
                } else {
                    if (incr < mss) {
                        incr = mss;
                    }
                    incr += (mss * mss) / incr + (mss / 16);
                    if ((cwnd + 1) * mss <= incr) {
                        cwnd++;
                    }
                }
                if (cwnd > rmtWnd) {
                    cwnd = rmtWnd;
                    incr = rmtWnd * mss;
                }
            }
        }


        if (ackNoDelay && ackcount > 0) { // ack immediately
            flush(true,current);
        }

        return 0;
    }

    private int wndUnused() {
        if (rcvQueue.size() < rcvWnd) {
            return rcvWnd - rcvQueue.size();
        }
        return 0;
    }

    /**
     * ikcp_flush
     */
    private long flush(boolean ackOnly,long current) {

        // 'ikcp_update' haven't been called.
        //if (!updated) {
        //    return;
        //}

        //long current = this.current;
        long uintCurrent = long2Uint(current);

        Segment seg = Segment.createSegment(byteBufAllocator, 0);
        seg.conv = conv;
        seg.cmd = IKCP_CMD_ACK;
        seg.frg = 0;
        seg.wnd = wndUnused();//可接收数量
        seg.una = rcvNxt;//已接收数量，下次要接收的包的sn，这sn之前的包都已经收到
        seg.sn = 0;
        seg.ts = 0;

        ByteBuf buffer = createByteBuf();


        boolean hasAck =false;
        // flush acknowledges有收到的包需要确认，则发确认包
        int count = ackcount;
        for (int i = 0; i < count; i++) {
            if (buffer.readableBytes() + IKCP_OVERHEAD > mtu) {
                output(buffer, this);
                buffer = createByteBuf();
                hasAck = false;
            }
            long sn =  acklist[i * 2];

            if (sn >= rcvNxt || count-1 == i) {
                hasAck = true;
                seg.sn = acklist[i * 2];
                seg.ts = acklist[i * 2 + 1];
                encodeSeg(buffer, seg);
                if (log.isDebugEnabled()) {
                    log.debug("{} flush ack: sn={}, ts={} ,count={}", this, seg.sn, seg.ts,count);
                }
            }
        }

        ackcount = 0;


        if(ackOnly&&hasAck){
            output(buffer, this);
            return interval;
        }

        // probe window size (if remote window size equals zero)
        //拥堵控制 如果对方可接受窗口大小为0  需要询问对方窗口大小
        if (rmtWnd == 0) {
            if (probeWait == 0) {
                probeWait = IKCP_PROBE_INIT;
                tsProbe = current + probeWait;
            } else {
                if (itimediff(current, tsProbe) >= 0) {
                    if (probeWait < IKCP_PROBE_INIT) {
                        probeWait = IKCP_PROBE_INIT;
                    }
                    probeWait += probeWait / 2;
                    if (probeWait > IKCP_PROBE_LIMIT) {
                        probeWait = IKCP_PROBE_LIMIT;
                    }
                    tsProbe = current + probeWait;
                    probe |= IKCP_ASK_SEND;
                }
            }
        } else {
            tsProbe = 0;
            probeWait = 0;
        }

        // flush window probing commands
        if ((probe & IKCP_ASK_SEND) != 0) {
            seg.cmd = IKCP_CMD_WASK;
            if (buffer.readableBytes() + IKCP_OVERHEAD > mtu) {
                output(buffer, this);
                buffer = createByteBuf();
            }
            encodeSeg(buffer, seg);
            if (log.isDebugEnabled()) {
                log.debug("{} flush ask", this);
            }
        }

        // flush window probing commands
        if ((probe & IKCP_ASK_TELL) != 0) {
            seg.cmd = IKCP_CMD_WINS;
            if (buffer.readableBytes() + IKCP_OVERHEAD > mtu) {
                output(buffer, this);
                buffer = createByteBuf();
            }
            encodeSeg(buffer, seg);
            if (log.isDebugEnabled()) {
                log.debug("{} flush tell: wnd={}", this, seg.wnd);
            }
        }

        probe = 0;

        // calculate window size
        int cwnd0 = Math.min(sndWnd, rmtWnd);
        if (!nocwnd) {
            cwnd0 = Math.min(this.cwnd, cwnd0);
        }

        int newSegsCount=0;
        // move data from snd_queue to snd_buf
        while (itimediff(sndNxt, sndUna + cwnd0) < 0) {
            Segment newSeg = sndQueue.poll();
            if (newSeg == null) {
                break;
            }
            newSeg.conv = conv;
            newSeg.cmd = IKCP_CMD_PUSH;
            newSeg.sn = sndNxt;
            sndNxt++;
            newSegsCount++;
            sndBuf.add(newSeg);
        }

        // calculate resent
        int resent = fastresend > 0 ? fastresend : Integer.MAX_VALUE;

        // flush data segments
        int change = 0;
        boolean lost = false;
        long lostSegs = 0, fastRetransSegs=0, earlyRetransSegs=0;
        long minrto = interval;


        for (Iterator<Segment> itr = sndBufItr.rewind(); itr.hasNext();) {
            Segment segment = itr.next();
            boolean needsend = false;
            if (segment.xmit == 0) {
                needsend = true;
                segment.rto = rxRto;
                segment.resendts = current + segment.rto ;
                if (log.isDebugEnabled()) {
                    log.debug("{} flush data: sn={}, resendts={}", this, segment.sn, (segment.resendts - current));
                }
            } else if (itimediff(current, segment.resendts) >= 0) {
                needsend = true;
                if (!nodelay) {
                    segment.rto = rxRto;
                } else {
                    segment.rto = rxRto / 2;
                }
                //go 版本的
                //if (!nodelay) {
                //    segment.rto += rxRto;
                //} else {
                //    segment.rto += rxRto / 2;
                //}
                segment.resendts = current + segment.rto;
                lost = true;
                lostSegs++;
                if (log.isDebugEnabled()) {
                    log.debug("{} resend. sn={}, xmit={}, resendts={}", this, segment.sn, segment.xmit, (segment
                            .resendts - current));
                }
            } else if (segment.fastack >= resent) {
                needsend = true;
                segment.fastack = 0;
                segment.rto = rxRto;
                segment.resendts = current + segment.rto;
                change++;
                fastRetransSegs++;
                if (log.isDebugEnabled()) {
                    log.debug("{} fastresend. sn={}, xmit={}, resendts={} ", this, segment.sn, segment.xmit, (segment
                            .resendts - current));
                }
            }
            //else if(segment.fastack>0 &&newSegsCount==0){  // early retransmit
            //    needsend = true;
            //    segment.fastack = 0;
            //    segment.rto = rxRto;
            //    segment.resendts = current + segment.rto;
            //    change++;
            //    earlyRetransSegs++;
            //}


            if (needsend) {
                segment.xmit++;
                segment.ts = uintCurrent;
                segment.wnd = seg.wnd;
                segment.una = rcvNxt;

                ByteBuf segData = segment.data;
                int segLen = segData.readableBytes();
                int need = IKCP_OVERHEAD + segLen;

                if (buffer.readableBytes() + need > mtu) {
                    output(buffer, this);
                    buffer = createByteBuf();
                }

                encodeSeg(buffer, segment);

                if (segLen > 0) {
                    // don't increases data's readerIndex, because the data may be resend.
                    buffer.writeBytes(segData, segData.readerIndex(), segLen);
                }

                if (segment.xmit >= deadLink) {
                    state = -1;
                }

                // get the nearest rto
                long rto = itimediff(segment.rto, current);
                if(rto>0 &&rto<minrto){
                    minrto = rto;
                }
            }
        }

        // flash remain segments
        if (buffer.readableBytes() > 0) {
            output(buffer, this);
        } else {
            buffer.release();
        }

        seg.recycle(true);


        long sum = lostSegs;
        if (lostSegs > 0) {
            Snmp.snmp.LostSegs.addAndGet(lostSegs);
        }
        if (fastRetransSegs > 0) {
            Snmp.snmp.FastRetransSegs.addAndGet(fastRetransSegs);
            sum += fastRetransSegs;
        }
        if (earlyRetransSegs > 0) {
            Snmp.snmp.EarlyRetransSegs.addAndGet(earlyRetransSegs);
            sum += earlyRetransSegs;
        }
        if (sum > 0) {
            Snmp.snmp.RetransSegs.addAndGet(sum);
        }
        // update ssthresh
        if (change > 0) {
            int inflight = (int) (sndNxt - sndUna);
            ssthresh = inflight / 2;
            if (ssthresh < IKCP_THRESH_MIN) {
                ssthresh = IKCP_THRESH_MIN;
            }
            cwnd = ssthresh + resent;
            incr = cwnd * mss;
        }

        if (lost) {
            ssthresh = cwnd0 / 2;
            if (ssthresh < IKCP_THRESH_MIN) {
                ssthresh = IKCP_THRESH_MIN;
            }
            cwnd = 1;
            incr = mss;
        }

        if (cwnd < 1) {
            cwnd = 1;
            incr = mss;
        }
        return minrto;
    }

    /**
     * update getState (call it repeatedly, every 10ms-100ms), or you can ask
     * ikcp_check when to call it again (without ikcp_input/_send calling).
     * 'current' - current timestamp in millisec.
     *
     * @param current
     */
    public void update(long current) {
        //this.current = current;

        if (!updated) {
            updated = true;
            tsFlush = current;
        }

        int slap = itimediff(current, tsFlush);

        if (slap >= 10000 || slap < -10000) {
            tsFlush = current;
            slap = 0;
        }

        //if (slap >= 0) {
        //    tsFlush += interval;
        //    if (itimediff(current, tsFlush) >= 0) {
        //        tsFlush = current + interval;
        //    }
        //    flush(false,current);
        //}

        if (slap >= 0) {
            tsFlush += interval;
            if (itimediff(current, tsFlush) >= 0) {
                tsFlush = current + interval;
            }
        } else {
            tsFlush = current + interval;
        }

        flush(false,current);
    }

    /**
     * Determine when should you invoke ikcp_update:
     * returns when you should invoke ikcp_update in millisec, if there
     * is no ikcp_input/_send calling. you can call ikcp_update in that
     * time, instead of call update repeatly.
     * Important to reduce unnacessary ikcp_update invoking. use it to
     * schedule ikcp_update (eg. implementing an epoll-like mechanism,
     * or optimize ikcp_update when handling massive kcp connections)
     *
     * @param current
     * @return
     */
    public long check(long current) {
        if (!updated) {
            return current;
        }

        long tsFlush = this.tsFlush;
        int slap = itimediff(current, tsFlush);
        if (slap >= 10000 || slap < -10000) {
            tsFlush = current;
            slap = 0;
        }

        if (slap >= 0) {
            return current;
        }

        int tmFlush = itimediff(tsFlush, current);
        int tmPacket = Integer.MAX_VALUE;

        for (Iterator<Segment> itr = sndBufItr.rewind(); itr.hasNext();) {
            Segment seg = itr.next();
            int diff = itimediff(seg.resendts, current);
            if (diff <= 0) {
                return current;
            }
            if (diff < tmPacket) {
                tmPacket = diff;
            }
        }

        int minimal = tmPacket < tmFlush ? tmPacket : tmFlush;
        if (minimal >= interval) {
            minimal = interval;
        }
        return current + minimal;
    }

    public boolean checkFlush() {
        if (ackcount > 0) {
            return true;
        }
        if (probe != 0) {
            return true;
        }
        if (sndBuf.size() > 0) {
            return true;
        }
        if (sndQueue.size() > 0) {
            return true;
        }
        return false;
    }

    public int getMtu() {
        return mtu;
    }

    public int setMtu(int mtu) {
        if (mtu < IKCP_OVERHEAD || mtu < 50) {
            return -1;
        }

        this.mtu = mtu;
        this.mss = mtu - IKCP_OVERHEAD;
        return 0;
    }

    public int getInterval() {
        return interval;
    }

    public int setInterval(int interval) {
        if (interval > 5000) {
            interval = 5000;
        } else if (interval < 10) {
            interval = 10;
        }
        this.interval = interval;

        return 0;
    }

    public int nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        this.nodelay = nodelay;
        if (nodelay) {
            this.rxMinrto = IKCP_RTO_NDL;
        } else {
            this.rxMinrto = IKCP_RTO_MIN;
        }

        if (interval >= 0) {
            if (interval > 5000) {
                interval = 5000;
            } else if (interval < 10) {
                interval = 10;
            }
            this.interval = interval;
        }

        if (resend >= 0) {
            fastresend = resend;
        }

        this.nocwnd = nc;

        return 0;
    }

    public int waitSnd() {
        return this.sndBuf.size() + this.sndQueue.size();
    }

    public int getConv() {
        return conv;
    }

    public void setConv(int conv) {
        this.conv = conv;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isNodelay() {
        return nodelay;
    }

    public void setNodelay(boolean nodelay) {
        this.nodelay = nodelay;
        if (nodelay) {
            this.rxMinrto = IKCP_RTO_NDL;
        } else {
            this.rxMinrto = IKCP_RTO_MIN;
        }
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

    public int getRxMinrto() {
        return rxMinrto;
    }

    public void setRxMinrto(int rxMinrto) {
        this.rxMinrto = rxMinrto;
    }

    public int getRcvWnd() {
        return rcvWnd;
    }

    public void setRcvWnd(int rcvWnd) {
        this.rcvWnd = rcvWnd;
    }

    public int getSndWnd() {
        return sndWnd;
    }

    public void setSndWnd(int sndWnd) {
        this.sndWnd = sndWnd;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public int getDeadLink() {
        return deadLink;
    }

    public void setDeadLink(int deadLink) {
        this.deadLink = deadLink;
    }

    public void setByteBufAllocator(ByteBufAllocator byteBufAllocator) {
        this.byteBufAllocator = byteBufAllocator;
    }

    public boolean isAutoSetConv() {
        return autoSetConv;
    }

    public void setAutoSetConv(boolean autoSetConv) {
        this.autoSetConv = autoSetConv;
    }

    public KcpOutput getOutput() {
        return output;
    }

    public void setOutput(KcpOutput output) {
        this.output = output;
    }

    public boolean isAckNoDelay() {
        return ackNoDelay;
    }

    public void setAckNoDelay(boolean ackNoDelay) {
        this.ackNoDelay = ackNoDelay;
    }

    @Override
    public String toString() {
        return "Kcp(" +
                "conv=" + conv +
                ')';
    }

}
