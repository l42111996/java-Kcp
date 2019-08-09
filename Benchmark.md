#一些测试结果

# rtt测试
1. [测试代码](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/KcpRttExampleClient.java)
rtt:1450 参数 lag:300 drop:10% snd:512 rnd:512 mtu:512 nodelay:true interval:10 nocwnd:true ackNoDelay:true fastresend:2


2. [fec测试代码](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/KcpRttExampleClient.java)
rtt:890 参数 lag:300 drop:10% snd:512 rnd:512 mtu:512 nodelay:true interval:10 nocwnd:true ackNoDelay:true fastresend:2 fecDataShardCount:3 fecParityShardCount:1

 
# cpu测试
