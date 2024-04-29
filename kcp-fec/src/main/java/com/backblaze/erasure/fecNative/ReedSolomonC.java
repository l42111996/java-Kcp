package com.backblaze.erasure.fecNative;

import java.io.*;

/**
 * Created by JinMiao
 * 2018/8/27.
 */
public class ReedSolomonC {

    private static boolean nativeSupport = true;
    static {
        try {
            String path = System.getProperty("user.dir");
            String libPath = new File(path, "kcp-fec/src/main/java/com/backblaze/erasure/fecNative/native/libjni").toString();
            String extension = "";
            if (System.getProperty("os.name").startsWith("Windows")) {
                extension = "dll";
            } else if (System.getProperty("os.name").startsWith("Linux")) {
                extension = "so";
            } else if (System.getProperty("os.name").startsWith("Mac")) {
                extension = "dylib";
            }
            libPath += "." + extension;
            System.load(libPath);
            init();
        }catch (Throwable e){
            nativeSupport = false;
        }
    }

    public static boolean isNativeSupport() {
        return nativeSupport;
    }

    protected static native void init();

    protected native long rsNew(int data_shards, int parity_shards);

    protected native void rsRelease(long reedSolomonPtr);

    protected native void rsEncode(long reedSolomonPtr,long[] shards,int byteCount);

    protected native void rsReconstruct(long reedSolomonPtr,long[] shards,boolean[] shardPresent,int byteCount);
}
