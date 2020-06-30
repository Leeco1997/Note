[TOC]

#### (一) JAVA基础

##### 1.封装/继承/多态

>**封装:**在面向对象设计中,将一切事物都描述成一个独立\封闭的对象,只向外部提供最简单的接口,不关心这个对象具有的内部属性和功能.

>**继承:**从已有的类中得到一些继承信息,并且在此基础上扩展.  多个类具有共同的属性 ,可以减轻代码的冗余度.

>**多态:**允许不同的子类对于同一消息做出不同的响应.如**方法重写和方法重载.**

##### 2.static关键字

   ` 特点:在没有创建对象的情况下来进行调用,直接通过类名去访问。` 

   `静态方法只能访问静态成员。（非静态既可以访问静态，又可以访问非静态）` 

   `不能使用this去调用.随着类的加载自动完成初始化,内存中只有一个,JVM只分配一次内存，所有的类共享； `

+ **静态变量**: 减少对象的创建,常用用全局变量,count,Logger.

  +  作为共享变量使用

  +  减少对象的创建

  + 保留唯一副本

  ```java
  public class Singleton {
      private static volatile Singleton singleton;//单例模式
   
      private Singleton() {}
   
      public static Singleton getInstance() {
          if (singleton == null) {
              synchronized (Singleton.class) {
                  if (singleton == null) {
                      singleton = new Singleton();
                  }
              }
          }
          return singleton;
      }
  }
  ```

  

+ 静态方法: **为了方便在不创建对象的情况下调用** 如Build.

+ 静态代码块: 通常来说是为了对静态变量进行一些初始化操作，比如单例模式、定义枚举类 

+ 静态内部类: builder设计模式 

  ```java
  public class Person {
      private String name;
      private int age;
   
      private Person(Builder builder) {
          this.name = builder.name;
          this.age = builder.age;
      }
   
      public static class Builder {
   
          private String name;
          private int age;
   
          public Builder() {
          }
   
          public Builder name(String name) {
              this.name = name;
              return this;
          }
          public Builder age(int age) {
              this.age=age;
              return this;
          }
   
          public Person build() {
              return new Person(this);
          }
      }
   
      public String getName() {
          return name;
      }
   
      public void setName(String name) {
          this.name = name;
      }
   
      public int getAge() {
          return age;
      }
   
      public void setAge(int age) {
          this.age = age;
      }
  }
   
  // 在需要创建Person对象的时候
  Person person = new Person.Builder().name("张三").age(17).build();
  ```

  

  >**加载顺序:**
  >
  >父类静态变量
  >
  >父类静态代码块
  >
  >子类静态变量
  >
  >子类静态代码块
  >
  >父类普通变量
  >
  >父类普通代码块
  >
  >父类构造函数
  >
  >子类普通变量
  >
  >子类普通代码块
  >
  >子类构造函数

  ![](https://pics3.baidu.com/feed/024f78f0f736afc33409f1471839eec1b74512b4.jpeg?token=33db0ecbe211c671b69179720b8e41d4&s=ED9CAA528ACE3EC846392E6303003066)

1. 存放在方法区, static修饰的数据是共享数据，对象中的存储的是特有的数据。 ;
2. 随着类的加载就已经加载了;
3.  static修饰的成员变量\成员函数被所有的对象共享。 

[1]成员变量与静态变量的区别?

1. 生命周期不同;
2. 使用方式不同;
3. 存储位置不同;

##### 3.final&&finally

+ final修饰的变量是一个常量，不能被修改；

+ final修饰方法不能被重写，但是可以重载；

+ final修饰的类不能被继承。

  ---

+ finally常常放在try,catch的后面，无论前面是否发生异常，都会执行finally语句，除非在前面调用了关闭虚拟机的方法。

+  finally块一般是用来释放物理资源（数据库连接，网络连接，磁盘文件等）。 

##### 4.equals&&==&&hashcode的区别

1. 默认的equals方法，里面其实也是直接==判断，直接判断对象的引用地址，如果是native类型，则直接判断数值是否相等。

   **对象是放在堆中的，栈中存放的是对象的引用（地址）**。由此可见'=='是对栈中的值进行比较的。如果要比较堆中对象的内容是否相同，那么就要重写equals方法了。  

2. 重写equals和hashcode.

+ 如果a=b，则h(a) = h(b)。

+ 如果a!=b，则h(a)与h(b)可能得到相同的散列值。 

  **hashcode的作用**【先比较hashcode->再比较equals】

  > 在一个set集合中，需要插入一个新的元素，如果使用equals方法的话，则需要比较每一个元素，开销较大。所以使用hashcode,先比较hashcode是否相同；如果hashcode相同，继续使用equals比较。

##### 5.面向对象和面向过程

解耦：

例：把大象放进冰箱；蛋炒饭和盖浇饭。

##### 6.序列化与反序列化

作用：1） **把对象的字节序列永久存储在硬盘上**；

​			2）**在网络上传输字节序列**。

>两个进程在传输的过程中，各种类型的数据都会转转化为二进制序列。
>
>发送方需要把java对象转化为序列化对象，而接受方则会反序列化。

远程代理：远程对象的本地代表。代理其实就是一个中间件，和真正的服务器沟通。

##### 7. 异常的类型

 <img src="https://img-blog.csdn.net/20180920165502957?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L21pY2hhZWxnbw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70" alt="img" style="zoom:80%;" /> 

##### 8.String/StringBuffer/StringBulider

底层： (Char类型的数组)进行存储 

| String                                                       | StringBuffer                      | StringBuilder      |
| ------------------------------------------------------------ | --------------------------------- | ------------------ |
| final修饰，不可变性                                          | 可变，线程安全。**synchronize**。 | 可变，线程不安全。 |
| 当两个String对象拥有相同的值时，他们只引用常量池中的同一个拷贝 | 拼接http请求。                    | SQL语句的拼装      |

##### 9. IO流

 ![img](https://img-blog.csdn.net/20180127210359151) 

###### 9.1 IO流特性

1. 先入先出；
2. 顺序存取；
3. 只读或者只写，不能同时具备两个功能。

###### 9.2 IO流常用接口

 File、OutputStream、InputStream、Writer、Reader  &&   Serializable 

* 考虑传输得是1字节还是2个字节，有中文则用字符流。
* 选择输入还是输出流。

+ **ObjectInputStream**【反序列化对象】 &&  **ObjectOutputStream**  【序列化对象】

###### 9.3 字节流与字符流

+ 字符流不能用于处理图像视频等非文本类型的文件 

+ 字节流没有缓冲区，直接操作文本本身，字符流有缓冲区，所以需要调用close方法，才能输出信息。不使用close关闭流，需要使用flush().

  ```java
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("e:" +   File.separator + "demoA.txt")));
          oos.writeObject(new Person("java开发", 20));
          oos.close();
          //反序列化
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("e:" +     File.separator + "demoA.txt")));
          Object person = ois.readObject();
          Person p = (Person) person;
          System.out.println(p);
          ois.close();
  ```

  

##### 10.抽象类和接口的区别

| 抽象类                                                       | 接口                                                      |
| ------------------------------------------------------------ | --------------------------------------------------------- |
| 没有包含足够多的信息。可以实现方法。                         | 完全是抽象的，只定义，不实现。                            |
| 除了你不能实例化抽象类之外，它和普通Java类没有任何区别       | 契约模式，继承某个接口，必须实现它的方法。                |
|                                                              | 不能被是实例化。                                          |
| 抽象类是用来捕捉子类的通用特性的。【用于隐藏、拓展行为功能】 | Java单继承的原因所以需要曲线救国 作为继承关系的一个补充。 |
|                                                              | 有利于代码规范，接口分离原则。                            |
|                                                              | public static final 修饰变量。                            |
|                                                              | public abstract修饰方法。                                 |

##### 11 IO/NIO/BIO

* 同步/异步

* 阻塞/非阻塞

  
