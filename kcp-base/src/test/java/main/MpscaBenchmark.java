package main;

import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jctools.queues.*;
import org.jctools.queues.atomic.MpscChunkedAtomicArrayQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 *
 Benchmark                                        (implementation)   Mode  Cnt         Score          Error  Units
 MpscaBenchmark.MyGroup                               PARAM_Linked  thrpt    3  51081130.039 ± 22670119.965  ops/s
 MpscaBenchmark.MyGroup:read                          PARAM_Linked  thrpt    3  25540597.167 ± 11334748.828  ops/s
 MpscaBenchmark.MyGroup:write                         PARAM_Linked  thrpt    3  25540532.871 ± 11335371.539  ops/s
 MpscaBenchmark.MyGroup                   PARAM_LINKED_ATOMICQUEUE  thrpt    3  50800677.821 ± 22446778.474  ops/s
 MpscaBenchmark.MyGroup:read              PARAM_LINKED_ATOMICQUEUE  thrpt    3  25400339.501 ± 11223775.305  ops/s
 MpscaBenchmark.MyGroup:write             PARAM_LINKED_ATOMICQUEUE  thrpt    3  25400338.320 ± 11223003.327  ops/s
 MpscaBenchmark.MyGroup              PARAM_MpscUnboundedArrayQueue  thrpt    3   8523884.340 ±  2478333.168  ops/s
 MpscaBenchmark.MyGroup:read         PARAM_MpscUnboundedArrayQueue  thrpt    3   4261945.984 ±  1239077.075  ops/s
 MpscaBenchmark.MyGroup:write        PARAM_MpscUnboundedArrayQueue  thrpt    3   4261938.356 ±  1239256.093  ops/s
 MpscaBenchmark.MyGroup        PARAM_MpscUnboundedAtomicArrayQueue  thrpt    3   7750201.020 ±   734250.824  ops/s
 MpscaBenchmark.MyGroup:read   PARAM_MpscUnboundedAtomicArrayQueue  thrpt    3   3875105.906 ±   367144.551  ops/s
 MpscaBenchmark.MyGroup:write  PARAM_MpscUnboundedAtomicArrayQueue  thrpt    3   3875095.114 ±   367106.310  ops/s
 MpscaBenchmark.MyGroup                       PARAM_MpscArrayQueue  thrpt    3  13748739.474 ±  2507214.536  ops/s
 MpscaBenchmark.MyGroup:read                  PARAM_MpscArrayQueue  thrpt    3   6874362.473 ±  1253480.134  ops/s
 MpscaBenchmark.MyGroup:write                 PARAM_MpscArrayQueue  thrpt    3   6874377.001 ±  1253734.422  ops/s
 MpscaBenchmark.MyGroup                 PARAM_MpscAtomicArrayQueue  thrpt    3  13016730.228 ±   259225.449  ops/s
 MpscaBenchmark.MyGroup:read            PARAM_MpscAtomicArrayQueue  thrpt    3   6508362.564 ±   129594.326  ops/s
 MpscaBenchmark.MyGroup:write           PARAM_MpscAtomicArrayQueue  thrpt    3   6508367.664 ±   129631.132  ops/s
 * Created by JinMiao
 * 2020/6/24.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@State(Scope.Group)
public class MpscaBenchmark {

    public static final String PARAM_Linked = "PARAM_Linked",PARAM_LINKED_ATOMICQUEUE = "PARAM_LINKED_ATOMICQUEUE",PARAM_MpscUnboundedArrayQueue="PARAM_MpscUnboundedArrayQueue",PARAM_MpscUnboundedAtomicArrayQueue="PARAM_MpscUnboundedAtomicArrayQueue",PARAM_MpscArrayQueue="PARAM_MpscArrayQueue",PARAM_MpscAtomicArrayQueue="PARAM_MpscAtomicArrayQueue";


    public static final int PRODUCER_THREADS_NUMBER = 4;

    public static final String GROUP_NAME = "MyGroup";

    @Param({PARAM_Linked,PARAM_LINKED_ATOMICQUEUE,PARAM_MpscUnboundedArrayQueue,PARAM_MpscUnboundedAtomicArrayQueue,PARAM_MpscArrayQueue,PARAM_MpscAtomicArrayQueue})
    public volatile String implementation;

    public volatile Queue<Long> queue;

    @Setup(Level.Trial)
    public void setUp() {
        switch (implementation) {
            case PARAM_Linked:
                queue = new MpscLinkedQueue<>();
                break;
            case PARAM_LINKED_ATOMICQUEUE:
                queue = new MpscLinkedAtomicQueue<>();
                break;
            case PARAM_MpscUnboundedArrayQueue:
                queue = new MpscUnboundedArrayQueue<>(1024);
                break;
            case PARAM_MpscUnboundedAtomicArrayQueue:
                queue = new MpscUnboundedAtomicArrayQueue<>(1024);
                break;
            case PARAM_MpscAtomicArrayQueue:
                queue = new MpscAtomicArrayQueue<>(1024);
                break;
            case PARAM_MpscArrayQueue:
                queue = new MpscArrayQueue<>(1024);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported implementation " + implementation);
        }
    }

    @Benchmark
    @Group(GROUP_NAME)
    @GroupThreads(PRODUCER_THREADS_NUMBER)
    public void write(Control control) {
        while (!control.stopMeasurement && !queue.offer(1L)) {
        }
    }

    @Benchmark
    @Group(GROUP_NAME)
    public void read(Control control) {
        while (!control.stopMeasurement && queue.poll() == null) {
        }
    }
}
