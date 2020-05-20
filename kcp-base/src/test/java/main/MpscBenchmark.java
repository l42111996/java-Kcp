package main;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MpscChunkedArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscAtomicArrayQueue;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscGrowableAtomicArrayQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2020/5/20.
 *
 MpscBenchmark.MyGroup                      MpscArrayQueue  thrpt    3  23236.229 ±  9402.469  ops/ms
 MpscBenchmark.MyGroup:read                 MpscArrayQueue  thrpt    3  11618.120 ±  4701.283  ops/ms
 MpscBenchmark.MyGroup:write                MpscArrayQueue  thrpt    3  11618.109 ±  4701.186  ops/ms
 MpscBenchmark.MyGroup                MpscAtomicArrayQueue  thrpt    3  11904.605 ± 23155.200  ops/ms
 MpscBenchmark.MyGroup:read           MpscAtomicArrayQueue  thrpt    3   5952.330 ± 11577.666  ops/ms
 MpscBenchmark.MyGroup:write          MpscAtomicArrayQueue  thrpt    3   5952.275 ± 11577.535  ops/ms
 MpscBenchmark.MyGroup               MpscChunkedArrayQueue  thrpt    3  22858.648 ±  7910.293  ops/ms
 MpscBenchmark.MyGroup:read          MpscChunkedArrayQueue  thrpt    3  11429.353 ±  3955.361  ops/ms
 MpscBenchmark.MyGroup:write         MpscChunkedArrayQueue  thrpt    3  11429.295 ±  3954.932  ops/ms
 MpscBenchmark.MyGroup        MpscGrowableAtomicArrayQueue  thrpt    3  21024.496 ± 20103.975  ops/ms
 MpscBenchmark.MyGroup:read   MpscGrowableAtomicArrayQueue  thrpt    3  10512.282 ± 10051.564  ops/ms
 MpscBenchmark.MyGroup:write  MpscGrowableAtomicArrayQueue  thrpt    3  10512.214 ± 10052.411  ops/ms
 MpscBenchmark.MyGroup                  ArrayBlockingQueue  thrpt    3   9502.221 ± 44989.505  ops/ms
 MpscBenchmark.MyGroup:read             ArrayBlockingQueue  thrpt    3   4751.105 ± 22494.781  ops/ms
 MpscBenchmark.MyGroup:write            ArrayBlockingQueue  thrpt    3   4751.116 ± 22494.724  ops/ms
 MpscBenchmark.MyGroup                 LinkedBlockingQueue  thrpt    3   9430.241 ±  8998.417  ops/ms
 MpscBenchmark.MyGroup:read            LinkedBlockingQueue  thrpt    3   4715.115 ±  4499.178  ops/ms
 MpscBenchmark.MyGroup:write           LinkedBlockingQueue  thrpt    3   4715.126 ±  4499.239  ops/ms
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
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