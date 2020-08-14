java-锁

> 如何实现线程同步？

`synchronized`   `Lock`   `wait/notify`

jdk1.6以后优化了，加入了锁升级。无锁---偏向锁---轻量级锁---重量级锁

### Synchronized

> 调用mutex

### ReentrantLock

#### 实现原理

#### 队列

排队的时候需要调用  `park`/`unpark`

排队的时候是排在第二个，第一个node是 `thread = null`



#### readHolds

#### WaitStatus



#### 公平锁与非公平锁

+ 公平锁(加锁)

```java
//获取锁  
protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            //判断state的状态 
            int c = getState();
            if (c == 0) {
                //不需要排队 + cas成功 + 设置线程持有锁成功
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            //可重入锁
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                //防止溢出
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
    		//既没有获取到锁，也不是重入
            return false;
        }

```

**===更新队头/队尾===**

持有锁的当前线程不在队列中。

```java
public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
} 
//排队  
 private Node addWaiter(Node mode) {
     //Node里面存放的 thread + next+ prev
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
       //队列不为空
        if (pred != null) {
            node.prev = pred;
            //cas更新尾结点
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        //入队列
        enq(node);   //compareAndSetHead(new Node())，队头为null
        return node;
}

  private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                //队列初始化
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

//入队列以后，park当前线程
final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            //自旋
            for (;;) {
                //找到node的前驱节点
                final Node p = node.predecessor();
                //排队在第二个位置 && 尝试获取锁
                //这里就和真实排队场景一样，需要询问一下前面的人是否已经结束了
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
            //成功park()
            if (failed)
                cancelAcquire(node);
        }
    }
//判断自己是否需要park()，自旋两次
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
      //volatile int waitStatus;默认初始化为0
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            //每一次都会把前驱结点 ws=-1
            //因为当前线程已经park();自己不知道自己是否已经睡眠；
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
	//park()
    private final boolean parkAndCheckInterrupt() {
        //为什么不在这里修改 waitStatus?
        LockSupport.park(this);
        return Thread.interrupted();
    }
```







#### 手写一个ReentrantLock

```java
//
```



### AQS

`while(自旋)`+ `CAS`+ `Queue [park/unpark]`

+ 判断是否需要park()
+ park()线程

  

### 面试问题

#### 谈一谈ReentrantLock？

多个线程交替执行的时候，其实不需要队列，jdk级别解决同步问题。只有队列的`park`/`unpark`才会调用os.





