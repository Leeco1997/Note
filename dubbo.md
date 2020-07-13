## 微服务架构学习笔记

[TOC]

### 一、 基本概念

---

#### 1.1 去中心化

#### 1.2 SOA

> 将业务系统服务化，将不同的模块解耦，各种异构系统之间可以实现服务调用、消息交换、资源共享。

简单来说，就是将不同的业务功能拆分为不同的系统模块。

SOA的两大基石： **RPC**+**MQ** 

**使用RPC的好处？**

答：调用远程系统方法的时候不需要考虑底层的数据包格式和报文标准，以及交互的异常、错误等行为。

**使用MQ的好处？**【祥见MQ学习笔记】

+ 异步通信
+ **解耦 + 削峰**
+ 增加系统的可扩展能力，如果消费端的能力不足，直接新增几个消费者来处理业务即可，无需修改其他的东西。

#### 1.3 什么是微服务？

+ 由一些独立的的服务共同组成的应用系统；
+ 每个服务**单独部署**，运行在自己的进程中；
+ 每个服务都有自己**独立业务**；
+ 分布式管理。

**重点：低耦合 + 高内聚**

#### 1.4 如何解决高可用、高并发？

**高可用：** 

+ 限流 （Nginx、RateLimiter）
+ 降级和熔断 (Hystrix、sentinel)
+ 超时和重传 (timeout、retries)

**高并发**

+ 异步 （异步回调机制）
+ 缓存 （redis、memcache）
+ 池化

#### 1.5 如何保证事务的一致性？

#### 1.6 SpringCloud && dubbo

+ 基于tcp的RPC
  + tcp网络通信（socket）
  + 序列化与反序列化
  + 反射
  + 代理
+ 基于http的RPC
  + 网络通信（url）
  + 序列化（xml,json）

#### 1.7 通信协议

+ web Service

  基于http+xml

+ RestFul

  基于http +json (使用url地址，拿到一个对象，这个对象是json格式的)

+ RMI

  java内部的分布式通信协议

+ RPC

### 二、 Dubbo框架

---

#### 2.1 基本使用

![img](https://camo.githubusercontent.com/660e543510891254fa0ca6138af3350458aa0582/687474703a2f2f647562626f2e6170616368652e6f72672f696d672f6172636869746563747572652e706e67)

**调用过程**

1. 服务容器负责启动，加载，运行服务提供者。
2. 服务提供者在启动时，向注册中心注册自己提供的服务。
3. 服务消费者在启动时，向注册中心订阅自己所需的服务。
4. 注册中心返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者。
5. 服务消费者，从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。
6. 服务消费者和提供者，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心。

**特点**

- 基于透明接口的RPC
- 智能负载均衡
- 自动服务注册和发现
- 高可扩展性
- 运行时流量路由
- 可视化服务治理

简单来说，有以下优势：

+ 注册中心负责服务的注册与查找，类似于目录，不转发请求，压力较小
+ 注册中心使用软负载算法直接调用提供者
+ 注册中心通过长连接感知服务提供者的存在，如果服务提供者宕机，可以立即反馈给消费者
+ 如果监控中心和注册中心全部宕机，不影响正在运行的提供者和消费者，消费者、提供者和注册中心均有缓存
+ 可以不使用注册中心，直接使用消费者和提供者连接

**以前的远程调用的问题：**

+ 服务过多的时候，配置了很多的url，配置管理比较复杂。而且也分不清哪些应用必须先于哪些应用的启动。
+ 当服务容量过多的时候，不能预估总容量需要多少机器支撑。

#### 2.2 集群容错

**Failover Cluster**

失败自动切换，当出现失败，重试其它服务器 。通常用于读操作，但重试会带来更长延迟。可通过 `retries="2"` 来设置重试次数(不含第一次)。

重试次数配置如下：

```xml
<dubbo:service retries="2" />
```

或

```xml
<dubbo:reference retries="2" />
```

或

```xml
<dubbo:reference>
    <dubbo:method name="findFoo" retries="2" />
</dubbo:reference>
```

**Failfast Cluster**

快速失败，只发起一次调用，失败立即报错。通常用于非幂等性的写操作，比如新增记录。

**Failsafe Cluster**

失败安全，出现异常时，直接忽略。通常用于写入审计日志等操作。

**Failback Cluster**

失败自动恢复，后台记录失败请求，定时重发。通常用于消息通知操作。

**Forking Cluster**

并行调用多个服务器，只要一个成功即返回。通常用于实时性要求较高的读操作，但需要浪费更多服务资源。可通过 `forks="2"` 来设置最大并行数。

**Broadcast Cluster**

广播调用所有提供者，逐个调用，任意一台报错则报错 [[2\]](http://dubbo.apache.org/zh-cn/docs/user/demos/fault-tolerent-strategy.html#fn2)。通常用于通知所有提供者更新缓存或日志等本地资源信息。

 按照以下示例在服务提供方和消费方配置集群模式

```xml
<dubbo:service cluster="failsafe" />
```

或

```xml
<dubbo:reference cluster="failsafe" />
```

#### 2.3 负载均衡

`dubbo`默认 `random` 随机调用;

`nginx`默认使用`加权轮询算法`.

**Random (权重+随机)**

* 按照概率使用权重分配比较均匀，有利于动态调整权重。

```java
 public HashMap<String, Integer> map = new HashMap<>() {
        {
            put("192.168.1.1", 2);
            put("192.168.1.2", 7);
            put("192.168.1.3", 1);
        }
    };

```



**RoundRobin(加权轮询)**

* 避免某些性能差的机器积累太多请求。

**LeastActive (最少活跃数)**

* Nginx的负载均衡默认算法是加权轮询算法。

**Consistenthash (一致性哈希)**

- **一致性 Hash**，相同参数的请求总是发到同一提供者。
- 当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。

---

**算法：** 对2^32取模,得到的结果是0~2^32-1,然后形成一个hash环。将数据key使用相同的函数Hash计算出哈希值，并确定此数据在环上的位置，从此位置**沿环顺时针“行走”**，**第一台**遇到的服务器就是其应该定位到的服务器！

**优势：**

1. **动态改变服务器的数量**

   如果一台服务器宕机了，只会影响与它顺时针相邻的最近的服务器。增加一台服务器，也只会影响与逆时针的第一台服务器之间的数据。

2. **单调性、分散性、平衡性**

   

**缺点：服务器太少的情况下，容易造成数据倾斜的问题。**

**解决方案**： 每个结点计算多个hash，数据定位算法不变，只是增加一步**虚拟节点** ->实际节点的映射。 每个结点计算多个hash，数据定位算法不变，只是增加一步**虚拟节点** ->实际节点的映射。

---

**分布式的集群**

+ 取模

+ 哈希，ip哈希和url哈希（源地址散列）

  > **Bug: 如果某一台机器宕机，会导致大量的id与服务器的映射关系失效**.

+ **一致性hash**

#### 2.4 线程模型

```xml
<dubbo:protocol name="dubbo" dispatcher="all" threadpool="fixed" threads="100" />
```

**Dispatcher**

- `all` 所有消息都派发到线程池，包括请求，响应，连接事件，断开事件，心跳等。
- `direct` 所有消息都不派发到线程池，全部在 IO 线程上直接执行。
- `message` 只有请求响应消息派发到线程池，其它连接断开事件，心跳等消息，直接在 IO 线程上执行。
- `execution` 只有请求消息派发到线程池，不含响应，响应和其它连接断开事件，心跳等消息，直接在 IO 线程上执行。
- `connection` 在 IO 线程上，将连接断开事件放入队列，有序逐个执行，其它消息派发到线程池。

**ThreadPool**

- `fixed` 固定大小线程池，启动时建立线程，不关闭，一直持有。(缺省)
- `cached` 缓存线程池，空闲一分钟自动删除，需要时重建。
- `limited` 可伸缩线程池，但池中的线程数只会增长不会收缩。只增长不收缩的目的是为了避免收缩时突然来了大流量引起的性能问题。
- `eager` 优先创建`Worker`线程池。在任务数量大于`corePoolSize`但是小于`maximumPoolSize`时，优先创建`Worker`来处理任务。当任务数量大于`maximumPoolSize`时，将任务放入阻塞队列中。阻塞队列充满时抛出`RejectedExecutionException`。(相比于`cached`:`cached`在任务数量超过`maximumPoolSize`时直接抛出异常而不是将任务放入阻塞队列)

#### 2.5 多协议配置

```java
 <!-- 多协议配置 -->
    <dubbo:protocol name="dubbo" port="20880" />
    <dubbo:protocol name="hessian" port="8080" />
    <!-- 使用多个协议暴露服务 -->
    <dubbo:service id="helloService" interface="com.alibaba.hello.api.HelloService" version="1.0.0" protocol="dubbo,hessian" />

```

#### 2.6 泛型化

使用Spring

```java
<bean id="genericService" class="com.foo.MyGenericService" />
<dubbo:service interface="com.foo.BarService" ref="genericService" />
```

使用API

```java
... 
// 用org.apache.dubbo.rpc.service.GenericService可以替代所有接口实现 
GenericService xxxService = new XxxGenericService(); 

// 该实例很重量，里面封装了所有与注册中心及服务提供方连接，请缓存 
ServiceConfig<GenericService> service = new ServiceConfig<GenericService>();
// 弱类型接口名 
service.setInterface("com.xxx.XxxService");  
service.setVersion("1.0.0"); 
// 指向一个通用服务实现 
service.setRef(xxxService); 
 
// 暴露及注册服务 
service.export();
```

#### 2.7 服务降级

向注册中心写入动态配置覆盖规则：

```java
RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://10.20.153.10:2181"));
registry.register(URL.valueOf("override://0.0.0.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&mock=force:return+null"));
```

其中：

- `mock=force:return+null` 表示消费方对该服务的方法调用都直接返回 null 值，不发起远程调用。用来屏蔽不重要服务不可用时对调用方的影响。
- 还可以改为 `mock=fail:return+null` 表示消费方对该服务的方法调用在失败后，再返回 null 值，不抛异常。用来容忍不重要服务不稳定时对调用方的影响。

#### 2.8 服务停机

**服务提供者**

- 停止时，先标记为不接收新请求，新请求过来时直接报错，让客户端重试其它机器。
- 然后，检测线程池中的线程是否正在运行，如果有，等待所有线程执行完成，除非超时，则强制关闭。

**服务消费方**

- 停止时，不再发起新的调用请求，所有新的调用在客户端即报错。
- 然后，检测有没有请求的响应还没有返回，等待响应返回，除非超时，则强制关闭。

缺省的超时时间是`1000ms` 

```xml
dubbo.service.shutdown.wait = 15000
```

#### 2.9 配置的覆盖规则

1) 方法级别配置优于接口级别，即小 Scope 优先

 2) Consumer 端配置优于 Provider 端配置，优于全局配置，最后是 Dubbo 硬编码的配置值

> Dubbo可以自动加载classpath根目录下的dubbo.properties，但是你同样可以使用JVM参数来指定路径：`-Ddubbo.properties.file=xxx.properties`。

如果classpath下有两个dubbo.properties，会随机选择一个加载。

**优先级从高到低：** 【dubbo.xml会覆盖dubbo.properties】

![properties-override](http://dubbo.apache.org/docs/zh-cn/user/sources/images/dubbo-properties-override.jpg)

#### 2.10 推荐用法

+ 在provider端尽量多配置consumer的属性

   `retries 缺省值是2` `timeout` `loadbalance` `actives`

+ 配置缓存文件，在重启的过程中，如果注册中心不可用，可以读取缓存文件。

  

### 三、核心原理

#### 3.1 SPI机制

> 将接口实现类的全限定类名配置在文件中，并且由服务器加载读取配置文件，加载实现类，可以实现动态替换实现类。