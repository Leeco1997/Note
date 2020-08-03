## Java  IO

### BIO/NIO

**BIO**

当用户进程调用了`recvfrom`这个系统调用，`kernel`就开始了IO的第一个阶段：准备数据（对于网络IO来说，很多时候数据在一开始还没有到达。比如，还没有收到一个完整的UDP包。这个时候kernel就要等待足够的数据到来）。

这个过程需要等待，也就是说数据被拷贝到操作系统内核的缓冲区中是需要一个过程的。而在用户进程这边，整个进程会被阻塞（当然，是进程自己选择的阻塞）。当`kernel`一直等到数据准备好了，它就会将数据从`kernel`中拷贝到用户内存，然后`kernel`返回结果，用户进程才解除`block`的状态，重新运行起来。

**NIO**

当用户进程发出`read`操作时，如果kernel中的数据还没有准备好，那么它并不会block用户进程，而是立刻返回一个`error`。从用户进程角度讲 ，它发起一个read操作后，并不需要等待，而是马上就得到了一个结果。

用户进程判断结果是一个error时，它就知道数据还没有准备好，于是它可以再次发送read操作。

一旦kernel中的数据准备好了，并且又再次收到了用户进程的`system call` 【用户进程需要不停的询问】，那么它马上就将数据拷贝到了用户内存，然后返回。



### 文件描述符

> 文件描述符在形式上是一个非负整数。实际上，它是一个索引值，指向内核为每一个进程所维护的该进程打开文件的记录表。当程序打开一个现有文件或者创建一个新文件时，内核向进程返回一个文件描述符。

### 从BIO到多路复用的发展 

`Blocking IO`

+ 创建太多线程（系统调用，使用软中断 `clone`）
+ 消耗内存资源，内存栈
+ cpu上下文切换浪费时间
+ Blocking

`New/NonBlocking IO`

缺点： 每次调用O(N)的时间复杂度

改进：使用`select`多路复用，同步的。  

> **I/O 多路复用的特点**是通过一种机制一个进程能同时等待多个文件描述符，而这些文件描述符（套接字描述符）其中的任意一个进入读就绪状态，select()函数就可以返回。

`select模型`

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/select.jpg)



`poll模型`

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/poll.jpg)

`epoll模型` 

充分发挥硬件功能，尽量不浪费cpu。只有数据到达的时候，才会产生中断，使用cpu资源读取数据。

网卡收到数据以后，会把数据存在DMA中，随后i网卡产生一个硬中断，cpu会回调，读取数据，找到文件描述符，找到socket。 --- 事件驱动

![epoll](https://raw.githubusercontent.com/Leeco1997/images/master/img/epoll.jpg)

```c++
//调用epoll_create
struct eventpoll{
    ....
    /*红黑树的根节点，这颗树中存储着所有添加到epoll中的需要监控的事件*/
    struct rb_root  rbr;
    /*双链表中则存放着将要通过epoll_wait返回给用户的满足条件的事件*/
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

