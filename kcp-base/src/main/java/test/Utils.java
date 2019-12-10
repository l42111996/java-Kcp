package test;

import io.netty.buffer.ByteBuf;

/**
 * Created by JinMiao
 * 2019/12/10.
 */
public class Utils {

    public static final synchronized void printByteBuffer(String log,ByteBuf byteBuf){
        byte[] bytes = new byte[byteBuf.writerIndex()];
        byteBuf.getBytes(0,bytes);
        System.err.println("-------"+log+" start -------------");
        for (byte aByte : bytes) {
            System.err.print(aByte+",");
        }
        System.out.println();
        System.err.println("-------"+log+" end -------------");


    }
}
