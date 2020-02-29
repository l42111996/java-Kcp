# java-Kcp

[![Powered][2]][1]

[README in english](https://github.com/l42111996/java-Kcp/blob/master/README.en.md)

基于netty版本实现的kcp(包含fec功能的实现)

KCP是一个基于udp的快速可靠协议(rudp)，能以比 TCP浪费10%-20%的带宽的代价，换取平均延迟降低 30%-40%，且最大延迟降低三倍的传输效果。


[1]: https://github.com/skywind3000/kcp
[2]: http://skywind3000.github.io/word/images/kcp.svg


# 兼容性:
1. 兼容c版本kcp
2. fec基于 https://github.com/Backblaze/JavaReedSolomon 实现


# 优化:
1. 基于disruptor事件驱动,充分利用多核
2. 优化fastack逻辑，降低10%流量
3. 优化check函数。
4. 优化集合迭代器。
5. 包含fec,降低延迟
6. 附带crc32校验
7. 使用时间轮,优化大量连接cpu占用
8. 使用directbuf和对象池，无gc压力
9. 增加使用conv或者ip+port确定channel唯一性，游戏建议使用conv与tcp配置使用,[相关资料](https://github.com/skywind3000/kcp/wiki/Cooperate-With-Tcp-Server)
10. 增加同通道不可靠udp发送接收，游戏应用更广泛


# 使用方法以及参数
1. [server端示例](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/KcpRttExampleServer.java)
2. [client端实例](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/KcpRttExampleClient.java)
3. [最佳实践](https://github.com/skywind3000/kcp/wiki/KCP-Best-Practice)
4. [大量资料](https://github.com/skywind3000/kcp)
5. 兼容c#端，[java服务端](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/Kcp4sharpExampleServer.java) , [c#客户端](https://github.com/l42111996/csharp-kcp/blob/master/example-Kcp/KcpRttExampleClient.cs)
6. [遇到过的问题](https://github.com/l42111996/java-Kcp/blob/master/QA.md)
7. [性能测试结果](https://github.com/l42111996/java-Kcp/blob/master/Benchmark.md)
8. [兼容kcp-go,包含fec兼容](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/Kcp4GoExampleClient.java)

# 已完成测试
1. 单连接死循环收发包内存检测，性能测试  
配置:mbp 2.3 GHz Intel Core i9 16GRam
单连接 带fec 5W/s qps
单连接 不带fec  9W/s qps
2. java服务端与c#客户端兼容测试
3. 3000连接保持使用单核30%cpu
4. fec结合directbuffer检测对应cpu最佳性能方案



# TODO 
1,高并发吞吐量下，rtt稳定性测试
   
   
# 相关资料

1. https://github.com/skywind3000/kcp 原版c版本的kcp
2. https://github.com/xtaci/kcp-go go版本kcp,有大量优化
3. https://github.com/Backblaze/JavaReedSolomon java版本fec
4. https://github.com/LMAX-Exchange/disruptor 高性能的线程间消息传递库
5. https://github.com/JCTools/JCTools 高性能并发库
6. https://github.com/szhnet/kcp-netty java版本的一个kcp
7. https://github.com/l42111996/csharp-kcp 基于dotNetty的c#版本kcp,完美兼容
   
   
#交流

QQ:526167774
   
    