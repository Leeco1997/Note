## JVM学习笔记

### :one: JAVA运行内存

### :two: 垃圾收集器

#### 2.1 强软弱虚四种引用

强引用：只要强引用还在，则不会被回收。

软引用：内存不足的时候，回被回收。用于缓存

弱应用：只能存活到下一次gc之前。`ThreadLocal`

虚引用：管理堆外内存。`NIO`  zero copy   `零拷贝`。防止内存泄漏

![](https://raw.githubusercontent.com/Leeco1997/images/master/img/虚引用.jpg)



### :three: 内存分配策略

### :four: 类加载机制

