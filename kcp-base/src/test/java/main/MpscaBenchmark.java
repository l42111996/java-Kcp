package main;

import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 *
 * Benchmark                                      (implementation)   Mode  Cnt         Score          Error  Units
 * MpscNoCapacityBenchmark.MyGroup                    PARAM_Linked  thrpt    3  53389377.536 ± 22345602.442  ops/s
 * MpscNoCapacityBenchmark.MyGroup:read               PARAM_Linked  thrpt    3  26694715.226 ± 11172841.874  ops/s
 * MpscNoCapacityBenchmark.MyGroup:write              PARAM_Linked  thrpt    3  26694662.311 ± 11172761.077  ops/s
 * MpscNoCapacityBenchmark.MyGroup        PARAM_LINKED_ATOMICQUEUE  thrpt    3  37142210.446 ± 49326137.693  ops/s
 * MpscNoCapacityBenchmark.MyGroup:read   PARAM_LINKED_ATOMICQUEUE  thrpt    3  18571104.104 ± 24661172.819  ops/s
 * MpscNoCapacityBenchmark.MyGroup:write  PARAM_LINKED_ATOMICQUEUE  thrpt    3  18571106.342 ± 24664964.875  ops/s
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

    public static final String PARAM_Linked = "PARAM_Linked";

    public static final String PARAM_LINKED_ATOMICQUEUE = "PARAM_LINKED_ATOMICQUEUE";

    public static final int PRODUCER_THREADS_NUMBER = 4;

    public static final String GROUP_NAME = "MyGroup";

    @Param({PARAM_Linked,PARAM_LINKED_ATOMICQUEUE})
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
