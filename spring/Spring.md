## Spring源码学习

![](https://cdn.jsdelivr.net/gh/Leeco1997/images@master/img/spring.jpg)

> Core核心容器、事件、资源、i18n,验证、数据绑定、类型转换、aop.

### 一. Spring AOP

#### 1.1 什么是AOP？

#### 1.2 AOP的源码？

#### 1.3 如何手写一个AOP？

#### 1.4 AOP的具体应用有哪些？在框架中是如何体现的？





### 二. Spring IOC

#### 2.1 什么是IOC？

> 在spring中，所有的java资源都是java Bean，IOC的作用就是管理这些Bean以及他们之间的依赖关系。比如Bean的创建、行为等。不需要手动`new` 对象，只需要告诉spring IOC所需要的资源，IOC自己则会找到所需要的资源。
>
> IOC还提供了对bean生命周期的管理，延迟加载。  **IoC 容器实际上就是个 Map（key，value）,Map 中存放的是各种对象。**

![](https://cdn.jsdelivr.net/gh/Leeco1997/images@master/img/applicationContext.jpg)

`ApplicationContext` 的里面有两个具体的实现子类，用来读取配置配件的：

- `ClassPathXmlApplicationContext` - 从 class path 中加载配置文件，更常用一些；
- `FileSystemXmlApplicationContext` - 从本地文件中加载配置文件，不是很常用，如果再到 Linux 环境中，还要改路径，不是很方便。

#### 2.2 DI

DI是实现IOC的方式。



### 四. Bean的生命周期

1. 初始化`ApplicationContext`
2. 扫描、解析 ---->  `BeanDefination`对象---> `BeanDefinitionHolder`封装别名
3. 校验，存放到`Map<String name,BeanDefination bean>`
4. 调用BeanFactory---> 创建Bean【判断是否单例对象、是否懒加载等】
   + 验证  是否是单例，是否有依赖等等
   + 创建之前会加入正在创建的 `set<String>`
   + new Object() ---> BeanWarpper
   + 将属性放入map，然后注入属性
   + 回调 后置处理器
   + ​	
5. 放入单例池（一级缓存）
6. 销毁

![](https://cdn.jsdelivr.net/gh/Leeco1997/images@master/img/newBean.jpg)

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {
    //自动装配一个构造器
}

		// Candidate constructors for autowiring?
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			return autowireConstructor(beanName, mbd, ctors, args);
		}

```

`getSingleton()`

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//从单例池当（一级缓存）中直接拿，也就是文章里面'目前'的解释
		//这也是为什么getBean("xx")能获取一个初始化好bean的根本代码
		Object singletonObject = this.singletonObjects.get(beanName);
		//如果这个时候是x注入y，创建y，y注入x，获取x的时候那么x不在容器
		//第一个singletonObject == null成立
		//第二个条件判断是否存在正在创建bean的集合当中，前面我们分析过，成立
		//进入if分支
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				//先从三级缓存那x？为什么先从三级缓存拿？下文解释
				singletonObject = this.earlySingletonObjects.get(beanName);
				//讲道理是拿不到的，因为这三个map现在只有二级缓存中存了一个工厂对象
				//回顾一下文章上面的流程讲工厂对象那里，把他存到了二级缓存
				//所以三级缓存拿到的singletonObject==null  第一个条件成立
				//第二个条件allowEarlyReference=true，这个前文有解释
				//就是spring循环依赖的开关，默认为true 进入if分支
				if (singletonObject == null && allowEarlyReference) {
					//从二级缓存中获取一个 singletonFactory，回顾前文，能获取到
					//由于这里的beanName=x，故而获取出来的工厂对象，能产生一个x半成品bean
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					//由于获取到了，进入if分支
					if (singletonFactory != null) {
						//调用工厂对象的getObject()方法，产生一个x的半成品bean
						//怎么产生的？下文解释，比较复杂
						singletonObject = singletonFactory.getObject();
						//拿到了半成品的xbean之后，把他放到三级缓存；为什么？下文解释
						this.earlySingletonObjects.put(beanName, singletonObject);
						//然后从二级缓存清除掉x的工厂对象；？为什么，下文解释
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}

```

+ 先从单例池中获取，然后从三级缓存中拿，再从二级缓存中获取。



#### 4.1 循环依赖

`spring 的单例对象默认支持循环依赖，可以手动关闭`

:arrow_left: 什么是循环依赖？如何实现的？

```java
@Component
public class A {
    @Autowired
    B b;
}

@Component
public class B {
    @Autowired
    A a;
}

```

实例化 A --> 属性注入B --->B为Null,实例化B --> 属性注入A--->A为null,但是A 正在create-->从缓存中读取。

:arrow_down_small:为什么使用**三级缓存**？不使用二级缓存？ `Map<String，Object>`

+ SingletonObjects  单例池
+ SigletonFactories   存放的是工厂对象
+ earlySingletonObjects  临时对象【半成品Bean】
+ ![img](https://img-blog.csdnimg.cn/20200320145943343.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2phdmFfbHl2ZWU=,size_16,color_FFFFFF,t_70)

> 单例对象只需要创建一次，所有需要单例池来缓存；
>
> 缓存的是一个XXXFactory，为了解决循环依赖，比如代理；【策略 + 工厂设计模式 》 Bean】
>
> > 开闭原则，扩展开放，修改关闭。可以实现BeanPostProcessors()
>
> 正在创建的时候，放入earlySingletonObjects.【性能提升】

**如何关闭循环依赖？**

```java
//在refresh以前修改
 AnnotationConfigApplicationContext context = new 	        AnnotationConfigApplicationContext(Application.class);
 context.setAllowCircularReferences(false);
 context.refresh();
//修改构造方法
    public AnnotationConfigApplicationContext() {
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }
```

#### 4.2 spring的策略模式

调用所有的后置处理器`postBeanProcessors`

#### 4.3 spring的扩展点

实现`BeanFactoryPostProcessor`

#### 4.4 Spring的生命周期回调方法

+ init()
+ postConstruct
+ preDestory

实现方式： 1. 使用注解  2. 实现InitializingBean  3. 配置xml

```xml
Multiple lifecycle mechanisms configured for the same bean, with different initialization methods, are called as follows:

Methods annotated with @PostConstruct
afterPropertiesSet() as defined by the InitializingBean callback interface
A custom configured init() method
```



### 五. 属性的自动注入

+ 构造方法
+ `setter()`
+ `@Autowired`
+ 