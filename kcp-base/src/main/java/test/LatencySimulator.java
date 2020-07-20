package test;

import com.backblaze.erasure.fec.Snmp;
import internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.Kcp;
import kcp.IKcp;
import kcp.KcpOutput;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by JinMiao
 * 2019-01-04.
 */
public class LatencySimulator {

    private static long long2Uint(long n) {
        return n & 0x00000000FFFFFFFFL;
    }

    private long current;
    /**
     * 丢包率
     **/
    private int lostrate;
    private int rttmin;
    private int rttmax;
    private LinkedList<DelayPacket> p12 = new LinkedList();
    private LinkedList<DelayPacket> p21 = new LinkedList();
    private Random r12 = new Random();
    private Random r21 = new Random();


    // lostrate: 往返一周丢包率的百分比，默认 10%
    // rttmin：rtt最小值，默认 60
    // rttmax：rtt最大值，默认 125
    //func (p *LatencySimulator)Init(int lostrate = 10, int rttmin = 60, int rttmax = 125, int nmax = 1000):
    public void init(int lostrate, int rttmin, int rttmax) {
        this.current = System.currentTimeMillis();
        this.lostrate = lostrate / 2; // 上面数据是往返丢包率，单程除以2
        this.rttmin = rttmin / 2;
        this.rttmax = rttmax / 2;
    }


    // 发送数据
    // peer - 端点0/1，从0发送，从1接收；从1发送从0接收
    public int send(int peer, ByteBuf data) {
        int rnd;
        if (peer == 0) {
            rnd = r12.nextInt(100);
        } else {
            rnd = r21.nextInt(100);
        }
        //println("!!!!!!!!!!!!!!!!!!!!", rnd, p.lostrate, peer)
        if (rnd < lostrate) {
            return 0;
        }
        DelayPacket pkt = new DelayPacket();
        pkt.init(data);
        current = System.currentTimeMillis();
        int delay = rttmin;
        if (rttmax > rttmin) {
            delay += new Random().nextInt() % (rttmax - rttmin);
        }
        pkt.setTs(current + delay);
        if (peer == 0) {
            p12.addLast(pkt);
        } else {
            p21.addLast(pkt);
        }
        return 1;
    }

    // 接收数据
    public int recv(int peer, ByteBuf data) {
        DelayPacket pkt;
        if (peer == 0) {
            if (p21.size() == 0) {
                return -1;
            }
            pkt = p21.getFirst();
        } else {
            if (p12.size() == 0) {
                return -1;
            }
            pkt = p12.getFirst();
        }
        current = System.currentTimeMillis();

        if (current < pkt.getTs()) {
            return -2;
        }

        if (peer == 0) {
            p21.removeFirst();
        } else {
            p12.removeFirst();
        }
        int maxsize = pkt.getPtr().readableBytes();
        data.writeBytes(pkt.getPtr());
        pkt.release();
        return maxsize;
    }


    public static void main(String[] args) {
        LatencySimulator latencySimulator = new LatencySimulator();
        try {
            //latencySimulator.test(0);
            //latencySimulator.test(1);
            latencySimulator.test(2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        //latencySimulator.BenchmarkFlush();
    }


    //测试flush性能
    public void BenchmarkFlush(){
        Kcp kcp = new Kcp(1, (data, kcp1) -> {});
        for (int i = 0; i < 1000; i++) {
            Kcp.Segment segment =  Kcp.Segment.createSegment(null);
            kcp.sndBufItr.add(segment);
        }
        for (Iterator<Kcp.Segment> itr = kcp.sndBufItr.rewind(); itr.hasNext(); ) {
            Kcp.Segment seg = itr.next();
            seg.setXmit(1);
            seg.setResendts(System.currentTimeMillis()+10000);
        }
        //预热
        for (int i = 0; i < 1000000; i++) {
            kcp.flush(false,System.currentTimeMillis());
        }

        long start= System.nanoTime();
        for (int i = 0; i < 200000; i++) {
            kcp.flush(false,System.currentTimeMillis());
        }
        System.out.println((System.nanoTime()-start)/200000);
    }




    //func BenchmarkFlush(b *testing.B) {
    //    kcp := NewKCP(1, func(buf []byte, size int) {})
    //    kcp.snd_buf = make([]segment, 1024)
    //    for k := range kcp.snd_buf {
    //        kcp.snd_buf[k].xmit = 1
    //        kcp.snd_buf[k].resendts = currentMs() + 10000
    //    }
    //    b.ResetTimer()
    //    b.ReportAllocs()
    //    var mu sync.Mutex
    //    for i := 0; i < b.N; i++ {
    //        mu.Lock()
    //        kcp.flush(false)
    //        mu.Unlock()
    //    }
    //}


    public void test(int mode) throws InterruptedException {
        LatencySimulator vnet = new LatencySimulator();
        vnet.init(20, 600, 600);
        KcpOutput output1 = (buf, kcp) ->{
            vnet.send(0, buf);
            buf.release();
        };
        KcpOutput output2 = (buf, kcp) -> {
            vnet.send(1, buf);
            buf.release();
        };

        IKcp kcp1 = new Kcp(0x11223344, output1);
        IKcp kcp2 = new Kcp(0x11223344, output2);
        //kcp1.setAckMaskSize(8);
        //kcp2.setAckMaskSize(8);

        current = long2Uint(System.currentTimeMillis());
        long slap = current + 20;
        int index = 0;
        int next = 0;
        long sumrtt = 0;
        int count = 0;
        int maxrtt = 0;
        kcp1.setRcvWnd(512);
        kcp1.setSndWnd(512);
        kcp2.setRcvWnd(512);
        kcp2.setSndWnd(512);
        // 判断测试用例的模式
        if (mode == 0) {
            // 默认模式
            kcp1.nodelay(false, 10, 0, false);
            kcp2.nodelay(false, 10, 0, false);
        } else if (mode == 1) {
            // 普通模式，关闭流控等
            kcp1.nodelay(false, 10, 0, true);
            kcp2.nodelay(false, 10, 0, true);
        } else {
            // 启动快速模式
            // 第二个参数 nodelay-启用以后若干常规加速将启动
            // 第三个参数 interval为内部处理时钟，默认设置为 10ms
            // 第四个参数 resend为快速重传指标，设置为2
            // 第五个参数 为是否禁用常规流控，这里禁止
            kcp1.nodelay(true, 10, 2, true);
            kcp2.nodelay(true, 10, 2, true);
            kcp1.setRxMinrto(10);
            kcp1.setFastresend(1);
        }
        int hr;
        long ts1 =  System.currentTimeMillis() ;

        //写数据 定时更新
        for (;;) {
            current = long2Uint(System.currentTimeMillis());
            Thread.sleep(1);
            long now = System.currentTimeMillis();
            kcp1.update(now);
            kcp2.update(now);


            //每隔 20ms，kcp1发送数据
            for (; current >= slap; slap += 20) {
                ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
                buf.writeIntLE(index);
                index++;
                buf.writeIntLE((int) current);
                kcp1.send(buf);
                buf.release();
            }

            //处理虚拟网络：检测是否有udp包从p1->p2
            for (;;) {
                ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(2000);
                try {
                    hr = vnet.recv(1, buffer);
                    if (hr < 0) {
                        break;
                    }
                    kcp2.input(buffer,true,System.currentTimeMillis());
                }finally {
                    buffer.release();
                }
            }

            // 处理虚拟网络：检测是否有udp包从p2->p1
            for (;;){
                ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(2000);
                try {
                    hr = vnet.recv(0, buffer);
                    if (hr < 0) {
                        break;
                    }
                    // 如果 p1收到udp，则作为下层协议输入到kcp1
                    kcp1.input(buffer, true, System.currentTimeMillis());
                }finally {
                    buffer.release();
                }
            }

            // kcp2接收到任何包都返回回去
            CodecOutputList<ByteBuf> bufList  =  CodecOutputList.newInstance();
            kcp2.recv(bufList);
            for (ByteBuf byteBuf : bufList) {
                //ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(2000);
                //byteBuf.writerIndex(byteBuf.readerIndex());
                //byteBuf.readerIndex(0);
                kcp2.send(byteBuf);
                byteBuf.release();
            }
            bufList.recycle();

            // kcp1收到kcp2的回射数据
            bufList  =  CodecOutputList.newInstance();
            kcp1.recv(bufList);
            for (ByteBuf byteBuf : bufList) {
                long sn = byteBuf.readIntLE();
                long ts = byteBuf.readUnsignedIntLE();
                long rtt = 0;
                rtt = current - ts;
                System.out.println("rtt :" +rtt);


                if (sn != next) {
                    // 如果收到的包不连续
                    //for i:=0;i<8 ;i++ {
                    //println("---", i, buffer[i])
                    //}
                    System.err.println("ERROR sn "+ count+ "<->"+ next+ sn);
                    return;
                }
                next++;
                sumrtt += rtt;
                count++;
                if (rtt > maxrtt) {
                    maxrtt = (int) rtt;
                }
                byteBuf.release();

            }
            bufList.recycle();
            if (next > 1000) {
                break;
            }
        }
        ts1 = System.currentTimeMillis() - ts1;
        String[] names = new String[]{"default", "normal", "fast"};
        System.out.format("%s mode result (%dms): \n", names[mode], ts1);
        System.out.format("avgrtt=%d maxrtt=%d \n", (int)(sumrtt/count), maxrtt);
        System.out.println("lost percent: "+(Snmp.snmp.RetransSegs.doubleValue()));
        System.out.println("snmp: "+(Snmp.snmp.toString()));
    }

}


