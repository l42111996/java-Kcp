package com.backblaze.erasure;

import com.backblaze.erasure.fec.MyArrayList;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2020/7/2.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
//@State(Scope.Group)
public class FecDecodeBenchmark {


    @Benchmark
    public void remove(){
        buildFree();
    }


    @Benchmark
    public void removeRange(){
        buildRemoveRange();
    }



    public void buildRemoveRange(){
        int first = 10;
        int n=20;
        MyArrayList<Integer> q = new MyArrayList<>();
        for (int i = 0; i < 100; i++) {
            q.add(i);
        }
        q.removeRange(first,first+n);
    }

    public void buildFree(){
        int first = 0;
        int n=20;
        MyArrayList<Integer> q = new MyArrayList<>();
        for (int i = 0; i < 100; i++) {
            q.add(i);
        }
        //q.removeRange(first,first+n);
        for (int i = first; i < q.size(); i++) {
            int index = i+n;
            if(index==q.size()) {
                break;
            }
            q.set(i,q.get(index));
        }
        for (int i = 0; i < n; i++) {
            q.remove(q.size()-1);
        }
    }

}
