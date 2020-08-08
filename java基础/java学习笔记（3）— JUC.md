##  JAVA 并发编程
[TOC]
### 一、 基础知识

#### 1.1 上下文切换
什么时候发生上下文切换？
+ CPU时间片使用结束
+ 垃圾回收的时候（stop the world）
+ 有更高优先级的线程来了
+ sleep，yield，wait（自身调用某些信息）
> Cpu通过时间片分配算法来循环执行任务，让我们感觉多个线程是同时执行的，时间片一般是几十毫秒（ms）.

**使用程序计数器记录下一条指令的位置**
**使用栈帧记录 局部变量，返回记录，锁信息，操作数栈**
一般在设计的时间，尽量减少上下文切换：
+ 无锁并发编程，避免使用锁
+ CAS算法，使用Atomic包
+ 使用最少线程，减少waiting线程的数量
+ 协程：使用单线程完成多任务的调度

#### 1.2  死锁
##### 1.2.1 写一个死锁代码
##### 1.2.2 如何解决死锁？
+ 避免一个线程获得多个锁资源
+ 使用定时锁，lock.tryLock()
+ 对于数据库的加锁和解锁必须放在一个数据库连接里，否则会出现解锁失败的情况

#### 1.3 进程、线程
> **进程是资源分配的最小单位，线程是程序执行的最小单位。**
>
> 对于计算机来说，每一个任务就是一个进程，在每一个进程中至少包含一个线程。每个线程拥有自己的**局部变量表、程序计数器**（指向正在执行的指令的指针）以及各自的**生命周期**。

#### 1.4 CPU
##### 1.4.1 三级缓存结构

cpu的速度是硬盘速度的100万倍

![image-20200713190957588](https://raw.githubusercontent.com/Leeco1997/images/master/img/cache.jpg)

> 查询元素的时候，寻找顺序依次为L1、L2、L3、main Memory;
>
> 缓存行对齐：Cache line : 64字节
>
> disruptor:底层的`cusor`就是使用的缓存行对齐，循环队列。

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/cacheline.jpg)

##### 1.4.2 伪共享

**引发原因：** 多个cpu同时修改缓存中的不同部分，从而导致其中一个的 cpu操作失效。可能会引发内存顺序冲突，cpu则必须清空流水线。

简单来说，其实就是数据失效是以缓存行为单位，如果需要修改的数据长度不足一个缓存行，则会导致其他元素失效。

java7使用`追加字节`的方式（一段long类型的变量)。
Java8使用`@sun.misc.Contended`避免伪共享。使用此注解的对象或字段的前后各增加128字节大小的padding，使用2倍于大多数硬件缓存行的大小来避免相邻扇区预取导致的伪共享冲突。

```java
public class FalseSharing implements Runnable {
    // long padding避免false sharing
    // 按理说jdk7以后long padding应该被优化掉了，但是从测试结果看padding仍然起作用
    public final static class VolatileLong2 {
        volatile long p0, p1, p2, p3, p4, p5, p6;
        public volatile long value = 0L;
        volatile long q0, q1, q2, q3, q4, q5, q6;
    }

    /**
     * jdk8新特性，Contended注解避免false sharing
     * Restricted on user classpath
     * Unlock: -XX:-RestrictContended
     */
    @sun.misc.Contended
    public final static class VolatileLong3 {
        public volatile long value = 0L;
    }
    }
```



#### 1.5 逃逸分析

> 在内存分配的时候，可以确定该对象的适用范围只会当前的方法中用到，则会被配到栈上，不会分配到堆里。



### 二、 java并发底层实现原理

#### 2.1 CAS （乐观锁）
##### 2.1.1 什么是CAS?
 > **compare and swap:**  假设内存中的原数据值是 v , 旧的预期值是 A ，操作后的值是B
 > 先比较 V ==A，相等则修改V = B;
 > 返回操作是否成功

 ##### 2.1.2 实现原理
```C++
inline jint  Atomic::cmpxchg    (jint  exchange_value, volatile jint*  dest, jint   compare_value, cmpxchg_memory_order order) {
  int mp = os::is_MP();
  __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp)
                    : "cc", "memory");
  return exchange_value;
}
```
##### 2.1.3 在实际中的应用
+ 自旋锁
+ 线程获取／释放锁的过程，循环使用CAS来获取锁。
 ##### 2.1.4 CAS实现原子操作的三大问题
+ **ABA**　
１.　版本号／时间戳
２.　java中使用AtomicStampedReference.compareAndSet(),先检查当前引用是否等于预期引用，再检查当前标志是否等于预期标志。`expectedReference == current.reference && expectedStamp == current.stamp `

+ **循环时间长导致CPU消耗大**
使用pause指令：延迟流水线指令，减少cpu消耗；避免在退出的时候引起CPU流水线清空（CPU Pipeline Flush）

+ **只能保证一个共享变量的原子性。**
 将多个共享变量合并为一个

#### 2.2 volatile 

##### 2.2.1 实现原理

1. 关键字：volatile

2. 字节码层面`ACC_VOLATILE`

3. JVM 层面

   `StoreStoreBarrier`  volatile写  `StoreLoadBarrier`

   `LoadloadBarrier` volatile读 `LoadStoreBarrier`

4. hotspot

   ```c++
   inline void OrderAccess::fence() {
     if (os::is_MP()) {
       // always use locked addl since mfence is sometimes expensive
   #ifdef AMD64
       __asm__ volatile ("lock; addl $0,0(%%rsp)" : : : "cc", "memory"); //lock指令
   #else
       __asm__ volatile ("lock; addl $0,0(%%esp)" : : : "cc", "memory");
   #endif
     }
   }
   ```

   >Lock前缀指令
   >
   >1. Lock指令一般不锁总线，而是锁住缓存，并且写回到内存，然后使用MESI来保证一致性；
   >2. 一个处理器的缓存写回到内存会导致其他处理器的缓存无效。

   **保证数据的可见性，防止指令重排，但是不能保证数据的原子性。**

##### 2.2.2 如何保证原子性呢？
+ Java中实现原子性操作 --> 调用Unsafe中的compareAndSwap方法--> JNI中的Atomic ::cmpxchg --> 使用汇编中的Lock指令
+ 处理器实现原子性操作-->锁总线 or 锁缓存 + [MESI]

##### 2.2.3 MESI缓存一致性协议

+ 独占（exclusive）：仅当前处理器拥有该缓存行，并且没有修改过，是最新的值，如果有其他CPU来读取该缓存行的值，则变成S.。
+ 共享（share）：有多个处理器拥有该缓存行，每个处理器都没有修改过缓存，是最新的值。
+ 修改（modified）：仅当前处理器拥有该缓存行，并且缓存行被修改过了，一定时间内会写回主存，回写成功状态会变为E。
+ 失效（invalid）：缓存行被其他处理器修改过，该值不是最新的值，需要读取主存上最新的值。

**M和E的区别是:**  M状态的数据是和内存不一致的，E状态的数据是和内存一致的。



![image-20200713190900497](E:\CS\cs-note\img\mesi.jpg)


##### 2.2.4 内存屏障

禁止屏障两侧的指令重排；
**Load Barrier 读屏障**  / **Stroe Barrier 写屏障**

> `storeLoad` 需要缓冲区的数据全部刷新回内存。 ---【cpu底层使用lock指令】

##### 2.2.5 happens - before

+ 程序次序规则 ： 前面的先发生
+ 锁定规则：先加锁后解锁
+ volatile变量规则：先写后读



#### 2.3 synchronized

jdk1.6对于synchronized做了很大的优化。

![image-20200720110254058](C:\Users\pipi\AppData\Roaming\Typora\typora-user-images\image-20200720110254058.png)

##### 2.3.1 java 头对象
java的对象头由以下三部分组成：

---
> **Mark Word** [hashcode、锁信息、分代年龄]

> **Class MetaData Address** [存储到对象类型数据的指针]

> **Array Length** [如果当前对象是数组的话]

---
**Mark Word的存储结构**

| 偏向锁标识位 | 锁标识位 | 锁状态   | 存储内容                     |
| :----------- | :------- | :------- | :--------------------------- |
| 0            | 01       | 未锁定   | hash code(31),年龄(4)        |
| 1            | 01       | 偏向锁   | 线程ID(54),时间戳(2),年龄(4) |
| 无           | 00       | 轻量级锁 | 栈中锁记录的指针(64)         |
| 无           | 10       | 重量级锁 | monitor的指针(64)            |
| 无           | 11       | GC标记   | 空，不需要记录信息           |

##### 2.3.2 锁的升级
###### 1. 偏向锁

使用范围：绝大部分情况下不存在竞争、只有一个线程在尝试获取锁的场景，偏向于第一个访问锁。通过减少CAS操作的数量，提高应用性能。

+ **获取锁的过程**

（1）访问Mark Word中偏向锁的标识是否设置成1，锁标志位是否为01，确认为可偏向状态。
（2）如果为可偏向状态，则测试线程ID是否指向当前线程，如果是，进入步骤5，否则进入步骤3。
（3）**如果线程ID并未指向当前线程，则通过CAS操作竞争锁**。如果竞争成功，则将Mark Word中线程ID设置为当前线程ID，然后执行5；如果竞争失败，执行4。
（4）如果CAS获取偏向锁失败，则表示有竞争。当到达全局安全点（safepoint）时获得偏向锁的线程被挂起，偏向锁**升级为轻量级锁**，然后被阻塞在安全点的线程继续往下执行同步代码。（撤销偏向锁的时候会导致**stop the word**）
（5）执行同步代码。

+ **释放锁**
>偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，线程不会主动去释放偏向锁。
>偏向锁的撤销，需要等待**全局安全点**（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态，撤销偏向锁后恢复到未锁定（标志位为“01”）或轻量级锁（标志位为“00”）的状态。

 + **JVM关闭偏向锁**

开启偏向锁：-XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0
关闭偏向锁：-XX:-UseBiasedLocking

```java
Object obj = new Object();
// 计算lhashcode,会禁用掉偏向锁，直接变成无锁状态
obg.hashcode(); 
synchronized(obj){
    //dosomething
}
```

###### 2. 轻量级锁

+ **加锁的过程**
1. 在进入同步代码块以前，如果当前状态是无锁状态`01`,JVM会在当前线程的栈帧中创建一个 `Lock Record` 的空间，用于存储锁对象目前的Mark Word的拷贝`Displaced Mark Word`。
2. 使用CAS将对象头的 `Mark word`  替换为指向`Lock Record` 的指针,并将`Lock Record` 里的`owner`指针指向` mark word`。
3. 如果更新成功了，则表示该线程拥有该对象的锁，并且将 `Mark Word`的锁标记为置为`00`。如果更新失败了，则JVM会先判断当前线程是否已经拥有该锁。若未拥有，则说明有竞争，当前线程开始自选获取锁，如果自旋失败在，则升级为重量级锁`10`。

+ **释放锁的过程**
1. 使用CAS操作将Displaced Mark Word替换回对象头；
2. 若替换成功，说明没有竞争，成功释放；
3. 若替换失败，说明此时存在竞争，则升级为重量级锁。

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/cas.jpg)

###### 3. 重量级锁

>如果轻量级锁自旋到达阈值后，没有获取到锁，就会升级为重量级锁.
>
>只有重量级锁可以调用wait()、notify()；

使用CAS更新mark Word失败，则会为Object对象申请monitor锁，使锁的标记为`10`.

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/monitor.jpg)



##### 2.3.3 锁的优化

1. 减小锁的粒度 

   `LinkedBlockingQueue`
   在队列头入队，在队列尾出队，入队和出队使用不同的锁，相对于LinkedBlockingArray只有一个锁效率要高；

    `ReentrantReadWriteLock`
   读操作加读锁，可以并发读，写操作使用写锁，只能单线程写；

2. 锁的粗化与锁消除

>**锁粗化**是将多次拼接在一起的加锁解锁合并为一次加锁、解锁。例如：
``` java
 for(int i=0;i<size;i++){
 synchronized(lock){
  //do something
 }
```

>**锁消除**是发生在编译时期，如果一段代码中，堆中的对象不会被其他线程访问，则可以不使用同步加锁。
>`String string = str1 + str2 ;`

##### 2.2.4 锁的使用方式

```java
 //修饰静态方法，其实就是锁住整个class类对象
public static synchronized void method(){}

//下面两种方式都是一样的，锁住调用该方法的对象
public synchronized void method(){}
public  void method(){
    synchronized(this){}
}
```

###  :zap:常见面试问题？

#### 1. synchronized的底层实现？

+ java代码： `synchronized`
+ 使用javac/javap命令可以看见字节码文件，`monitorEnter`、`monitorExit`
+ jvm执行过程中`自动锁升级`
+ 汇编语言 `lock comxchg`

#### 2. 如何实现高并发？解决思路？

1. 扩容
2. 缓存
3. 消息队列 AMQ,Kafka,RabitMQ
4. 应用拆分 Dubbo
5. 限流
6. 服务降级或者服务熔断
7. 数据库分库分表
8. 任务调度分布式elastic-job 

## 4. 并发容器和框架

###  线程安全的类

+ String/Integer(包装类)
+ StrignBuffer
+ Random
+ vector
+ HashTable
+ juc下的类



### 4.1 Lock
> JDk1.5以后增加了Lock接口，提供了与synchronized类似的功能。
##### 4.1.1 Lock VS Synchronized
**Lock的优势：**
+ **底层：** volatile + CAS (乐观锁)
+  非阻塞的获取锁，`tryLock()`;
+ 能够响应中断lockInterruptibliy()，当获取锁的线程被中断的时候，会抛出中断异常，并且释放锁；
+ 超时的获取锁，如果在指定的时间里没有获取锁，则返回false；
+ 可以使用Condtion；
```java
Lock lock = new ReentrantLock();
lock.lock();
 try {
      …
   }  catch{
     //一定要手动释放锁
    lock.unlock();
 }
```
**Synchronized的优势：**
+ **底层**的指令是monitorenter和monitorexit，当锁的计数器=0的时候，释放锁资源。(可重入锁、悲观锁)
+ 可以用于实例方法，静态方法，同步代码块；
##### 4.1.2 ReentrantLock          
> 可重入锁，一个线程可对临界资源重复加锁。
>
> **底层：AQS  公平锁&&非公平锁**

**实现方式**
```java
公平锁
非公平锁
```
**获取锁的过程**
```java
// 第一步：调用ReentrantLockLock的lock()
     final void lock() {
     //使用CAS获取锁成功
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
    //使用CAS获取锁失败
                acquire(1);
        }
        

        public final void acquire(int arg) {    
   //第二步：如果尝试获取失败并且添加队列成功的,说明已经加入到了AQS的队列中。那么就会调用selfInterrupt函数中断线程执行
         if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))                     selfInterrupt();
         }


//第三步：AQS中的独占式获取锁
final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        //c==0,表示当前锁没有被任何线程获取，尝试使用CAS获取
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        //已经拥有该锁
        else if (current == getExclusiveOwnerThread()) {
            //更新状态（体现可重入）
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
    //添加Waiter结点，插入尾部
      private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }
    
 final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setH ead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }}
```

 **ReentrantLock VS Sychronized**

![image-20200713191526093](E:\CS\cs-note\img\ReentrantLock.jpg)

##### 4.1.3 ReentrantReadWriteLock

+ 读写锁的加锁

```java
//写锁的加锁
protected final boolean tryAcquire(int acquires) {
	Thread current = Thread.currentThread();
	int c = getState(); // 取到当前锁的个数
	int w = exclusiveCount(c); // 取写锁的个数w
	if (c != 0) { // 如果已经有线程持有了锁(c!=0)
    // (Note: if c != 0 and w == 0 then shared count != 0)
		if (w == 0 || current != getExclusiveOwnerThread()) // 如果写线程数（w）为0（换言之存在读锁） 或者持有锁的线程不是当前线程就返回失败
			return false;
		if (w + exclusiveCount(acquires) > MAX_COUNT)    // 如果写入锁的数量大于最大数（65535，2的16次方-1）就抛出一个Error。
      throw new Error("Maximum lock count exceeded");
		// Reentrant acquire
    setState(c + acquires);
    return true;
  }
  if (writerShouldBlock() || !compareAndSetState(c, c + acquires)) // 如果当且写线程数为0，并且当前线程需要阻塞那么就返回失败；或者如果通过CAS增加写线程数失败也返回失败。
		return false;
	setExclusiveOwnerThread(current); // 如果c=0，w=0或者c>0，w>0（重入），则设置当前线程或锁的拥有者
	return true;
}

```

如果存在读锁，则写锁不能被获取，原因在于：必须确保写锁的操作对读锁可见，如果允许读锁在已被获取的情况下对写锁的获取，那么正在运行的其他读线程就无法感知到当前写线程的操作。

```java
//读锁的加锁

```



##### 4.1.4 CountDownLatch(共享锁)

线程每次调用，就减一，使用CAS操作。

##### 4.1.5 CyclicBarrier

##### 4.1.6 Semaphore



### 4.2 AQS 

##### 4.2.1 AQS核心原理

>如果被请求的共享资源空闲，那么就将当前请求资源的线程设置为有效的工作线程，将共享资源设置为锁定状态；
>
>**如果共享资源被占用，就需要一定的阻塞等待唤醒机制来保证锁分配**。这个机制主要用的是**CLH队列的变体**实现的，将暂时获取不到锁的线程加入到队列中。
>CLH是单向链表，AQS中的队列是CLH变体的**虚拟双向队列（FIFO**），AQS是通过将每条请求共享资源的线程封装成一个**节点** `Node`来实现锁的分配。
```java
//记录同步状态，通过CAS 来更改state的状态
private volatile int state;
```
##### 4.2.2 数据结构

**Node结点**

| 属性和方法  | 含义                              |
| ----------- | --------------------------------- |
| waitStatus  | 在队列中的状态                    |
| thread      | 处于该节点的线程                  |
| prev        | 前驱指针                          |
| predecessor | 返回前驱节点，没有的话抛出npe     |
| predecessor | 指向下一个处于CONDITION状态的节点 |
| next        | 后继指针                          |

**线程的两种模式**

独占/共享   `shared` `exclusive`

##### 4.2.3 同步状态

##### 4.2.4 等待队列

`addWaiter` 就是一个在双端链表添加尾节点的操作，需要注意的是，双端链表的头结点是一个无参构造函数的头结点.

##### 4.2.5 获取锁/解锁过程

![image-20200713193850525](https://raw.githubusercontent.com/Leeco1997/images/master/img/lock.jpg)

一个线程获取锁失败了，被放入等待队列，`acquireQueued`会把放入队列中的线程不断去获取锁，直到获取成功或者不再需要获取（中断）。

##### 4.2.6 中断机制



### 4.3 Unsafe

> 直接操作堆外内存,类似于C语言的指针。   `public native long allocateMemory(long var1);`
>
> jdk1.9以后，不能通过反射使用。

### 4.4 Atomic

> 原子性操作:不可被中断的一个或者一系列操作。   
>
> 底层原理：【CAS】

:o: 并发场景下的自增操作实现方式？

+ synchronized
+ AtomicLong
+ LongAdder   --- 分段锁

##### 4.4.1 AtomicInteger

##### 4.4.2 原子累加器

+ LongAdder  性能更好

  ```java
   // 有竞争的时候使用cell，默认2-4-8-16
  transient volatile Cell[] cells;
  // 基础值, 如果没有竞争, 则用 cas 累加这个域 
  transient volatile long base;
  // 在 cells 创建或扩容时, 置为 1, 表示加锁
  transient volatile int cellsBusy;
  ```

  ```java
   @sun.misc.Contended //防止伪共享
      static final class Cell {
          volatile long value;
          Cell(long x) { value = x; }
          final boolean cas(long cmp, long val) {
              return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
          }
  		//……
      }
  ```

  ```java
  //add()
  
  ```

  

  

  

  > 

+  AtomicLong

##### 4.4.3原子数组 

+ AtomicIntegerArray