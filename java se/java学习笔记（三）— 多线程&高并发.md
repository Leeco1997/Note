## JAVA 并发编程
[TOC]
### 1. 基础知识
#### 1.1 上下文切换
> Cpu通过时间片分配算法来循环执行任务，让我们感觉多个线程是同时执行的，时间片一般是几十毫秒（ms）.

一般在设计的时间，尽量减少上下文切换：
+ 无锁并发编程，避免使用锁
+ CAS算法，使用Atomic包
+ 使用最少线程，减少waiting线程的数量
+ 协程：使用单线程完成多任务的调度

#### 1.2  死锁
##### 1.2.1 写一个死锁代码
##### 1.2.2 如何解决死锁？
+ 避免一个线程获得多个锁资源
+ 使用定时锁， tryLock(long timeout, TimeUnit unit)
+ 对于数据库的加锁和解锁必须放在一个数据库连接里，否则会出现解锁失败的情况

#### 1.3 进程、线程
> **进程是资源分配的最小单位，线程是程序执行的最小单位。**
>
> > 对于计算机来说，每一个任务就是一个进程，在每一个进程中至少包含一个线程。每个线程拥有自己的**局部变量表、程序计数器**（指向正在执行的指令的指针）以及各自的**生命周期**。

#### 1.4 CPU
##### 1.4.1 三级缓存结构
![e6a6f88dd6024d990b016f6df272f3c4.png](en-resource://database/690:1)

##### 1.4.2 伪共享
+ **引发原因：** 多个cpu同时修改缓存中的不同部分，从而导致其中一个的 cpu操作失效。可能会引发内存顺序冲突，cpu则必须清空流水线。

##### 1.4.3 MESI缓存一致性协议
+ 独占（exclusive）：仅当前处理器拥有该缓存行，并且没有修改过，是最新的值，如果有其他CPU来读取该缓存行的值，则变成S.。
+ 共享（share）：有多个处理器拥有该缓存行，每个处理器都没有修改过缓存，是最新的值。
+ 修改（modified）：仅当前处理器拥有该缓存行，并且缓存行被修改过了，一定时间内会写回主存，回写成功状态会变为E。
+ 失效（invalid）：缓存行被其他处理器修改过，该值不是最新的值，需要读取主存上最新的值。

**M和E的区别是:** M状态的数据是和内存不一致的，E状态的数据是和内存一致的。
![ae965afb9169073478096399712ce1aa.png](en-resource://database/692:1)




##### 1.4.5 内存屏障
##### 1.4.6 指令重排


### 2. java并发底层实现原理
#### 2.1 CAS （思想：乐观锁）
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
**解决方案：**　
１.　版本号／时间戳
２.　java中使用AtomicStampedReference.compareAndSet(),先检查当前引用是否等于预期引用，再检查当前标志是否等于预期标志。`expectedReference == current.reference && expectedStamp == current.stamp `

+ **循环时间长导致CPU消耗大**
**解决方案：**
使用pause指令：延迟流水线指令，减少cpu消耗；避免在退出的时候引起CPU流水线清空（CPU Pipeline Flush）

+ **只能保证一个共享变量的原子性。**
**解决方案：** 将多个共享变量合并为一个

#### 2.2 volatile 【可见性、防止指令重排】
##### 2.2.1 实现原理
**使用Lock前缀指令**

1. Lock指令一般不锁总线，而是锁住缓存，并且写回到内存，然后使用MESI来保证一致性；
2. 一个处理器的缓存写回到内存会导致其他处理器的缓存无效。
**保证数据的可见性，防止指令重排，但是不能保证数据的原子性。**

##### 2.2.3 那么如何保证原子性呢？
+ **Java中实现原子性操作 --> 调用Unsafe中的compareAndSwap方法--> JNI中的Atomic ::cmpxchg --> 使用汇编中的Lock指令**
+ **处理器实现原子性操作-->锁总线 or 锁缓存 + [MESI]**

#### 2.3 synchronized
jdk1.6对于synchronized做了很大的优化。
##### 2.3.1 java 头对象
java的对象头由以下三部分组成：

---
> **Mark Word** [hashcode、锁信息、分代年龄]

> **Class MetaData Address** [存储到对象类型数据的指针]

> **Array Length** [如果当前对象是数组的话]

---
**Mark Word的存储结构**

![97c595f561689f012e3f7db24b871b63.png](en-resource://database/686:1)



##### 2.3.2 锁的升级
**无锁状态 --> 偏向锁 --> 轻量级锁 --> 重量级锁**
**注意：** 锁升级后，不能降级。
**1. 偏向锁**
**使用范围**：绝大部分情况下不存在竞争、只有一个线程在尝试获取锁的场景，偏向于第一个访问锁。通过减少CAS操作的数量，提高应用性能。
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


**2. 轻量级锁**
+ **加锁的过程**
1. 在进入同步代码块以前，如果当前状态是无锁状态`01`,JVM会在当前线程的栈帧中创建一个 `Lock Record` 的空间，用于存储锁对象目前的Mark Word的拷贝`Displaced Mark Word`。
2. 使用CAS将对象头的 `Mark word`  替换为指向`Lock Record` 的指针,并将`Lock Record` 里的`owner`指针指向` mark word`。
3. 如果更新成功了，则表示该线程拥有该对象的锁，并且将 `Mark Word`的锁标记为置为`00`。如果更新失败了，则JVM会先判断当前线程是否已经拥有该锁。若未拥有，则说明有竞争，当前线程开始自选获取锁，如果自旋失败在，则升级为重量级锁`10`。

![98aafe6b6315791d54f8e8e5272e73d3.png](en-resource://database/688:1)

+ **释放锁的过程**
1. 使用CAS操作将Displaced Mark Word替换回对象头；
2. 若替换成功，说明没有竞争，成功释放；
3. 若替换失败，说明此时存在竞争，则升级为重量级锁。


**3. 重量级锁**
>如果轻量级锁自旋到达阈值后，没有获取到锁，就会升级为重量级锁.


##### 2.3.3 锁的优化
1. 减小锁的粒度 
>LinkedBlockingQueue
>在队列头入队，在队列尾出队，入队和出队使用不同的锁，相对于LinkedBlockingArray只有一个锁效率要高；

2. 使用读写锁 
>ReentrantReadWriteLock,
>读操作加读锁，可以并发读，写操作使用写锁，只能单线程写；

3. 锁的粗化与锁消除
>**锁粗化**是将多次拼接在一起的加锁解锁合并为一次加锁、解锁。例如：
``` java
 for(int i=0;i<size;i++){
 synchronized(lock){
  //do something
 }
```

>**锁消除**是发生在编译时期，如果一段代码中，堆中的对象不会被其他线程访问，则可以不使用同步加锁。**典型例子：**
>`String string = str1 + str2 ;`

4. 如果线程竞争不激烈，可以使用Volatile + CAS

5. 避免伪共享

>java7使用`追加字节`的方式（一段long类型的变量)。
>Java8使用`@sun.misc.Contended`避免伪共享。使用此注解的对象或字段的前后各增加128字节大小的padding，使用2倍于大多数硬件缓存行的大小来避免相邻扇区预取导致的伪共享冲突。
```java
public class FalseSharing implements Runnable {

    public final static int NUM_THREADS = 4; // change
    public final static long ITERATIONS = 500L * 1000L * 1000L;
    private final int arrayIndex;

    private static VolatileLong[] longs = new VolatileLong[NUM_THREADS];//    private static VolatileLong2[] longs = new VolatileLong2[NUM_THREADS];//    private static VolatileLong3[] longs = new VolatileLong3[NUM_THREADS];

    static {
        for (int i = 0; i < longs.length; i++) {
            longs[i] = new VolatileLong();
        }
    }

    public FalseSharing(final int arrayIndex) {
        this.arrayIndex = arrayIndex;
    }

    public static void main(final String[] args) throws Exception {
        long start = System.nanoTime();
        runTest();
        System.out.println("duration = " + (System.nanoTime() - start));
    }

    private static void runTest() throws InterruptedException {
        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new FalseSharing(i));
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    public void run() {
        long i = ITERATIONS + 1;
        while (0 != --i) {
            longs[arrayIndex].value = i;
        }
    }

    public final static class VolatileLong {
        public volatile long value = 0L;
    }

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

##### 2.4 Atomic并发包
1. 原子性操作
> 不可被中断的一个或者一系列操作。


### 4. 并发容器和框架
#### 4.1 Lock
> JDk1.5 以后增加了Lock接口，提供了与synchronized类似的功能。
##### 4.1.1 Lock VS Synchronized
**Lock的优势：**
+ **底层：** volatile + CAS (乐观锁)
+  非阻塞的获取锁，tryLock(),能够及时的返回true or false,不会阻塞；
+ 能够响应中断lockInterruptibliy()，当获取锁的线程被中断的时候，会抛出中断异常，并且释放锁；
+ 超时的获取锁，如果在指定的时间里没有获取锁，则返回false；
+ 可以使用Condition；
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
+ 属于jvm的关键字，可能性能调优上方便一些？
##### 4.1.2 ReentrantLock          
> 可重入锁，一个线程可对临界资源重复加锁。**底层：AQS  公平锁&&非公平锁**

**实现方式**
```java

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
                setHead(node);
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

#### 4.2 AQS 
##### 4.2.1 AQS核心原理

>如果被请求的共享资源空闲，那么就将当前请求资源的线程设置为有效的工作线程，将共享资源设置为锁定状态；
>
>如果共享资源被占用，就需要一定的阻塞等待唤醒机制来保证锁分配。这个机制主要用的是**CLH队列的变体**实现的，将暂时获取不到锁的线程加入到队列中。
>CLH是单向链表，AQS中的队列是CLH变体的虚拟双向队列（FIFO），AQS是通过将每条请求共享资源的线程封装成一个**节点** `Node`来实现锁的分配。
```java
//记录同步状态
private volatile int state;
```
##### 4.2.2 独占 VS 共享

>自定义同步器要么是独占方式，要么是共享方式，它们也只需实现**tryAcquire-tryRelease**、**tryAcquireShared-tryReleaseShared**中的一种即可。