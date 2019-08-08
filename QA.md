# 测试遇到过的问题和经验
1. 大量客户端测试时服务器或者客户端收不到对方信息，但发送成功，因为bind时候bind到了Ipv6,相同的的ipv4端口被其他进程占用导致，
在启动参数加上 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false 禁用ipv6
2. 大吞吐量测试时发现系统丢包严重，epoll模型接收只占用一个线程且cpu泡满，后开启SO_REUSEPORT参数优化为多核，参考https://www.jianshu.com/p/61df929aa98b
3. 大量连接时定时任务使用java api定时线程耗时太多，优化为时间轮