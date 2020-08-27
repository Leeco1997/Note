##  多线程

### 一、线程

#### 1.1 start()/run()

#### 1.2 sleep()/ wait()/yield()/join()

#### 1.3 中断

 interrupt/ isInterrupted()/interrupted()

+ 打断 sleep(),wait(),join()的线程

  抛出InterruptedExecption,清空标志位。

+ 打断正常运行的线程

  继续运行，标记为置为true.

+ interrupted()调用后，会把标志位清除。(第二次调用的时候就是返回`false`)

#### 1.4 终止线程

1. **stop()---不推荐使用**

   > 如果当前线程没有释放锁，那么会**浪费锁资源**。

2. **System.exit(0);**

   > 停止整个进程。

3. **不推荐使用的方式**

   stop()/suspend()/resume()

   建议使用： interrupt()/wait()/notify()

#### 1.5 park/unpark

> permit不能叠加，也就是说permit的个数要么是0，要么是1。

![5](https://raw.githubusercontent.com/Leeco1997/images/master/img/image-20200721155352706.png)

#### 1.6 守护线程

当没有非守护线程存在的时候，则虚拟机会退出。

应用场景

+ 垃圾回收线程
+ Tomcat中的接受请求和分发请求的线程。

#### 1.7 线程的状态

从操作系统的角度来讲，只有五种。

**new  > Runnable/Running > wait > Blocked**

```java
public Enum State{
    NEW,
    RUNNABLE,
    BLOCKED,
    WAIT,
    TIMED_WAITING,
    TERMINATED;
}
```

#### 1.8 ThreadLocal

> 只有当前线程可以访问，具有线程隔离作用。

应用场景：Spring事务中对于数据库的连接，每一次的事务必须保证使用的是同一个连接，所以可以使用ThreadLocal。

```java
    public void set(T value) {
        Thread t = Thread.currentThread();
        //map 里面存放的是 <ThreadLocal,value>
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```

```java
    static class ThreadLocalMap {
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;
			//K是一个弱引用，指向ThreadLocal对象
            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }
    }
```

![image-20200720213144174](https://raw.githubusercontent.com/Leeco1997/images/master/img/weakReference.jpg)

> 为了防止内存泄漏，需要手动删除。

``` map.remove()/map.set()/map.get()```都可以实现自动清楚无效`Entry`。---这里可以结合线性探测法来考虑。



#### 1.10  Balking 设计模式

```java
public class MonitorService {
    // 用来表示是否已经有线程已经在执行启动了    
    private volatile boolean starting;
    public void start() {      
        log.info("尝试启动监控线程...");        
        synchronized (this) {           
            if (starting) {               
                return;            
            }            
            starting = true;        
        }                
        // 真正启动监控线程...    } 
    }

```

#### 1.11  hook线程

> 在JVM退出的时候，会被执行。如果使用kill pid xx，则不会执行；
>
> hook线程可以进行一些释放数据库连接、关闭socket连接的工作；

### 二、线程池

##### :grey_question: 为什么要使用多线程？如何设置线程的个数？

**为了最大化的利用IO/CPU.**

+ CPU密集型程序

  **最大化的利用CPU核心数目  cpu核心数目+1**。

  > 计算(CPU)密集型的线程恰好在某时因为发生一个页错误或者因其他原因而暂停，刚好**有一个“额外”的线程**，可以确保在这种情况下CPU周期不会中断工作。

+ IO密集型程序

  在进行IO操作的时候，CPU是空闲的。

  **最佳线程数目 = 1/cpu利用率  = 1+ IO耗时/CPU耗时**

**单线程一定慢吗？**

> 不一定。比如Redis。因为它是基于内存操作，这种情况下，单线程可以很高效的利用CPU

#### 2.1 ThreadPoolExectuor

##### 线程状态

```xml
*   RUNNING  111:     Accept new tasks and process queued tasks 
*   SHUTDOWN 000:     Don't accept new tasks, but process queued tasks
*   STOP     001:     Don't accept new tasks, don't process queued tasks,
*                     and interrupt in-progress tasks
*   TIDYING  010:     All tasks have terminated, workerCount is zero,
*                     the thread transitioning to state TIDYING
*                     will run the terminated() hook method
*   TERMINATED 011:   terminated() has completed
```

```java
//使用ctl表示状态的任务个数,合二为一，减少cas的操作  
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
//或运算，高3位表示状态
private static int ctlOf(int rs, int wc) { return rs | wc; }
```

##### 构造方法

```java
//keepAliveTime是大于coreSize的线程，最大空闲时间  
//maxSize只有在有界队列中才有效
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        //……
    }
```

##### 拒绝策略

+ AbortPolicy  抛出异常
+ DiscardPolicy
+ DiscardOldestPolicy

> `dubbo`在抛出异常的时候会记录日志，并且dump栈信息；
>
> `netty`的实现是创建一个新的队列；
>
> `AMQ`是超时等待60s放入队列；
>
> `PinPoint`使用一个拒绝策略链，依次使用每一种拒绝策略

##### newFixedExecutor

`coreSize = maxSize`

##### newCachedExecutor

```java
//使用同步队列，core Size= 0,maxSize = Integer.MAX_VALUE
public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>(),
                                      threadFactory);
    }
```

> 线程数量可以不断增加，没有上限，空闲后1分钟会自动释放。
>
> 适合数量多，但是任务时间短的任务。

##### newSIngledExecutor

```java
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
       //使用了装饰者模式，防止调用里的方法
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>(),
                                    threadFactory));
    }
```



> 保证任务的执行可以串行执行，任务大于1的时候，会返给无界队列。

vs 单线程：

+ 当某个任务运行异常的时候，会自动重新创建一个线程。换句话说，也就是可以保证有一个线程可用。

### 面试问题

#### ThreadLocal

1、大体说下你对 ThreadLocal的理解？

ThreadLocal可以提供线程局部变量，也就是说每个线程可以拥有一份自己的一个副本变量。多个线程之前互不干扰。

2、ThreadLocal的原理是什么呢？

追问1：ThreadLocalMap内存储的是什么？

```java
//每个Thread里面有一个threadLocals对象，指向堆中的ThreadLocalMap
ThreadLocal.ThreadLocalMap threadLocals = null;
//Map里面存放的Entry<K,V>  
static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }
```

追问2：ThreadLocal它是怎样做到线程之间互不干扰的呢？

每个线程的`ThreadLoclaMap`会给对应的ThreadLocal生成一份自己的数据。

3、JDK8 版本的ThreadLocal设计有什么优势相比更早之前的老版本（不指jdk1.7，比jdk1.7还要老，可以认为是ThreadLocal第一版）？

+ 线程被销毁的时候，对应的ThreadLocalMap会在下一次GC的时候被回收。

+ Entry的Key是弱引用，弱引用不参与root算法；

4、ThreadLocalMap 存放数据时，数据的hash值是从Object.hashCode()拿到的，还是其它方式？为什么？

重写了hashcode，采用的黄金分割数。



5、为什么ThreadLocal选择自定义一款Map而没有沿用JDK中的HashMap？

+ key可以设置为弱引用，不影响GC；
+ ThreadLocalMap在`get`\ `set`的时候有清理过期数据的功能，可以一定程度上的防止内存泄漏；

6、每个线程的 ThreadLocalMap对象 是什么时候创建的呢？

+ 采用延迟加载，在第一次`get` /`set`的时候创建；
+ 只会初始化一次

7、ThreadLocalMap 底层存储数据的数组长度 初始化是多少？16
追问1：这个数组大小为什么必须为 2的次方数？

追问2：ThreadLocalMap的扩容阈值是多少呢？

追问3：ThreadLocalMap达到扩容阈值一定会扩容么？

追问4：扩容算法 你简单说一说。

+ 2的幂次方-1全是1，与运算得到的结果是0~2的幂-1，与运算的效率高于取模运算。

+ 扩容的阈值是当前数组的2/3;

+ 扩容之前，会先进一次rehash,清除过期的数据；全表扫描以后，如果仍然达到阈值的3/4，则会扩容；

+ ```java
  int newLen = oldLen * 2; //新的数组长度
  ```

  新建数组 --> 复制元素（重新计算元素下标，线性探测法）--> 更新threadlocalMap对象的引用 --> 计算下一次出发的阈值

8、ThreadLocalMap对象的 get逻辑，你说下。
追问1：假设get首次未命中，向下迭代查找时，碰到过期数据了，怎么处理？

+ 触发一次探测式数据清理，一直迭代到 `slot == null`

追问2：探测式清理过期数据，向下迭代过程中碰到正常数据，怎么处理？

+ 重新计算index

9、ThreadLocalMap set数据流程，大体说一下。
追问1：set数据时碰到过期数据了，需要做替换逻辑，这个替换逻辑是怎么做的？