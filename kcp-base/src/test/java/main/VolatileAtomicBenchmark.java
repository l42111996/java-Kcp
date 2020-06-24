package main;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark                                          Mode  Cnt            Score             Error  Units
 * VolatileAtomicBenchmark.GROUP_NAME                thrpt    3  11520559872.738 ±  2548748730.823  ops/s
 * VolatileAtomicBenchmark.GROUP_NAME:volatileRead   thrpt    3  11368008033.661 ±  2518167944.554  ops/s
 * VolatileAtomicBenchmark.GROUP_NAME:volatileWrite  thrpt    3    152551839.078 ±    30595466.236  ops/s
 * VolatileAtomicBenchmark.GROUP_NAME1               thrpt    3   5491817786.486 ± 25550691348.844  ops/s
 * VolatileAtomicBenchmark.GROUP_NAME1:atmoicRead    thrpt    3   5362082320.914 ± 25385552586.081  ops/s
 * VolatileAtomicBenchmark.GROUP_NAME1:atmoicWrite   thrpt    3    129735465.572 ±   169450075.999  ops/s
 * Created by JinMiao
 * 2020/6/22.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@State(Scope.Group)
public class VolatileAtomicBenchmark {

    private volatile int vol;

    private AtomicInteger atomicInteger = new AtomicInteger();

    @Setup(Level.Trial)
    public void setUp() {
        vol = 0;
    }
    @Benchmark
    @GroupThreads(1)
    @Group("GROUP_NAME")
    public void volatileWrite(Control control) {
        vol++;
    }

    @Benchmark
    @GroupThreads(3)
    @Group("GROUP_NAME")
    public void volatileRead(Control control) {
        int b = vol;
        b++;
    }


    @Benchmark
    @GroupThreads(1)
    @Group("GROUP_NAME1")
    public void atmoicWrite(Control control) {
        atomicInteger.incrementAndGet();
    }

    @Benchmark
    @GroupThreads(3)
    @Group("GROUP_NAME1")
    public void atmoicRead(Control control) {
        int c = atomicInteger.get();
        c++;
    }

}
