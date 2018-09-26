package kcp;

import io.netty.buffer.ByteBuf;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public interface KcpListener {


    /**
     * 连接之后
     * @param ukcp
     */
    void onConnected(Ukcp ukcp);

    /**
     * kcp message
     *
     * @param bb the data
     * @param kcp
     */
    void handleReceive(ByteBuf bb, Ukcp kcp);

    /**
     *
     * kcp异常，之后此kcp就会被关闭
     *
     * @param ex 异常
     * @param kcp 发生异常的kcp，null表示非kcp错误
     */
    void handleException(Throwable ex, Ukcp kcp);

    /**
     * 关闭
     *
     * @param kcp
     */
    void handleClose(Ukcp kcp);
}
