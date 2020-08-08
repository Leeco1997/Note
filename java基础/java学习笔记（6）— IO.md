## Java  IO

### BIO/NIO/AIO

+ 阻塞IO
+ 异步IO
+ 非阻塞IO

**BIO**

当用户进程调用了`recvfrom`这个系统调用，`kernel`就开始了IO的第一个阶段：准备数据（对于网络IO来说，很多时候数据在一开始还没有到达。比如，还没有收到一个完整的UDP包。这个时候kernel就要等待足够的数据到来）。

这个过程需要等待，也就是说数据被拷贝到操作系统内核的缓冲区中是需要一个过程的。而在用户进程这边，整个进程会被阻塞（当然，是进程自己选择的阻塞）。当`kernel`一直等到数据准备好了，它就会将数据从`kernel`中拷贝到用户内存，然后`kernel`返回结果，用户进程才解除`block`的状态，重新运行起来。



**NIO**

当用户进程发出`read`操作时，如果kernel中的数据还没有准备好，那么它并不会block用户进程，而是立刻返回一个`error`。从用户进程角度讲 ，它发起一个read操作后，并不需要等待，而是马上就得到了一个结果。

用户进程判断结果是一个error时，它就知道数据还没有准备好，于是它可以再次发送read操作。

一旦kernel中的数据准备好了，并且又再次收到了用户进程的`system call` 【用户进程需要不停的询问】，那么它马上就将数据拷贝到了用户内存，然后返回。

> 1. 基于事件驱动-> selector（支持对多个socketChannel的监听）
> 2. 统一的事件分派中心-> dispatch
> 3. 事件处理服务-> read & write

### 文件描述符

> 文件描述符在形式上是一个非负整数。实际上，它是一个索引值，指向内核为每一个进程所维护的该进程打开文件的记录表。当程序打开一个现有文件或者创建一个新文件时，内核向进程返回一个文件描述符。

### 多路复用模型

`Blocking IO`

+ 创建太多线程（系统调用，使用软中断 `clone`）
+ 消耗内存资源，内存栈
+ cpu上下文切换浪费时间
+ Blocking

`New/NonBlocking IO`

缺点： 每次调用O(N)的时间复杂度

改进：使用`select`多路复用，同步的。  

> **I/O 多路复用的特点**是通过一种机制一个进程能同时等待多个文件描述符，而这些文件描述符（套接字描述符）其中的任意一个进入读就绪状态，select()函数就可以返回。

#### select模型

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/select.jpg)

#### poll模型

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/poll.jpg)

#### epoll模型

充分发挥硬件功能，尽量不浪费cpu。只有数据到达的时候，才会产生中断，使用cpu资源读取数据。

网卡收到数据以后，会把数据存在DMA中，随后i网卡产生一个硬中断，cpu会回调，读取数据，找到文件描述符，找到socket。 --- 事件驱动

![epoll](https://raw.githubusercontent.com/Leeco1997/images/master/img/epoll.jpg)

```c++
//调用epoll_create
struct eventpoll{
    ....
    /*红黑树的根节点，需要监听的socket 文件描述符*/
    struct rb_root  rbr;
    /*双链表中则存放着将要通过epoll_wait返回给用户的 就绪列表*/
    struct list_head rdlist;
    ....
}
```



**select/poll/epoll小结**

+ epoll事先通过epoll_ctl()来注册一 个`文件描述符 epfd`，一旦基于某个文件描述符就绪时，内核会采用类似`callback`的回调机制，迅速激活这个文件描述符，当进程调用`epoll_wait() `时便得到通知

+ epoll不同于select和poll`轮询`的方式，而是通过每个fd定义的`回调`函数来实现的。只有就绪的fd才会执行回调函数。
+ epoll使用红黑树存储事件
+ select有最大数目限制
+ poll本质上和select没有区别， 但是它没有最大连接数的限制，原因是它是基于链表来存储的，每次调用时都会对连接进行线性遍历

> 如果没有大量的idle -connection或者dead-connection，epoll的效率并不会比select/poll高很多，但是当遇到大量的idle- connection，就会发现epoll的效率大大高于select/poll。

#### reactor模型

> - **Reactor**将I/O事件分派给对应的Handler
> - **Acceptor**处理客户端新连接，并分派请求到处理器链中
> - **Handlers**执行非阻塞读/写 任务



[IO多路复用参考文章](https://segmentfault.com/a/1190000003063859)

### redis的多线程

redis中使用epoll模型解决客户端连接的问题， 原子串行化。

redis 6.X中使用多线程，多个IO线程`read`、`write`，计算过程仍然是串行的



### 工作模式

+ LT（水平触发 default）

  内核通知你文件描述符已经准备好了，此时可以立即进行IO操作，也可以不执行；如果不执行IO，则下次仍然会继续通知你；

+ ET （边缘触发）

  必须立即执行IO,如果不立即执行，`不会重复发送就绪通知`。只能使用no-block socket，避免长时间的阻塞导致其他处理文件操作符的任务饿死。

### 零拷贝



### 常见面试问题

#### BIO的缺点？

服务端使用`serverSocket`绑定端口，监听当前端口，然后等待`accept`事件,accept会阻塞当前线程。每次来一个连接，就建立一次长连接，指定一个线程，很难做到C10K.

> + 很难解决并发
>
> + 使用InputStream/OutputStream/byte [],NIO封装了`ByteBuffer`
>
>   > ByteBuffer.flip()这个方法，这个方法的作用主要是重置索引，在write()之前和read()之后调用，否则会因为索引不对，导致你的数据写不进去，读不出来。

+ NIO 如何解决C10K？

  1. java的API提供了`selector`,把`socket`注册到selecrtor中，当某个socket准备就绪，就唤醒主线程进行相应的处理。

  2. jvm虚拟机使用的系统调用 `systemCall kernal`

  3. os 的`select`函数

     ```json
     1. 涉及内核态/用户态的切换
     2. 遍历fd集合是否有准备好的socket套接字 [linux上一切皆文件]
     3. 如果有就绪的，则直接返回；若没有，则阻塞。
     
     //问：默认最大可以监听1024个socket?为什么？
     fd_set是一个位图结构，就是01二进制，bitmap的默认长度是1024.
     select函数: 1. 在fd文件中打一个标记 mask
                 2. 唤醒java线程，是一个整数，有几个准备就绪的socket;
     			3. O(N)的遍历，检查fd_set的状态
     //问：select是一直占用CPU,不停的轮询吗?
     1.第一遍轮询以后，保留给需要检查socket的等待队列；
     2.socket包括读缓存、写缓存、等待队列；
     3.当数据经过网线、网卡、DMA写入内存，完成传输的时候会触发一个中断，然后使得cpu暂停目前工作，执行中断的逻辑，开始分析socket数据包，分别放入读写缓存。然后把等待队列中的等待队列放入工作队列，获取CPU时间片以后开始执行。轮询检查打了标记的socket.
     ```

  4. `poll`函数

     1. 与select的入参不一样，数组结构，`kernal`里面使用链表存储。解决了1024限制的问题。

  5. `select` 和`poll`的缺点

     1. 返回给java的只是一个数，需要新的一轮调用得到哪个socket就绪，O(N)的遍历
     2. 用户态和内核态数据的拷贝。

  6. `epoll`的优势

      解决`数据拷贝`和返回准备就绪`socket`的位置。

     1. 调用`epoll_create`,创建`eventpoll`对象，包括`socket`的文件描述符列表和就绪列表；
     2. 使用`epoll_ctl`维护`socket`的文件描述符;
     3. `epoll_wait`如何返回准备就绪的socket？

> 当网卡数据通过中断或进程写入socket文件缓冲区后，驱动会把已经处于等待队列的进程唤醒，这部分由内核负责调度，具体是将进程的状态设置成RUNNING并交由调度器恢复进程上下文。从中断退出到进程调度这段时间，就可以发生多个socket可读。多个可读的socket号会通过epoll的参数返回出来