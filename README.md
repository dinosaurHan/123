#Betting-server工程介绍<br>

##1.代码说明<br>

###1.1、目录结构
Betting-server/<br>
├── src/<br>
│   ├── main/<br>
│   │   ├── java/<br>
│   │   │   ├── com/<br>
│   │   │   │   ├── betbrain/<br>
│   │   │   │   │   │   ├── BettingServer.java<br>
│   │   │   │   │   │   ├── handler/<br>
│   │   │   │   │   │   │   ├── SessionHandler.java<br>
│   │   │   │   │   │   │   ├── StakeHandler.java<br>
│   │   │   │   │   │   │   ├── HighStakesHandler.java<br>
│   │   │   │   │   │   ├── model/<br>
│   │   │   │   │   │   │   ├── Session.java<br>
│   │   │   │   │   │   ├── server/<br>
│   │   │   │   │   │   │   ├── Handler.java<br>
│   │   │   │   │   │   │   ├── Router.java<br>
│   │   │   │   │   │   │   ├── ServiceUnavailableRejectionHandler.java<br>
│   │   │   │   │   │   ├── service/<br>
│   │   │   │   │   │   │   ├── SessionService.java<br>
│   │   │   │   │   │   │   ├── StakeService.java<br>
│   │   │   │   │   │   ├── util/<br>
│   │   │   │   │   │   │   ├── SessionKeyGenerator.java<br>
├── README.md<br>
├── pom.xml<br>

###1.2、功能概述<br>
####1.2.1、关于服务启动和请求路由<br>
服务启动类位于BettingServer中，主类运行后，会监听8001端口，等待客户端请求；收到请求后，会根据url path路由到对应的处理类。如http://localhost:8001/999111/session，则会将请求路由到SessionHandler来处理，路由过程则是根据url正则匹配规则来实现的，以解决<br>
HttpServer.createContext的前缀匹配问题（具体参考Router.java)，停机时会检查存量请求，处理完成后才会shutdownNow。为了方便测试，添加了一些监控信息在代码中，CR时可以忽略。<br>

####1.2.1、密钥的生成、存储和过期设计<br>
考虑到单实例服务器内存、CPU资源是有限的，在设计时，考虑了数据的过期淘汰；在com.betbrain.service.SessionService中的成员变量sessions中，我们设计了过期时间和定时任务处理过期淘汰，另外，请求过来时也会实时判断过期时间（参考：com.betbrain.service.SessionService；<br>

####1.3.1、投注记录数据的存储、排序和查询<br>
#####1.3.1.1 实现说明<br>
关于投注数据的存储，考虑到需要根据投注id，返回最大的Top 20的投注记录，同时考虑到如果将所有的投注数据放到同一个位置存储，对于查询、更新、排序等都将会造成更大的开销，所以，实现时将投注数据按照betId进行了分片存储，减少单个数据结构的大小，以此减少每次查询时需要检索的数据量级。<br>
此外，为了降低每次查询top20接口的耗时，将top20的数据进行了本地缓存；计算top20时，考虑空间和时间的效率，我们将top 20的数据存储到了最小堆PriorityQueue中，只需要维护前N个元素，加快排序的速度和降低内存的开销。<br>
同时带来了新的问题，每次新的投注发生时，都需要从全量数据中计算出top20的数据，这可能对投注接口的性能是有损的，实现时，也考虑了是否应该将每个客户在每个betId下的最大投注记录单独存储，这样可以降低计算top20时需要检索的数据量级，因为暂不清楚后续是否会有需要按照客户id查询在所有betId下的投注记录，<br>
所以并未设计反向索引，存储每个customerId在每个betId下的投注记录这种数据结构，所以这版实现仅限于在betId-->customerId-->投注记录这个结构中进行存储、计算、排序和缓存；与此同时，这里还有一个矛盾点，是否应该引入top20缓存这种数据结构，因为他带来了额外的存储、维护的开销，这里可能需要考虑投注和<br>
获取top20投注记录这2个接口的频次关系，如果查询top20投注记录这个接口的频次远远大于投注的请求频率，我们做这个事情是有意义的，反之，不如直接在查询时在重新计算，优先保证投注接口的吞吐。同时，开发时也考虑到了是否top20的数据需要实时更新，是否可以接受top20的数据没那么实时，<br>
这样我们就可以在投注时进行异步更新，降低对投注接口的性能影响，这里可能需要面试官在CR时给与一定的帮助和建议:)。<br>
<br>
#####1.3.1.2 可能存在的问题<br>
上面提到的问题在进行压力测试时，也体现在数据表现上，投注接口的QPS相对其他2个接口是最低的。此外，其他存在可能的问题以及优化方向：<br>
A.投注时，compute操作在每个betId上加了分片锁（依赖ConcurrentHashMap的底层实现），这对不同betId%share_count的没有影响，对于同一个betId%share_count下的并发操作，可能带来问题，这里的优化方向是使用更细力度的锁、使用更多的分片等，<br>
不要在betId这种比较粗粒度的维度进行并发限制，降低系统吞吐；<br>
B.如果可以接受投注记录数据的低延迟，可以考虑异步更新，可以更好的提高投注接口的吞吐，同时需要注意异步更新成功率的监控；<br>

#####版本以来：jdk1.8

