## Netty

> 基于Java NIO 的异步调用和事件驱动的实现。

### 核心组件

#### Channel

> 它代表一个到实体的（一个硬件设备、一个文件）的开放连接，如读写操作。可以看作是传入和传出数据的载体。

#### 回调

回调其实就是一个方法，当回调被触发的时候，相关的事件可以被`ChannelHandler`处理。

```java
public class ConnectHandler extends ChannelInboundHandlerAdapter {
    @Override
    //当一个新的连接已经被建立时，channelActive(ChannelHandlerContext)将会被调用
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        System.out.println(
                "Client " + ctx.channel().remoteAddress() + " connected");
    }
}
```

#### Future

```java
 future.addListener(new GenericFutureListener<Future<? super Void>>() {
     //连接成功的时候，会触发这个回调方法
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if (future.isSuccess()){
                    //……
                }else{
                    future.cause().printStackTrace();
                }
            }
        });
```



#### 事件和ChannelHandler

> Netty使用不同的事件来通知我们状态的改变或者是操作的状态，基于事件触发相应的动作。可能的事件 连接已被激活或者失效、数据读取、写数据等。

### 常用功能

#### 心跳机制  

`idleStateHandler`

#### 拆包粘包

**Netty的拆包器：**

1. 固定长度的拆包器 FixedLengthFrameDecoder

   每个应用层数据包的都拆分成都是固定长度的大小，比如 1024字节。

   这个显然不大适应在 Java 聊天程序 进行实际应用。

2. 行拆包器 LineBasedFrameDecoder

   每个应用层数据包，都以换行符作为分隔符，进行分割拆分。

   这个显然不大适应在 Java 聊天程序 进行实际应用。

3. 分隔符拆包器 DelimiterBasedFrameDecoder

   每个应用层数据包，都通过自定义的分隔符，进行分割拆分。

   这个版本，是LineBasedFrameDecoder 的通用版本，本质上是一样的。

   这个显然不大适应在 Java 聊天程序 进行实际应用。

4. 基于数据包长度的拆包器 LengthFieldBasedFrameDecoder

   将应用层数据包的长度，作为接收端应用层数据包的拆分依据。按照应用层数据包的大小，拆包。这个拆包器，有一个要求，就是应用层协议中包含数据包的长度

#### 零拷贝

- 传统IO，可以把磁盘的文件经过内核空间，读到JVM空间，然后进行**各种操作**，最后再写到磁盘或是发送到网络，效率较慢但支持数据文件操作。

- 零拷贝则是直接在内核空间完成文件读取并转到磁盘（或发送到网络）。由于它没有读取文件数据到JVM这一环，因此程序无法操作该文件数据，尽管效率很高！

#### NIO的直接内存

>  直接内存（mmap技术）将文件直接映射到内核空间的内存，返回一个**操作地址**（address），它解决了文件数据需要拷贝到JVM才能进行操作的窘境。而是直接在内核空间直接进行操作，**省去了内核空间拷贝到用户空间**这一步操作。