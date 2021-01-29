package com.backblaze.erasure.fecNative;

/**
 * Created by JinMiao
 * 2018/8/27.
 */
public class ReedSolomonC {

    static {
        //System.load("/Users/king/Desktop/libjni.dylib");
        String path = System.getProperty("user.dir");
        System.load(path+"/kcp-fec/src/main/java/com/backblaze/erasure/fecNative/native/libjni.dylib");
        init();
    }

    protected static native void init();

    protected native long rsNew(int data_shards, int parity_shards);

    protected native void rsRelease(long reedSolomonPtr);

    protected native void rsEncode(long reedSolomonPtr,long[] shards,int byteCount);

    protected native void rsReconstruct(long reedSolomonPtr,long[] shards,boolean[] shardPresent,int byteCount);
}
