package com.backblaze.erasure.fec;

/**
 * Created by JinMiao
 * 2018/6/6.
 */
public class Fec {
    public static int mtuLimit = 1500;
    public static int
            fecHeaderSize      = 6,
            fecHeaderSizePlus2 = fecHeaderSize + 2, // plus 2B data size
            typeData           = 0xf1,
            typeFEC            = 0xf2;

}
