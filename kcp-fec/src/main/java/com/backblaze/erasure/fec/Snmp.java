package com.backblaze.erasure.fec;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by JinMiao
 * 2018/8/29.
 */
public class Snmp {
    // bytes sent from upper level
    public AtomicLong BytesSent = new AtomicLong();
    // bytes received to upper level
    public AtomicLong BytesReceived = new AtomicLong();
    // max number of connections ever reached
    public AtomicLong MaxConn = new AtomicLong();
    // accumulated active open connections
    public AtomicLong ActiveOpens = new AtomicLong();
    // accumulated passive open connections
    public AtomicLong PassiveOpens = new AtomicLong();
    // current number of established connections
    public AtomicLong CurrEstab = new AtomicLong();
    // UDP read errors reported from net.PacketConn
    public AtomicLong InErrs = new AtomicLong();
    // checksum errors from CRC32
    public AtomicLong InCsumErrors = new AtomicLong();
    // packet iput errors reported from KCP
    public AtomicLong KCPInErrors = new AtomicLong();
    // incoming packets count
    public AtomicLong InPkts = new AtomicLong();
    // outgoing packets count
    public AtomicLong OutPkts = new AtomicLong();
    // incoming KCP segments
    public AtomicLong InSegs = new AtomicLong();
    // outgoing KCP segments
    public AtomicLong OutSegs = new AtomicLong();
    // UDP bytes received
    public AtomicLong InBytes = new AtomicLong();
    // UDP bytes sent
    public AtomicLong OutBytes = new AtomicLong();
    // accmulated retransmited segments
    public AtomicLong RetransSegs = new AtomicLong();
    // accmulated fast retransmitted segments
    public AtomicLong FastRetransSegs = new AtomicLong();
    // accmulated early retransmitted segments
    public AtomicLong EarlyRetransSegs = new AtomicLong();
    // number of segs infered as lost
    public AtomicLong LostSegs = new AtomicLong();
    // number of segs duplicated
    public AtomicLong RepeatSegs = new AtomicLong();
    // correct packets recovered from FEC
    public AtomicLong FECRecovered = new AtomicLong();
    // incorrect packets recovered from FEC
    public AtomicLong FECErrs = new AtomicLong();
    // 收到的 Parity数量
    public AtomicLong FECParityShards = new AtomicLong();
    // number of data shards that's not enough for recovery
    public AtomicLong FECShortShards = new AtomicLong();
    // number of data shards that's not enough for recovery
    public AtomicLong FECRepeatDataShards = new AtomicLong();



    public static volatile Snmp snmp = new  Snmp();


    @Override
    public String toString() {
        return "Snmp{" +
                "BytesSent=" + BytesSent +
                ", BytesReceived=" + BytesReceived +
                ", MaxConn=" + MaxConn +
                ", ActiveOpens=" + ActiveOpens +
                ", PassiveOpens=" + PassiveOpens +
                ", CurrEstab=" + CurrEstab +
                ", InErrs=" + InErrs +
                ", InCsumErrors=" + InCsumErrors +
                ", KCPInErrors=" + KCPInErrors +
                ", 收到包=" + InPkts +
                ", 发送包=" + OutPkts +
                ", InSegs=" + InSegs +
                ", OutSegs=" + OutSegs +
                ", 收到字节=" + InBytes +
                ", 发送字节=" + OutBytes +
                ", 总共重发数=" + RetransSegs +
                ", 快速重发数=" + FastRetransSegs +
                ", 空闲快速重发数=" + EarlyRetransSegs +
                ", 超时重发数=" + LostSegs +
                ", 收到重复包数量=" + RepeatSegs +
                ", fec恢复数=" + FECRecovered +
                ", fec恢复错误数=" + FECErrs +
                ", 收到fecParity数=" + FECParityShards +
                ", fec缓存冗余淘汰data包数=" + FECShortShards +
                ", fec收到重复的数据包=" + FECRepeatDataShards +
                '}';
    }

}
