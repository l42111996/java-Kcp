package main;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Benchmark                                     Mode  Cnt         Score          Error  Units
 SignalBenchmark.signal                       thrpt    3  37230465.734 ± 50417551.328  ops/s
 SignalBenchmark.signal:await                 thrpt    3    158900.873 ±   685914.242  ops/s
 SignalBenchmark.signal:signal                thrpt    3  37071564.860 ± 51093152.567  ops/s
 SignalBenchmark.signalAll                    thrpt    3  37483352.305 ± 32047146.318  ops/s
 SignalBenchmark.signalAll:awaitForSignalAll  thrpt    3    152516.702 ±   616198.089  ops/s
 SignalBenchmark.signalAll:signalAll          thrpt    3  37330835.603 ± 32604177.865  ops/s
 * Created by JinMiao
 * 2020/6/22.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@State(Scope.Group)
public class SignalBenchmark {

    private Condition signalCondition;
    private Lock signalLock;

    private Condition signalAllCondition;
    private Lock signaAlllLock;

    @Setup(Level.Trial)
    public void setUp() {
        signalLock = new ReentrantLock();
        signalCondition = signalLock.newCondition();
        signaAlllLock = new ReentrantLock();
        signalAllCondition = signaAlllLock.newCondition();
    }


    @Benchmark
    @GroupThreads(1)
    @Group("signal")
    public void signal(Control control) {
        try {
            signalLock.lock();
            signalCondition.signal();
        }finally {
            signalLock.unlock();
        }
    }

    @Benchmark
    @GroupThreads(1)
    @Group("signal")
    public void await(Control control) {
        try {
            signalLock.lock();
            signalCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            signalLock.unlock();
        }
    }


    @Benchmark
    @GroupThreads(1)
    @Group("signalAll")
    public void signalAll(Control control) {
        try {
            signaAlllLock.lock();
            signalAllCondition.signal();
        }finally {
            signaAlllLock.unlock();
        }
    }


    @Benchmark
    @GroupThreads(1)
    @Group("signalAll")
    public void awaitForSignalAll(Control control) {
        try {
            signaAlllLock.lock();
            signalAllCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            signaAlllLock.unlock();
        }
    }


}
