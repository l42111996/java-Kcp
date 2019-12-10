# java-Kcp

[![Powered][2]][1]

Kcp based on netty version (including implementation of fec function)

KCP is a udp-based fast and reliable protocol (rudp), which can reduce the average delay by 30% -40% at the cost of wasting 10% -20% of bandwidth over TCP, and reduce the maximum delay by three times the transmission effect.


[1]: https://github.com/skywind3000/kcp
[2]: http://skywind3000.github.io/word/images/kcp.svg


#compatibility:
1. Compatible with c version of kcp
2. fec implementation based on https://github.com/Backblaze/JavaReedSolomon


#optimization:
1. Based on disruptor event-driven, make full use of multi-core
2. Optimize fastack logic and reduce traffic by 10%
3. Optimize the check function.
4. Optimize collection iterators.
5. Include fec to reduce latency
6. With crc32 check
7. Use the time wheel to optimize the CPU usage of a large number of connections
8. Use directbuf and object pool, no gc pressure
9. Increase the use of conv or ip + port to determine the uniqueness of the channel. The game is recommended to use conv and tcp configuration. [Related information](https://github.com/skywind3000/kcp/wiki/Cooperate-With-Tcp-Server)
10. Increase unreliable udp sending and receiving in the same channel, and the game is more widely used


#Using method and parameters
1. [Server-side example](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/KcpRttExampleServer.java)
2. [Client Example](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/KcpRttExampleClient.java)
3. [Best Practices](https://github.com/skywind3000/kcp/wiki/KCP-Best-Practice)
4. [A lot of information](https://github.com/skywind3000/kcp)
5. Compatible with C #, [java server](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/Kcp4sharpExampleServer.java), [c #Client](https://github.com/l42111996/kcp4sharp/blob/master/kcp4sharp/TestKcp.cs)
6. [Encountered problems](https://github.com/l42111996/java-Kcp/blob/master/QA.md)
7. [Performance test results](https://github.com/l42111996/java-Kcp/blob/master/Benchmark.md)
8. [Compatible with kcp-go, including fec compatible](https://github.com/l42111996/java-Kcp/blob/master/kcp-netty/src/main/java/test/Kcp4GoExampleClient.java)


#Test completed
1. Single connection endless loop sending and receiving packet memory detection, performance test
Configuration: mbp 2.3 GHz Intel Core i9 16GRam
Single connection with fec 5W / s qps
Single connection without fec 9W / s qps
2. Java server and c # client compatibility test
3. 3000 connections remain using single core 30% cpu
4. fec combined with directbuffer detection for the best CPU performance plan



#TODO
1. RTT stability test under high concurrent carbonization


#Relevant information

1. https://github.com/skywind3000/kcp The original ccp version of kcp
2. https://github.com/xtaci/kcp-go go version kcp, with a lot of optimization
3. https://github.com/Backblaze/JavaReedSolomon java version fec
4. https://github.com/LMAX-Exchange/disruptor High-performance inter-thread messaging library
5. https://github.com/JCTools/JCTools efficient concurrent library
6. https://github.com/szhnet/kcp-netty A kcp for java version


#communicate with

QQ: 526167774