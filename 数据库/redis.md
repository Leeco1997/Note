## Redis

[TOC]

#### 为什么要使用Redis?

#### 1. 简介

+ 内存数据库 （读写速度：寄存器>内存>磁盘）
+  IO多路复用技术
+ 单线程架构 

**核心：高性能／高并发**

#### Redis的集群方案大致有三种：

1）redis cluster集群方案；

2）master/slave主从方案；

3）哨兵模式

##### 1.1 主从复制

**优点**：

+ 在主从复制基础上实现的读写分离，可以实现Redis的读负载均衡 ，最大化读负载能力 。
+  主从复制实现了数据的热备份，是持久化之外的一种数据冗余方式。

**缺点**： 当主机 Master 宕机以后，我们需要人工解决切换，比如使用slave of no one 。写服务无法使用，就需要手动去切换 。

##### 1.2 哨兵机制 

基本原理：**心跳机制** + **投票裁决**。

> + **监控**（ Monitoring ）：Sentinel 会定期检查主从服务器是否处于正常工作状态。 
>
> + **提醒**（ Notification ）：当被监控的某个 Redis 服务器出现异常时，Sentinel 可以通过 API 向管理员或者其他应用程序发送通知。
> + **自动故障迁移**（Automatic failover）：当一个主服务器不能正常工作时，Sentinel 会开始一次自动故障迁移操作，它会将失效主服务器的其中一个从服务器升级为新的主服务器， 并让失效主服务器的其他从服务器改为复制新的主服务器；当客户端试图连接失效的主服务 器时，集群也会向客户端返回新主服务器的 地址， 使得集群可以使用新主服务器代替失效 服务器。

 <img src="https://segmentfault.com/img/bVboQ0V?w=745&amp;h=465/view" alt="preview"  /> 

#### 2. redis 常见数据结构以及使用场景分析

| 类型   | 使用场景                                                     |
| ------ | :----------------------------------------------------------- |
| String | k-v                                                          |
| List   | 消息列表；Lrange读取从某个元素开始的多少个元素，可用于分页，微博下拉页面 |
| Set    | 可以自动重排；可以实现交集、并集、差集，例如共同好友         |
| Z -set | 使得集合中的元素能够按score进行有序排列。                    |
| Hash   | 存放个人信息                                                 |



#### 3. 基础知识点

##### 3.1 事务Transcation

- multi : 标记一个事务块的开始（ queued ）
- exec : 执行所有事务块的命令 （ 一旦执行exec后，之前加的监控锁都会被取消掉 ）　
- discard : 取消事务，放弃事务块中的所有命令

**不具有原子性**，**也不能回滚**。

**Watch**

>WATCH命令可以监控一个或多个键，一旦其中有一个键被修改（或删除），之后的事务就不会执行。监控一直持续到EXEC命令（事务中的命令是在EXEC之后才执行的，所以在MULTI命令后可以修改WATCH监控的键值）

顺序： 开始事务->命令入队->执行事务

##### 3.2 管道pipeline

##### 3.3 持久化机制

+ 快照（snapshotting，RDB）

  > 一种是内部调用`SAVE` `BGSAVE`把redis数据库中的数据保存成RDB文件
  > BGSAVE的时候会启动另一个单线程。

+ 只追加文件 （append-only ﬁle,AOF）---常用

  > 开启AOF持久化后每执行一条会更改Redis中的数据的命令，Redis就会将该命令写入硬盘中的AOF文件。为了兼顾数据和写入性能，用户可以考虑 `append` ` fsync` ` everysec`**选项 ，让Redis每秒同步一次AOF文件，性能不会受到影响。

##### 3.4 分布式锁

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/redis_分布式.jpg)

**举例子**

> 1.定时查询未支付的订单数量；
>
> 2.避免用户重复下单


1. Cluster 集群模式
2. **redis**和**memcached** 的区别

   + redis支持更丰富的数据类型：Redis不仅仅支持简单的k/v类型的数据，同时还提供 list，set，zset，hash等数据结构的存储。memcache支持简单的数据类型，String，二进制类型。
   + Redis支持数据的**持久化**，可以将内存中的数据保持在磁盘中，重启的时候可以再次加载进行使用,而 Memecache把数据全部存在内存之中。
   + 集群模式：memcached没有原生的集群模式，需要依靠客户端来实现往集群中分片写入数据；但是 redis 目前 是原生支持 cluster 模式的.
   + Memcached是**多线程**，**非阻塞IO复用的网络模型**；Redis使用**单线程**的**多路 IO 复用**模型。
   + redis支持事务，lua脚本。

##### 3.5 redis **设置过期时间以及内存淘汰机制**

##### 3.6 redis 4.0新增配置项lazy freeing



#### 4. 常见异常

##### 4.1 缓存穿透

> 查询的key在数据库中不存在，并且对于该key的请求量很大，会造成后台系统很大的压力。

##### 4.2 缓存雪崩

> 重启的时候或者大量的缓存在某一时刻突然失效。

##### 4.3 缓存预热

##### 4.4 缓存降级

##### 4.5 大value缓存监控

**常见问题：** 

1. 如何避免热点数据大量失效？

2. 如何保证Redis的高可用？

   答：**主从复制**和**Redis集群**。

#### 5. 常见问题

##### 5.1 如何实现分布式扩容及数据迁移？

1. 扩容：预估容量—添加新节点—数据迁移
2. **数据迁移**
   - 如何做到不影响业务的正常读写，即业务方对迁移是基本无感知的？
   - 当一条数据处于迁移中间状态时，如果此时客户端需要对该数据进行读写，如何处理？
   - 迁移过程中收到读操作，是读源节点还是目标节点，如何确保客户端不会读到脏数据？
   - 迁移过程中是写源节点还是写目标节点，如果确保不写错地方、不会由于迁移使得新值被旧值覆盖？
   - 迁移完成，如何通知客户端，将读写请求路由到新节点上？

 【**单个结点迁移**】<img src="https://static001.infoq.cn/resource/image/93/bb/931f862d26540b9a891dfbc1cf001cbb.png" alt="img" style="zoom:80%;" /> 

> **插槽**是数据分割的最小单位，所以数据迁移时最少要迁移一个插槽，迁移的最小粒度是一个 key，对每个 key 的迁移可以看成是一个原子操作，会短暂阻塞源节点和目标节点上对该 key 的读写，这样就使得不会出现迁移“中间状态”，即 **key 要么在源节点，要么在目标节点**。 

如图所示，正在迁移9号插槽，将flag置为“正在迁移”的状态。

1. 如果key1尚未迁移，则直接读写返回；
2. 如果key1已经被迁移，则返回 Ask 响应 （IP:Port），告知客户端需要重新发起一次访问。
3. 如果9号插槽已经全部迁移完成，客户端还继续访问，则返回一个Moved响应， 客户端收到 Moved 响应后可以重新获取集群状态并更新插槽信息，后续对 9 号插槽的访问就会到新节点上进行。 

【 **多节点并行数据迁移** 】

pipeline迁移功能？？？

##### 5.2 如何实现冷热数据交换存储？

1. **基本思想：**基于 key 访问次数 (LFU) 的热度统计算法识别出热点数据，并将热点数据保留在 Redis 中，对于无访问 / 访问次数少的数据则转存到 SSD 上，如果 SSD 上的 key 再次变热，则重新将其加载到 Redis 内存中。 
2. **难点：**SSD和Redis的读取速度相差太多。
   + 多进程异步非阻塞IO模型保证Redis的速度
   + 多版本key的惰性删除
   + 多线程读写SSD

【参考文章】

 https://blog.csdn.net/universsky2015/article/details/102728114 

 https://www.infoq.cn/article/jingdong-redis-practice 

##### 5.3 如何实现分布式事务？

