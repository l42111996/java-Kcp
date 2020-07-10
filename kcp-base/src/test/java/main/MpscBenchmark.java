package main;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MpscChunkedArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscAtomicArrayQueue;
import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.atomic.MpscGrowableAtomicArrayQueue;
import org.jctools.queues.atomic.MpscLinkedAtomicQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 有界队列测试
 * Created by JinMiao
 * 2020/5/20.
 *
 Benchmark                                        (implementation)   Mode  Cnt         Score           Error  Units
 MpscCapacityBenchmark.MyGroup                      MpscArrayQueue  thrpt    3  28016642.829 ±   5835288.009  ops/s
 MpscCapacityBenchmark.MyGroup:read                 MpscArrayQueue  thrpt    3  14008350.850 ±   2917984.244  ops/s
 MpscCapacityBenchmark.MyGroup:write                MpscArrayQueue  thrpt    3  14008291.978 ±   2917303.800  ops/s
 MpscCapacityBenchmark.MyGroup                MpscAtomicArrayQueue  thrpt    3  21568431.588 ±  11960965.974  ops/s
 MpscCapacityBenchmark.MyGroup:read           MpscAtomicArrayQueue  thrpt    3  10784231.563 ±   5980209.995  ops/s
 MpscCapacityBenchmark.MyGroup:write          MpscAtomicArrayQueue  thrpt    3  10784200.025 ±   5980755.986  ops/s
 MpscCapacityBenchmark.MyGroup               MpscChunkedArrayQueue  thrpt    3  20967041.159 ±  10622955.468  ops/s
 MpscCapacityBenchmark.MyGroup:read          MpscChunkedArrayQueue  thrpt    3  10483536.484 ±   5311168.053  ops/s
 MpscCapacityBenchmark.MyGroup:write         MpscChunkedArrayQueue  thrpt    3  10483504.675 ±   5311787.421  ops/s
 MpscCapacityBenchmark.MyGroup        MpscGrowableAtomicArrayQueue  thrpt    3  21796632.795 ±  14176589.672  ops/s
 MpscCapacityBenchmark.MyGroup:read   MpscGrowableAtomicArrayQueue  thrpt    3  10898343.925 ±   7088428.489  ops/s
 MpscCapacityBenchmark.MyGroup:write  MpscGrowableAtomicArrayQueue  thrpt    3  10898288.870 ±   7088161.187  ops/s
 MpscCapacityBenchmark.MyGroup                  ArrayBlockingQueue  thrpt    3  15652600.106 ± 103896139.784  ops/s
 MpscCapacityBenchmark.MyGroup:read             ArrayBlockingQueue  thrpt    3   7826284.347 ±  51947891.290  ops/s
 MpscCapacityBenchmark.MyGroup:write            ArrayBlockingQueue  thrpt    3   7826315.759 ±  51948248.496  ops/s
 MpscCapacityBenchmark.MyGroup                 LinkedBlockingQueue  thrpt    3  10068103.002 ±  18234471.577  ops/s
 MpscCapacityBenchmark.MyGroup:read            LinkedBlockingQueue  thrpt    3   5034052.569 ±   9117174.419  ops/s
 MpscCapacityBenchmark.MyGroup:write           LinkedBlockingQueue  thrpt    3   5034050.433 ±   9117297.158  ops/s
 MpscNoCapacityBenchmark.MyGroup                        MpscLinked  thrpt    3  53389377.536 ± 22345602.442  ops/s
 MpscNoCapacityBenchmark.MyGroup:read                   MpscLinked  thrpt    3  26694715.226 ± 11172841.874  ops/s
 MpscNoCapacityBenchmark.MyGroup:write                  MpscLinked  thrpt    3  26694662.311 ± 11172761.077  ops/s
 MpscNoCapacityBenchmark.MyGroup                  MpscLinkedAtomic  thrpt    3  37142210.446 ± 49326137.693  ops/s
 MpscNoCapacityBenchmark.MyGroup:read             MpscLinkedAtomic  thrpt    3  18571104.104 ± 24661172.819  ops/s
 MpscNoCapacityBenchmark.MyGroup:write            MpscLinkedAtomic  thrpt    3  18571106.342 ± 24664964.875  ops/s
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@State(Scope.Group)
public class MpscBenchmark {

    public static final String PARAM_UNSAFE = "MpscArrayQueue";
    public static final String PARAM_AFU = "MpscAtomicArrayQueue";
    public static final String PARAM_LINKED = "MpscChunkedArrayQueue";
    public static final String PARAM_LINKED_GROUP = "MpscGrowableAtomicArrayQueue";
    public static final String PARAM_JDK = "ArrayBlockingQueue";
    public static final String PARAM_JDK_LINK = "LinkedBlockingQueue";
    public static final String PARAM_MPSC_LINKED = "MpscLinked";
    public static final String PARAM_MPMC_LINKED_ATOMICQUEUE = "MpscLinkedAtomic";



    public static final int PRODUCER_THREADS_NUMBER = 4;

    public static final String GROUP_NAME = "MyGroup";

    public static final int CAPACITY = 1024;

    @Param({PARAM_UNSAFE, PARAM_AFU, PARAM_LINKED,PARAM_LINKED_GROUP, PARAM_JDK, PARAM_JDK_LINK})
    public volatile String implementation;

    public volatile Queue<Long> queue;

    @Setup(Level.Trial)
    public void setUp() {
        switch (implementation) {
            case PARAM_UNSAFE:
                queue = new MpscArrayQueue<>(CAPACITY);
                break;
            case PARAM_AFU:
                queue = new MpscAtomicArrayQueue<>(CAPACITY);
                break;
            case PARAM_JDK:
                queue = new ArrayBlockingQueue<>(CAPACITY);
                break;
            case PARAM_LINKED:
                queue = new MpscChunkedArrayQueue<>(CAPACITY);
                break;
            case PARAM_JDK_LINK:
                queue = new LinkedBlockingQueue<>(CAPACITY);
                break;
            case PARAM_LINKED_GROUP:
                queue = new MpscGrowableAtomicArrayQueue<>(CAPACITY);
                break;
            case PARAM_MPSC_LINKED:
                queue = new MpscLinkedQueue<>();
                break;
            case PARAM_MPMC_LINKED_ATOMICQUEUE:
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