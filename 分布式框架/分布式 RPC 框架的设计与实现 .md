### 阅读论文笔记

#### - 分布式 RPC 框架的设计与实现 

1. 目录

   * 【背景以及技术介绍 】--- Spring, I/O技术、分布式服务、ZooKeeper

   * 【需求分析与设计】

     ![RPC-框架.png](https://i.loli.net/2020/05/08/R1HyjWMBT2G6StZ.png)

     

   ![image-20191110150801278.png](https://i.loli.net/2020/05/08/acjCr5OSv64V1KH.png)

   - **RPC框架的实现与测试**

     工具：Jdk1.8+IDEA+Zookeeper+Netty

   - 总结与致谢

2. 核心部分

   - **数据处理模块 **
   - 数据传输功能实现  Netty 
   - 服务发布模块实现  Spring
   - 服务调用模块 Zookeeper

   ```java
   //将请求数据封装到 RpcRequest 数据对象中 
   RpcRequest reqDataObject = new RpcRequest(); 
   //将对象中的字段依次封装 
   reqDataObject.setRequestId(randomRequestId); 
   „„ 
   //将封装好的数据对象发送给 Rpc-server 
   RpcClient client = new RpcClient(host, port); 
   RpcResponse resDataObject = client.send(reqDataObject); 
   -------------------------------
   问：这个send方法是如何实现的？使用Netty。
   ```

   ```java
   //当Rpc-server 接收到客户端请求时，完成服务调用，并封装好 RpcReponse 发送会客户端 
   Protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) { 
   RpcResponse resDataObject = new RpcResponse(); 
   resDataObject.setRequestId(msg.getRequestId()); 
   try { 
   resDataObject.setResult(handler(msg)); //handler为服务调用方法，并返回结果 
   } catch (Exception e) { 
   resDataObject ect.setError(e); 
   } 
   //写入 RpcEncoder）进行下一步处理后发送到 channel 中给客户端 
   ctx.writeAndFlush(resOb resDataObject ject).addListener(ChannelFutureListener.CLOSE); 
   } 
   -------------------------------
   问：最后一步没看懂。
   ```

   

#### - 基于 Netty 的高性能 RPC 服务器的研究与实现 

1. 目录

   - 背景与现状分析
   - 理论技术研究  ---  *参数传递机制，负载均衡算法，网络I/O模型*
   - 需求分析与架构设计 --- *Netty*
   - 基于 Netty 的 RPC 服务器的实现
   - 性能测试

   

2. **核心部分**

   2.1 Netty技术框架

   - Netty ByteBuff  & Channel

![RPC.png](https://i.loli.net/2020/05/08/MckYK3P8Uel9T1i.png)



2.2 编解码模块

2.3 通信调度模块

2.4 服务发布与订阅

2.5 业务处理模块

2.6 服务端/客户端调用

2.7 长连接服务

![Netty.png](https://i.loli.net/2020/05/08/FdUQZ2fxWB31GNg.png)



