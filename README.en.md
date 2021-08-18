# java-Kcp

[![Powered][2]][1]

[1]: https://github.com/skywind3000/kcp
[2]: http://skywind3000.github.io/word/images/kcp.svg

Kcp based on netty version (including implementation of fec function)

KCP is a udp-based fast and reliable protocol (rudp), which can reduce the average delay by 30% -40% at the cost of wasting 10% -20% of bandwidth over TCP, and reduce the maximum delay by three times the transmission effect.

# maven repository:

```xml
<dependency>
  <groupId>com.github.l42111996</groupId>
  <artifactId>kcp-base</artifactId>
  <version>1.6</version>
</dependency>
```
# Using method and parameters
1. [Server-side example](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/KcpRttExampleServer.java)
2. [Client Example](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/KcpRttExampleClient.java)
3. [Best Practices](https://github.com/skywind3000/kcp/wiki/KCP-Best-Practice)
4. [A lot of information](https://github.com/skywind3000/kcp)
5. Compatible with C #, [java server](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/Kcp4sharpExampleServer.java), [c #Client](https://github.com/l42111996/csharp-kcp/blob/master/example-Kcp/KcpRttExampleClient.cs)
6. [Encountered problems](https://github.com/l42111996/java-Kcp/blob/master/QA.md)
7. [Performance test results](https://github.com/l42111996/java-Kcp/blob/master/Benchmark.md)
8. [Compatible with kcp-go, including fec compatible](https://github.com/l42111996/java-Kcp/blob/master/kcp-example/src/main/java/test/Kcp4GoExampleClient.java)

# compatibility:
1. Compatible with c version of kcp
2. fec implementation based on https://github.com/Backblaze/JavaReedSolomon
3. Perfectly compatible C# version, https://github.com/l42111996/csharp-kcp, quickly build the network library before the game

# optimization:
1. Based on event-driven, make full use of multi-core
2. Optimize fastack logic and reduce traffic by 10%
3. Optimize the check function.
4. Optimize collection iterators.
5. Include fec to reduce latency
6. With crc32 check
7. Use the time wheel to optimize the CPU usage of a large number of connections
8. Use directbuf and object pool, no gc pressure
9. Increase the use of conv or ip + port to determine the uniqueness of the channel. The game is recommended to use conv and tcp configuration. [Related information](https://github.com/skywind3000/kcp/wiki/Cooperate-With-Tcp-Server)
10. Changes in export ip such as 4G switching wifi when adding games will not cause disconnection


# Relevant information
1. https://github.com/skywind3000/kcp The original ccp version of kcp
2. https://github.com/xtaci/kcp-go go version kcp, with a lot of optimization
3. https://github.com/Backblaze/JavaReedSolomon java version fec
4. https://github.com/LMAX-Exchange/disruptor High-performance inter-thread messaging library
5. https://github.com/JCTools/JCTools efficient concurrent library
6. https://github.com/szhnet/kcp-netty A kcp for java version
7. https://github.com/l42111996/csharp-kcp C# version of kcp based on dotNetty, perfectly compatible

# communicate with
QQ: 526167774