package threadPool.order.forkjoin;

import io.netty.util.Recycler;
import kcp.ReadTask;
import kcp.Ukcp;

/**
 * Created by JinMiao
 * 2020/8/3.
 */
public class ForkJoinTask extends java.util.concurrent.ForkJoinTask<Void> {

    private final Recycler.Handle<ForkJoinTask> recyclerHandle;
    Runnable runnable;

    private static final Recycler<ForkJoinTask> RECYCLER = new Recycler<ForkJoinTask>(Integer.MAX_VALUE) {
        @Override
        protected ForkJoinTask newObject(Handle<ForkJoinTask> handle) {
            return new ForkJoinTask(handle);
        }
    };

    private ForkJoinTask(Recycler.Handle<ForkJoinTask> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }


    protected static ForkJoinTask New(Runnable runnable) {
        ForkJoinTask readTask = RECYCLER.get();
        readTask.runnable = runnable;
        return readTask;
    }


    public final Void getRawResult() {
        return null;
    }

    public final void setRawResult(Void v) {
    }

    public final boolean exec() {
        runnable.run();
        release();
        return true;
    }

    private static final long serialVersionUID = 5232453952276885070L;


    public void release() {
        recyclerHandle.recycle(this);
    }
}
