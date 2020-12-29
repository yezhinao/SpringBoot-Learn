# SpringBoot学习-数据访问

# 参考资料

http://blog.didispace.com/spring-boot-learning-2x/

[TOC]



## 一、使用JdbcTemplate访问MySQL数据库

### 1.新建项目添加依赖

```xml
<!--        数据源配置-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<!--        嵌入式数据库支持-->
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <scope>runtime</scope>
</dependency>

<!--        连接生产数据源-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
```



### 2.配置数据源信息

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```



### 3.创建表

```sql
CREATE TABLE `User` (  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,  `age` int NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```



### 4.创建对象

```java
@Data
@NoArgsConstructor
public class User {

    private String name;
    private Integer age;

}
```



### 5.创建接口和实现类

```java
public interface UserService {

    /**
     * 新增一个用户
     *
     * @param name
     * @param age
     */
    int create(String name, Integer age);

    /**
     * 根据name查询用户
     *
     * @param name
     * @return
     */
    List<User> getByName(String name);

    /**
     * 根据name删除用户
     *
     * @param name
     */
    int deleteByName(String name);

    /**
     * 获取用户总量
     */
    int getAllUsers();

    /**
     * 删除所有用户
     */
    int deleteAllUsers();

}
```



```java
@Service
public class UserServiceImpl implements UserService {

    private JdbcTemplate jdbcTemplate;

    UserServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int create(String name, Integer age) {
        return jdbcTemplate.update("insert into USER(NAME, AGE) values(?, ?)", name, age);
    }

    @Override
    public List<User> getByName(String name) {
        List<User> users = jdbcTemplate.query("select NAME, AGE from USER where NAME = ?", (resultSet, i) -> {
            User user = new User();
            user.setName(resultSet.getString("NAME"));
            user.setAge(resultSet.getInt("AGE"));
            return user;
        }, name);
        return users;
    }

    @Override
    public int deleteByName(String name) {
        return jdbcTemplate.update("delete from USER where NAME = ?", name);
    }

    @Override
    public int getAllUsers() {
        return jdbcTemplate.queryForObject("select count(1) from USER", Integer.class);
    }

    @Override
    public int deleteAllUsers() {
        return jdbcTemplate.update("delete from USER");
    }

}
```

### 6.编写测试方法

```java
@SpringBootTest
class YezhinaoApplicationTests {

    @Autowired
    private UserService userSerivce;

    @BeforeEach
    public void setUp() {
        // 准备，清空user表
        userSerivce.deleteAllUsers();
    }

    @Test
    public void test() throws Exception {
        // 插入5个用户
        userSerivce.create("Tom", 10);
        userSerivce.create("Mike", 11);
        userSerivce.create("Didispace", 30);
        userSerivce.create("Oscar", 21);
        userSerivce.create("Linda", 17);

        // 查询名为Oscar的用户，判断年龄是否匹配
        List<User> userList = userSerivce.getByName("Oscar");
        Assert.assertEquals(21, userList.get(0).getAge().intValue());

        // 查数据库，应该有5个用户
        Assert.assertEquals(5, userSerivce.getAllUsers());

        // 删除两个用户
        userSerivce.deleteByName("Tom");
        userSerivce.deleteByName("Mike");

        // 查数据库，应该有5个用户
        Assert.assertEquals(3, userSerivce.getAllUsers());

    }

}
```

### 7.结果

![image-20201228173020186](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228173020186.png)

### 8.<font color='red'>可能会遇到的问题</font>：

* sql_mode

​	不影响结果。

```

[Err] 1055 - Expression #1 of ORDER BY clause is not in GROUP BY clause and contains nonaggregated column 'information_schema.PROFILING.SEQ' which is not functionally dependent on columns in GROUP BY clause; this is incompatible with sql_mode=only_full_group_by
```

强迫症可采用：在my.ini里添加

```sql
[mysqld] 
sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
```



* sql时区问题time_zone

* root登入mysql

* ```sql
  show variables like '%time_zone%';
  ```

* ```
  set global time_zone='+8:00';
  ```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/JdbcTemplate

##  二、默认数据源Hikari的配置详解

### 1.**什么是JDBC？**

> Java数据库连接（Java Database Connectivity，简称<font color='orange'>JDBC</font>）是Java语言中用来规范客户端程序如何来访问数据库的应用程序接口，提供了诸如查询和更新数据库中数据的方法。JDBC也是Sun Microsystems的商标。我们通常说的JDBC是面向关系型数据库的。



- <font color='orange'>DriverManager</font>：负责加载各种不同驱动程序（Driver），并根据不同的请求，向调用者返回相应的数据库连接（Connection）。
- <font color='orange'>Driver</font>：驱动程序，会将自身加载到DriverManager中去，并处理相应的请求并返回相应的数据库连接（Connection）。
- <font color='orange'>Connection</font>：数据库连接，负责与进行数据库间通讯，SQL执行以及事务处理都是在某个特定Connection环境中进行的。可以产生用以执行SQL的Statement。
- <font color='orange'>Statement</font>：用以执行SQL查询和更新（针对静态SQL语句和单次执行）。<font color='orange'>PreparedStatement</font>：用以执行包含动态参数的SQL查询和更新（在服务器端编译，允许重复执行以提高效率）。
- <font color='orange'>CallableStatement</font>：用以调用数据库中的存储过程。
- <font color='orange'>SQLException</font>：代表在数据库连接的建立和关闭和SQL语句的执行过程中发生了例外情况（即错误）。



### 2.**数据源的作用**

主要出于以下几个目的：

* 封装关于数据库访问的各种参数，实现统一管理

* 通过对数据库的连接池管理，节省开销并提高效率



数据源有：DBCP、C3P0、Druid、HikariCP等。





### 3.默认数据源：HikariCP

* 通用配置：以`spring.datasource.*`的形式存在

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```



* 数据源连接池配置：以`spring.datasource.<数据源名称>.*`

```properties
# 最小空闲连接，默认值10，小于0或大于maximum-pool-size，都会重置为maximum-pool-size
spring.datasource.hikari.minimum-idle=10
# 最大连接数，小于等于0会被重置为默认值10；大于零小于1会被重置为minimum-idle的值
spring.datasource.hikari.maximum-pool-size=20
# 空闲连接超时时间，默认值600000（10分钟），大于等于max-lifetime且max-lifetime>0，会被重置为0；不等于0且小于10秒，会被重置为10秒。
spring.datasource.hikari.idle-timeout=500000
# 连接最大存活时间.不等于0且小于30秒，会被重置为默认值30分钟.设置应该比mysql设置的超时时间短
spring.datasource.hikari.max-lifetime=540000
# 连接超时时间：毫秒，小于250毫秒，否则被重置为默认值30秒
spring.datasource.hikari.connection-timeout=60000
# 用于测试连接是否可用的查询语句
spring.datasource.hikari.connection-test-query=SELECT 1
```



* 更多完整配置项可查看下表：

| **name**                  | **描述**                                                     | **构造器默认值**               | **默认配置validate之后的值** | **validate重置**                                             |
| :------------------------ | :----------------------------------------------------------- | :----------------------------- | :--------------------------- | :----------------------------------------------------------- |
| autoCommit                | 自动提交从池中返回的连接                                     | TRUE                           | TRUE                         | –                                                            |
| connectionTimeout         | 等待来自池的连接的最大毫秒数                                 | SECONDS.toMillis(30) = 30000   | 30000                        | 如果小于250毫秒，则被重置回30秒                              |
| idleTimeout               | 连接允许在池中闲置的最长时间                                 | MINUTES.toMillis(10) = 600000  | 600000                       | 如果idleTimeout+1秒>maxLifetime 且 maxLifetime>0，则会被重置为0（代表永远不会退出）；如果idleTimeout!=0且小于10秒，则会被重置为10秒 |
| maxLifetime               | 池中连接最长生命周期                                         | MINUTES.toMillis(30) = 1800000 | 1800000                      | 如果不等于0且小于30秒则会被重置回30分钟                      |
| connectionTestQuery       | 如果您的驱动程序支持JDBC4，我们强烈建议您不要设置此属性      | null                           | null                         | –                                                            |
| minimumIdle               | 池中维护的最小空闲连接数                                     | -1                             | 10                           | minIdle<0或者minIdle>maxPoolSize,则被重置为maxPoolSize       |
| maximumPoolSize           | 池中最大连接数，包括闲置和使用中的连接                       | -1                             | 10                           | 如果maxPoolSize小于1，则会被重置。当minIdle<=0被重置为DEFAULT_POOL_SIZE则为10;如果minIdle>0则重置为minIdle的值 |
| metricRegistry            | 该属性允许您指定一个 Codahale / Dropwizard MetricRegistry 的实例，供池使用以记录各种指标 | null                           | null                         | –                                                            |
| healthCheckRegistry       | 该属性允许您指定池使用的Codahale / Dropwizard HealthCheckRegistry的实例来报告当前健康信息 | null                           | null                         | –                                                            |
| poolName                  | 连接池的用户定义名称，主要出现在日志记录和JMX管理控制台中以识别池和池配置 | null                           | HikariPool-1                 | –                                                            |
| initializationFailTimeout | 如果池无法成功初始化连接，则此属性控制池是否将 fail fast     | 1                              | 1                            | –                                                            |
| isolateInternalQueries    | 是否在其自己的事务中隔离内部池查询，例如连接活动测试         | FALSE                          | FALSE                        | –                                                            |
| allowPoolSuspension       | 控制池是否可以通过JMX暂停和恢复                              | FALSE                          | FALSE                        | –                                                            |
| readOnly                  | 从池中获取的连接是否默认处于只读模式                         | FALSE                          | FALSE                        | –                                                            |
| registerMbeans            | 是否注册JMX管理Bean（MBeans）                                | FALSE                          | FALSE                        | –                                                            |
| catalog                   | 为支持 catalog 概念的数据库设置默认 catalog                  | driver default                 | null                         | –                                                            |
| connectionInitSql         | 该属性设置一个SQL语句，在将每个新连接创建后，将其添加到池中之前执行该语句。 | null                           | null                         | –                                                            |
| driverClassName           | HikariCP将尝试通过仅基于jdbcUrl的DriverManager解析驱动程序，但对于一些较旧的驱动程序，还必须指定driverClassName | null                           | null                         | –                                                            |
| transactionIsolation      | 控制从池返回的连接的默认事务隔离级别                         | null                           | null                         | –                                                            |
| validationTimeout         | 连接将被测试活动的最大时间量                                 | SECONDS.toMillis(5) = 5000     | 5000                         | 如果小于250毫秒，则会被重置回5秒                             |
| leakDetectionThreshold    | 记录消息之前连接可能离开池的时间量，表示可能的连接泄漏       | 0                              | 0                            | 如果大于0且不是单元测试，则进一步判断：(leakDetectionThreshold < SECONDS.toMillis(2) or (leakDetectionThreshold > maxLifetime && maxLifetime > 0)，会被重置为0 . 即如果要生效则必须>0，而且不能小于2秒，而且当maxLifetime > 0时不能大于maxLifetime |
| dataSource                | 这个属性允许你直接设置数据源的实例被池包装，而不是让HikariCP通过反射来构造它 | null                           | null                         | –                                                            |
| schema                    | 该属性为支持模式概念的数据库设置默认模式                     | driver default                 | null                         | –                                                            |
| threadFactory             | 此属性允许您设置将用于创建池使用的所有线程的java.util.concurrent.ThreadFactory的实例。 | null                           | null                         | –                                                            |
| scheduledExecutor         | 此属性允许您设置将用于各种内部计划任务的java.util.concurrent.ScheduledExecutorService实例 | null                           | null                         | –                                                            |





## 三、使用国产数据库连接池Druid



### 1.配置Druid数据源

#### ①添加依赖

* 通用依赖：

```xml
<!--        数据源配置-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!--        连接生产数据源-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```



* druid依赖

```xml
<!--配置Druid数据源-->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.1.21</version>
</dependency>
```



#### ②在`application.properties`中配置数据库连接信息。

* 通用配置：Druid的配置都以`spring.datasource.druid`作为前缀

```properties
#基础配置
spring.datasource.druid.url=jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai
spring.datasource.druid.username=root
spring.datasource.druid.password=123456
spring.datasource.druid.driver-class-name=com.mysql.cj.jdbc.Driver
```



* Druid连接池配置

```properties
#初始化时建立物理连接的个数。初始化发生在显示调用init方法，或者第一次getConnection时
spring.datasource.druid.initialSize=10
#最大连接池数量
spring.datasource.druid.maxActive=20
#获取连接时最大等待时间，单位毫秒。配置了maxWait之后，缺省启用公平锁，并发效率会有所下降，如果需要可以通过配置useUnfairLock属性为true使用非公平锁。
spring.datasource.druid.maxWait=60000
#最小连接池数量
spring.datasource.druid.minIdle=1
#有两个含义： 1) Destroy线程会检测连接的间隔时间，如果连接空闲时间大于等于minEvictableIdleTimeMillis则关闭物理连接。 2) testWhileIdle的判断依据，详细看testWhileIdle属性的说明
spring.datasource.druid.timeBetweenEvictionRunsMillis=60000
#连接保持空闲而不被驱逐的最小时间
spring.datasource.druid.minEvictableIdleTimeMillis=300000
#建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。
spring.datasource.druid.testWhileIdle=true
#申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。
spring.datasource.druid.testOnBorrow=true
#归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。
spring.datasource.druid.testOnReturn=false
#是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性能提升巨大，比如说oracle。在mysql下建议关闭。
spring.datasource.druid.poolPreparedStatements=true
#要启用PSCache，必须配置大于0，当大于0时，poolPreparedStatements自动触发修改为true。在Druid中，不会存在Oracle下PSCache占用内存过多的问题，可以把这个数值配置大一些，比如说100
spring.datasource.druid.maxOpenPreparedStatements=20
#要启用PSCache，必须配置大于0，当大于0时，poolPreparedStatements自动触发修改为true。在Druid中，不会存在Oracle下PSCache占用内存过多的问题，可以把这个数值配置大一些，比如说100
spring.datasource.druid.validationQuery=SELECT 1
#单位：秒，检测连接是否有效的超时时间。底层调用jdbc Statement对象的void setQueryTimeout(int seconds)方法
spring.datasource.druid.validation-query-timeout=500
#属性类型是字符串，通过别名的方式配置扩展插件，常用的插件有： 监控统计用的filter:stat 日志用的filter:log4j 防御sql注入的filter:wall
spring.datasource.druid.filters=stat
```

* 关于Druid中各连接池配置的说明可查阅下面的表格：

| 配置                                      | 缺省值             | 说明                                                         |
| :---------------------------------------- | :----------------- | :----------------------------------------------------------- |
| name                                      |                    | 配置这个属性的意义在于，如果存在多个数据源，监控的时候可以通过名字来区分开来。如果没有配置，将会生成一个名字，格式是：”DataSource-“ + System.identityHashCode(this). 另外配置此属性至少在1.0.5版本中是不起作用的，强行设置name会出错。[详情-点此处](http://blog.csdn.net/lanmo555/article/details/41248763)。 |
| url                                       |                    | 连接数据库的url，不同数据库不一样。例如： mysql : jdbc:mysql://10.20.153.104:3306/druid2 oracle : jdbc:oracle:thin:@10.20.149.85:1521:ocnauto |
| username                                  |                    | 连接数据库的用户名                                           |
| password                                  |                    | 连接数据库的密码。如果你不希望密码直接写在配置文件中，可以使用ConfigFilter。[详细看这里](https://github.com/alibaba/druid/wiki/使用ConfigFilter) |
| driverClassName                           | 根据url自动识别    | 这一项可配可不配，如果不配置druid会根据url自动识别dbType，然后选择相应的driverClassName |
| initialSize                               | 0                  | 初始化时建立物理连接的个数。初始化发生在显示调用init方法，或者第一次getConnection时 |
| maxActive                                 | 8                  | 最大连接池数量                                               |
| maxIdle                                   | 8                  | 已经不再使用，配置了也没效果                                 |
| minIdle                                   |                    | 最小连接池数量                                               |
| maxWait                                   |                    | 获取连接时最大等待时间，单位毫秒。配置了maxWait之后，缺省启用公平锁，并发效率会有所下降，如果需要可以通过配置useUnfairLock属性为true使用非公平锁。 |
| poolPreparedStatements                    | false              | 是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性能提升巨大，比如说oracle。在mysql下建议关闭。 |
| maxPoolPreparedStatementPerConnectionSize | -1                 | 要启用PSCache，必须配置大于0，当大于0时，poolPreparedStatements自动触发修改为true。在Druid中，不会存在Oracle下PSCache占用内存过多的问题，可以把这个数值配置大一些，比如说100 |
| validationQuery                           |                    | 用来检测连接是否有效的sql，要求是一个查询语句，常用select ‘x’。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。 |
| validationQueryTimeout                    |                    | 单位：秒，检测连接是否有效的超时时间。底层调用jdbc Statement对象的void setQueryTimeout(int seconds)方法 |
| testOnBorrow                              | true               | 申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 |
| testOnReturn                              | false              | 归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 |
| testWhileIdle                             | false              | 建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。 |
| keepAlive                                 | false （1.0.28）   | 连接池中的minIdle数量以内的连接，空闲时间超过minEvictableIdleTimeMillis，则会执行keepAlive操作。 |
| timeBetweenEvictionRunsMillis             | 1分钟（1.0.14）    | 有两个含义： 1) Destroy线程会检测连接的间隔时间，如果连接空闲时间大于等于minEvictableIdleTimeMillis则关闭物理连接。 2) testWhileIdle的判断依据，详细看testWhileIdle属性的说明 |
| numTestsPerEvictionRun                    | 30分钟（1.0.14）   | 不再使用，一个DruidDataSource只支持一个EvictionRun           |
| minEvictableIdleTimeMillis                |                    | 连接保持空闲而不被驱逐的最小时间                             |
| connectionInitSqls                        |                    | 物理连接初始化的时候执行的sql                                |
| exceptionSorter                           | 根据dbType自动识别 | 当数据库抛出一些不可恢复的异常时，抛弃连接                   |
| filters                                   |                    | 属性类型是字符串，通过别名的方式配置扩展插件，常用的插件有： 监控统计用的filter:stat 日志用的filter:log4j 防御sql注入的filter:wall |
| proxyFilters                              |                    | 类型是List<com.alibaba.druid.filter.Filter>，如果同时配置了filters和proxyFilters，是组合关系，并非替换关系 |



* 完成了默认数据源HikariCP切换到Druid的所有操作。

![image-20201229093344969](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229093344969.png)



### 2.配置Druid监控



#### ①添加依赖

```xml
<!--        配置Druid监控-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```



#### ②在`application.properties`中添加Druid的监控配置。

```properties
#配置Druid监控
spring.datasource.druid.stat-view-servlet.enabled=true
#访问地址规则
spring.datasource.druid.stat-view-servlet.url-pattern=/druid/*
#是否允许清空统计数据
spring.datasource.druid.stat-view-servlet.reset-enable=true
#监控页面的登录账户
spring.datasource.druid.stat-view-servlet.login-username=admin
#监控页面的登录密码
spring.datasource.druid.stat-view-servlet.login-password=admin
```



#### ③开启防御sql注入的filter:wall

为了能在监控页面中监控SQL防火墙

```properties
spring.datasource.druid.filters=stat,wall
```



#### ④在HIkari的项目基础上创建一个UserController

```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public int create(@RequestBody User user) {
        return userService.create(user.getName(), user.getAge());
    }

    @GetMapping("/user/{name}")
    public List<User> getByName(@PathVariable String name) {
        return userService.getByName(name);
    }

    @DeleteMapping("/user/{name}")
    public int deleteByName(@PathVariable String name) {
        return userService.deleteByName(name);
    }

    @GetMapping("/user/count")
    public int getAllUsers() {
        return userService.getAllUsers();
    }

    @DeleteMapping("/user/all")
    public int deleteAllUsers() {
        return userService.deleteAllUsers();
    }
}
```



#### ⑤启动应用，访问Druid的监控页面

http://localhost:8080/druid/

![image-20201229094715348](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229094715348.png)

用户名和密码分别为一下配置的值：

<font color='orange'>spring.datasource.druid.stat-view-servlet.login-username</font>

<font color='orange'>spring.datasource.druid.stat-view-servlet.login-password</font>



* 首页

![image-20201229094936482](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229094936482.png)

* 数据源

![image-20201229094955416](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229094955416.png)

* SQL监控

![image-20201229095102429](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229095102429.png)

* SQL防火墙

![image-20201229095122682](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229095122682.png)



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/JdbcDruid

## 四、使用Spring Data JPA访问MySQL

### 1.什么是JPA

* <font color='red'>JPA</font>是<font color='orange'>Java Persistence API</font>的简称，中文名Java持久层API

* 主要目标之一就是提供更加简单的编程模型：在JPA框架下创建实体和创建Java 类一样简单，没有任何的约束和限制，只需要使用 javax.persistence.Entity进行注释，JPA的框架和接口也都非常简单，没有太多特别的规则和设计模式的要求，开发者可以很容易地掌握。JPA基于非侵入式原则设计，因此可以很容易地和其它框架或者容器集成。



### 2.使用步骤



#### ①添加依赖

```xml
<!--        连接mysql-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!--        配置JPA-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```



#### ②在`application.xml`中配置

数据库连接信息（如使用嵌入式数据库则不需要）、自动创建表结构的设置

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

#hibernate的配置属性，其主要作用是：自动创建、更新、验证数据库表结构。
spring.jpa.properties.hibernate.hbm2ddl.auto=update

```

<font color='orange'>spring.jpa.properties.hibernate.hbm2ddl.auto</font>的参数

- `create`：每次加载hibernate时都会删除上一次的生成的表，然后根据你的model类再重新来生成新表，哪怕两次没有任何改变也要这样执行，这就是导致数据库表数据丢失的一个重要原因。
- `create-drop`：每次加载hibernate时根据model类生成表，但是sessionFactory一关闭,表就自动删除。
- `update`：最常用的属性，第一次加载hibernate时根据model类会自动建立起表的结构（前提是先建立好数据库），以后加载hibernate时根据model类自动更新表结构，即使表结构改变了但表中的行仍然存在不会删除以前的行。要注意的是当部署到服务器后，表结构是不会被马上建立起来的，是要等应用第一次运行起来后才会。
- `validate`：每次加载hibernate时，验证创建数据库表结构，只会和数据库中的表进行比较，不会创建新表，但是会插入新值。



#### ③创建实体

通过ORM框架映射到数据库表中，由于配置了`hibernate.hbm2ddl.auto`，在应用启动的时候框架会自动去数据库中创建对应的表。



```java
@Entity
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



- `@Entity`注解标识了User类是一个持久化的实体
- `@Data`和`@NoArgsConstructor`是Lombok中的注解。用来自动生成各参数的Set、Get函数以及不带参数的构造函数。
- `@Id`和`@GeneratedValue`用来标识User对应对应数据库表中的主键



#### ④创建数据访问接口

```java
public interface UserRepository extends JpaRepository<User, Long> {

    User findByName(String name);

    User findByNameAndAge(String name, Integer age);

    @Query("from User u where u.name=:name")
    User findUser(@Param("name") String name);

}
```



- `User findByName(String name)`
- `User findByNameAndAge(String name, Integer age)`

它们分别实现了按name查询User实体和按name和age查询User实体，可以看到我们这里没有任何类SQL语句就完成了两个条件查询方法。这就是Spring-data-jpa的一大特性：**通过解析方法名创建查询**。



#### ⑤单元测试

```java
@RunWith(SpringRunner.class)
@SpringBootTest
class JpaApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void test() throws Exception {

        // 创建10条记录
        userRepository.save(new User("AAA", 10));
        userRepository.save(new User("BBB", 20));
        userRepository.save(new User("CCC", 30));
        userRepository.save(new User("DDD", 40));
        userRepository.save(new User("EEE", 50));
        userRepository.save(new User("FFF", 60));
        userRepository.save(new User("GGG", 70));
        userRepository.save(new User("HHH", 80));
        userRepository.save(new User("III", 90));
        userRepository.save(new User("JJJ", 100));

        // 测试findAll, 查询所有记录
        Assert.assertEquals(10, userRepository.findAll().size());

        // 测试findByName, 查询姓名为FFF的User
        Assert.assertEquals(60, userRepository.findByName("FFF").getAge().longValue());

        // 测试findUser, 查询姓名为FFF的User
        Assert.assertEquals(60, userRepository.findUser("FFF").getAge().longValue());

        // 测试findByNameAndAge, 查询姓名为FFF并且年龄为60的User
        Assert.assertEquals("FFF", userRepository.findByNameAndAge("FFF", 60).getName());

        // 测试删除姓名为AAA的User
        userRepository.delete(userRepository.findByName("AAA"));

        // 测试findAll, 查询所有记录, 验证上面的删除是否成功
        Assert.assertEquals(9, userRepository.findAll().size());

    }
}
```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/JPA



## 五、使用MyBatis访问MySQL

### 1.整合MyBatis

#### ①添加依赖

```xml
<!--        mybatis-->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.1</version>
</dependency>

<!--        mysql-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```



#### ②在`application.properties`中配置mysql的连接配置

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```



#### ③创建User表

```sql
CREATE TABLE `User` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```



#### ④创建User表的映射对象User

```java
@Data
@NoArgsConstructor
public class User {

    private Long id;

    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



#### ⑤创建User表的操作接口：UserMapper。

```java
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM USER WHERE NAME = #{name}")
    User findByName(@Param("name") String name);

    @Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

}
```





`@Param`中定义的`name`对应了SQL中的`#{name}`，`age`对应了SQL中的`#{age}`。





#### ⑥创建单元测试

```java
//是用作日志输出的
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class MybatisApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
//   测试结束回滚数据，保证测试单元每次运行的数据环境独立
    @Transactional
    public void test() throws Exception {
        userMapper.insert("AAA", 20);
        User u = userMapper.findByName("AAA");
        Assert.assertEquals(20, u.getAge().intValue());
    }
}
```



### 2.使用Map

* 通过`Map<String, Object>`对象来作为传递参数的容器：

UserMapper下添加

```java
@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name,jdbcType=VARCHAR}, #{age,jdbcType=INTEGER})")
int insertByMap(Map<String, Object> map);
```



测试类下添加

```java
@Test
@Transactional
public void test2(){
    Map<String, Object> map = new HashMap<>();
    map.put("name", "BBB");
    map.put("age", 20);
    userMapper.insertByMap(map);
    User u = userMapper.findByName("BBB");
    Assert.assertEquals(20, u.getAge().intValue());
}
```



### 3.使用对象

除了Map对象，我们也可直接使用普通的Java对象来作为查询条件的传参，比如我们可以直接使用User对象:

```java
@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")int insertByUser(User user);
```



### 4.增删改查

UserMapper下添加

```java
@Select("SELECT * FROM USER WHERE NAME = #{name}")
User findByName(@Param("name") String name);

@Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
int insert(@Param("name") String name, @Param("age") Integer age);


@Update("UPDATE user SET age=#{age} WHERE name=#{name}")
void update(User user);

@Delete("DELETE FROM user WHERE id =#{id}")
void delete(Long id);
```



测试类下添加

```java
@Test
@Transactional
public void testUserMapper() throws Exception {
    // insert一条数据，并select出来验证
    userMapper.insert("AAA", 20);
    User u = userMapper.findByName("AAA");
    Assert.assertEquals(20, u.getAge().intValue());
    // update一条数据，并select出来验证
    u.setAge(30);
    userMapper.update(u);
    u = userMapper.findByName("AAA");
    Assert.assertEquals(30, u.getAge().intValue());
    // 删除这条数据，并select验证
    userMapper.delete(u.getId());
    u = userMapper.findByName("AAA");
    Assert.assertEquals(null, u);
}
```



### 5.返回结果绑定

UserMapper下添加

<font color='orange'>@Result</font>中的<font color='red'>property</font>属性对应User对象中的成员名，<font color='red'>column</font>对应SELECT出的字段名。在该配置中故意没有查出id属性，只对User对应中的name和age对象做了映射配置

```java
@Results({
        @Result(property = "name", column = "name"),
        @Result(property = "age", column = "age")
})
@Select("SELECT name, age FROM user")
List<User> findAll();
```



测试类下添加

```java
@Test
@Transactional
public void testUserMapper2() throws Exception {
    List<User> userList = userMapper.findAll();
    for(User user : userList) {
        System.out.println(user);
        Assert.assertEquals(null, user.getId());
        Assert.assertNotEquals(null, user.getName());
    }
}
```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/Mybatis



## 六、使用MyBatis的XML配置方式

### 1.目录分级

![image-20201229134707718](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229134707718.png)



### 2.添加依赖

```xml
<!--        mybatis-->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.1</version>
</dependency>

<!--        mysql-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```



### 3.创建User类和user表

```java
@Data
@NoArgsConstructor
public class User {

    private Long id;

    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



```sql
CREATE TABLE `User` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```



### 4.指定的Mapper包下创建User表的Mapper定义：

```java
@Mapper
public interface UserMapper {


    User findByName(@Param("name") String name);

    int insert(@Param("name") String name, @Param("age") Integer age);
}
```



### 5.配置参数

在配置文件中通过`mybatis.mapper-locations`参数指定xml配置的位置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis.mapper-locations=classpath:mapper/*.xml
```



### 6.创建UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mybatis.mapper.UserMapper">
    <select id="findByName" resultType="com.example.mybatis.entity.User">
        SELECT * FROM USER WHERE NAME = #{name}
    </select>

    <insert id="insert">
        INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})
    </insert>
</mapper>
```



### 7.创建测试方法

```java
//是用作日志输出的
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class MybatisApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    @Rollback(true)
    public void test() throws Exception {
        userMapper.insert("AAA", 20);
        User u = userMapper.findByName("AAA");
        Assert.assertEquals(20, u.getAge().intValue());
    }
}
```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/Mybatis-xml



## 七、JdbcTemplate的多数据源配置

### 1.创建User表

```sql
CREATE TABLE `User` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```



### 2.添加依赖

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```



### 3.添加多数据源的配置

```java
spring.datasource.primary.jdbc-url=jdbc:mysql://localhost:3306/test1?serverTimezone=Asia/Shanghai
spring.datasource.primary.username=root
spring.datasource.primary.password=123456
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

spring.datasource.secondary.jdbc-url=jdbc:mysql://localhost:3306/test2?serverTimezone=Asia/Shanghai
spring.datasource.secondary.username=root
spring.datasource.secondary.password=123456
spring.datasource.secondary.driver-class-name=com.mysql.cj.jdbc.Driver
```



### 4.初始化数据源与JdbcTemplate

```java
@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource primaryDataSource) {
        return new JdbcTemplate(primaryDataSource);
    }

    @Bean
    public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        return new JdbcTemplate(secondaryDataSource);
    }

}
```

1. 前两个Bean是数据源的创建，通过`@ConfigurationProperties`可以知道这两个数据源分别加载了`spring.datasource.primary.*`和`spring.datasource.secondary.*`的配置。
2. `@Primary`注解指定了主数据源，就是当我们不特别指定哪个数据源的时候，就会使用这个Bean
3. 后两个Bean是每个数据源对应的`JdbcTemplate`。可以看到这两个`JdbcTemplate`创建的时候，分别注入了`primaryDataSource`数据源和`secondaryDataSource`数据源



### 5.测试类

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class JdbcApplicationTests {

    @Autowired
    protected JdbcTemplate primaryJdbcTemplate;

    @Autowired
    protected JdbcTemplate secondaryJdbcTemplate;

    @BeforeEach
    public void setUp() {
        primaryJdbcTemplate.update("DELETE  FROM  USER ");
        secondaryJdbcTemplate.update("DELETE  FROM  USER ");
    }

    @Test
    @Rollback(true)
    public void test() throws Exception {
        // 往第一个数据源中插入 2 条数据
        primaryJdbcTemplate.update("insert into user(name,age) values(?, ?)", "aaa", 20);
        primaryJdbcTemplate.update("insert into user(name,age) values(?, ?)", "bbb", 30);

        // 往第二个数据源中插入 1 条数据，若插入的是第一个数据源，则会主键冲突报错
        secondaryJdbcTemplate.update("insert into user(name,age) values(?, ?)", "ccc", 20);

        // 查一下第一个数据源中是否有 2 条数据，验证插入是否成功
        Assert.assertEquals("2", primaryJdbcTemplate.queryForObject("select count(1) from user", String.class));

        // 查一下第一个数据源中是否有 1 条数据，验证插入是否成功
        Assert.assertEquals("1", secondaryJdbcTemplate.queryForObject("select count(1) from user", String.class));
    }

}
```

1. 可能这里你会问，有两个JdbcTemplate，为什么不用`@Qualifier`指定？这里顺带说个小知识点，当我们不指定的时候，会采用参数的名字来查找Bean，存在的话就注入。
2. 这两个JdbcTemplate创建的时候，我们也没指定名字，它们是如何匹配上的？这里也是一个小知识点，当我们创建Bean的时候，默认会使用方法名称来作为Bean的名称，所以这里就对应上了。



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/JdbcTemplate-Datas



## 八、Spring Data JPA的多数据源配置

### 1.目录分级

![image-20201229154245185](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229154245185.png)



### 2.添加依赖

```xml
<!--        连接mysql-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!--        配置JPA-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```



### 3.创建实体

<font color='red'>JPA会自动建表</font>

* User

```java
@Entity
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



* Message

```java
@Entity
@Data
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String message;

    public Message(String title, String message) {
        this.title = title;
        this.message = message;
    }

}
```



### 4.创建Repository

* UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {

}
```



* MessageRepository

```java
public interface MessageRepository extends JpaRepository<Message, Long> {
    
}
```



### 5.添加多数据源的配置

```properties
spring.datasource.primary.jdbc-url=jdbc:mysql://localhost:3306/test1?serverTimezone=Asia/Shanghai
spring.datasource.primary.username=root
spring.datasource.primary.password=123456
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

spring.datasource.secondary.jdbc-url=jdbc:mysql://localhost:3306/test2?serverTimezone=Asia/Shanghai
spring.datasource.secondary.username=root
spring.datasource.secondary.password=123456
spring.datasource.secondary.driver-class-name=com.mysql.cj.jdbc.Driver

# 日志打印执行的SQL
spring.jpa.show-sql=true
# 不会删除表
spring.jpa.hibernate.ddl-auto=update
```



### 6.单独建一个多数据源的配置类

* DataSourceConfiguration

```java
@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

}
```



### 7.分别创建两个数据源的JPA配置

* Primary数据源的JPA配置：

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef="entityManagerFactoryPrimary",
        transactionManagerRef="transactionManagerPrimary",
        basePackages= { "com.example.jpa.primary" }) //设置Repository所在位置
public class PrimaryConfig {

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private HibernateProperties hibernateProperties;

    private Map<String, Object> getVendorProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }

    @Primary
    @Bean(name = "entityManagerPrimary")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactoryPrimary(builder).getObject().createEntityManager();
    }

    @Primary
    @Bean(name = "entityManagerFactoryPrimary")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryPrimary (EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(primaryDataSource)
                .packages("com.example.jpa.primary") //设置实体类所在位置
                .persistenceUnit("primaryPersistenceUnit")
                .properties(getVendorProperties())
                .build();
    }

    @Primary
    @Bean(name = "transactionManagerPrimary")
    public PlatformTransactionManager transactionManagerPrimary(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactoryPrimary(builder).getObject());
    }

}
```

* Secondary数据源的JPA配置：

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef="entityManagerFactorySecondary",
        transactionManagerRef="transactionManagerSecondary",
        basePackages= { "com.example.jpa.secondary" }) //设置Repository所在位置
public class SecondaryConfig {

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Autowired
    private JpaProperties jpaProperties;
    @Autowired
    private HibernateProperties hibernateProperties;

    private Map<String, Object> getVendorProperties() {
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
    }

    @Bean(name = "entityManagerSecondary")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactorySecondary(builder).getObject().createEntityManager();
    }

    @Bean(name = "entityManagerFactorySecondary")
    public LocalContainerEntityManagerFactoryBean entityManagerFactorySecondary (EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(secondaryDataSource)
                .packages("com.example.jpa.secondary") //设置实体类所在位置
                .persistenceUnit("secondaryPersistenceUnit")
                .properties(getVendorProperties())
                .build();
    }

    @Bean(name = "transactionManagerSecondary")
    PlatformTransactionManager transactionManagerSecondary(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactorySecondary(builder).getObject());
    }

}
```

**说明与注意**：

- 在使用JPA的时候，需要为不同的数据源创建不同的package来存放对应的Entity和Repository，以便于配置类的分区扫描
- 类名上的注解`@EnableJpaRepositories`中指定Repository的所在位置
- `LocalContainerEntityManagerFactoryBean`创建的时候，指定Entity所在的位置
- 其他主要注意在互相注入时候，不同数据源不同配置的命名，基本就没有什么大问题了



### 8.创建测试类

```java
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class JpaApplicationTests {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    @Rollback
    public void test() throws Exception {
        userRepository.save(new User("aaa", 10));
        userRepository.save(new User("bbb", 20));
        userRepository.save(new User("ccc", 30));
        userRepository.save(new User("ddd", 40));
        userRepository.save(new User("eee", 50));

        Assert.assertEquals(5, userRepository.findAll().size());

        messageRepository.save(new Message("o1", "aaaaaaaaaa"));
        messageRepository.save(new Message("o2", "bbbbbbbbbb"));
        messageRepository.save(new Message("o3", "cccccccccc"));

        Assert.assertEquals(3, messageRepository.findAll().size());
    }

}

```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data



## 九、MyBatis的多数据源配置

### 1.目录分级

![image-20201229161924690](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229161924690.png)



### 2.添加依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.1</version>
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency
```



### 3.创建实体和User表

```sql
CREATE TABLE `User` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `age` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```



* UserPrimary

```java
@Data
@NoArgsConstructor
public class UserPrimary {

    private Long id;

    private String name;
    private Integer age;

    public UserPrimary(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



* UserSecondary

```java
@Data
@NoArgsConstructor
public class UserSecondary {

    private Long id;

    private String name;
    private Integer age;

    public UserSecondary(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}
```



### 4.添加多数据源的配置



```properties
spring.datasource.primary.jdbc-url=jdbc:mysql://localhost:3306/test1?serverTimezone=Asia/Shanghai
spring.datasource.primary.username=root
spring.datasource.primary.password=123456
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

spring.datasource.secondary.jdbc-url=jdbc:mysql://localhost:3306/test2?serverTimezone=Asia/Shanghai
spring.datasource.secondary.username=root
spring.datasource.secondary.password=123456
spring.datasource.secondary.driver-class-name=com.mysql.cj.jdbc.Driver
```



### 5.创建接口

* UserMapperPrimary

```java
public interface UserMapperPrimary {

    @Select("SELECT * FROM USER WHERE NAME = #{name}")
    UserPrimary findByName(@Param("name") String name);

    @Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

    @Delete("DELETE FROM USER")
    int deleteAll();

}
```



* UserMapperSecondary

```java
public interface UserMapperSecondary {

    @Select("SELECT * FROM USER WHERE NAME = #{name}")
    UserSecondary findByName(@Param("name") String name);

    @Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

    @Delete("DELETE FROM USER")
    int deleteAll();
}
```



### 6.单独建一个多数据源的配置类

* DataSourceConfiguration

```java
@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

}
```



### 7.分别创建两个数据源的MyBatis配置

* PrimaryConfig

```java
@Configuration
@MapperScan(
        basePackages = "com.example.mybatis.primary",
        sqlSessionFactoryRef = "sqlSessionFactoryPrimary",
        sqlSessionTemplateRef = "sqlSessionTemplatePrimary")
public class PrimaryConfig {

    private DataSource primaryDataSource;

    public PrimaryConfig(@Qualifier("primaryDataSource") DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactoryPrimary() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(primaryDataSource);
        return bean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplatePrimary() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactoryPrimary());
    }

}
```





* SecondaryConfig

```java
@Configuration
@MapperScan(
        basePackages = "com.example.mybatis.secondary",
        sqlSessionFactoryRef = "sqlSessionFactorySecondary",
        sqlSessionTemplateRef = "sqlSessionTemplateSecondary")
public class SecondaryConfig {

    private DataSource secondaryDataSource;

    public SecondaryConfig(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryDataSource = secondaryDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactorySecondary() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(secondaryDataSource);
        return bean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplateSecondary() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactorySecondary());
    }

}
```



**说明与注意**：

1. 配置类上使用`@MapperScan`注解来指定当前数据源下定义的Entity和Mapper的包路径；另外需要指定sqlSessionFactory和sqlSessionTemplate，这两个具体实现在该配置类中类中初始化。
2. 配置类的构造函数中，通过`@Qualifier`注解来指定具体要用哪个数据源，其名字对应在`DataSourceConfiguration`配置类中的数据源定义的函数名。
3. 配置类中定义SqlSessionFactory和SqlSessionTemplate的实现，注意具体使用的数据源正确。



### 8.测试验证

```java
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class MybatisApplicationTests {

    @Autowired
    private UserMapperPrimary userMapperPrimary;
    @Autowired
    private UserMapperSecondary userMapperSecondary;

    @BeforeEach
    public void setUp() {
        // 清空测试表，保证每次结果一样
        userMapperPrimary.deleteAll();
        userMapperSecondary.deleteAll();
    }

    @Test
    @Rollback
    public void test() throws Exception {
        // 往Primary数据源插入一条数据
        userMapperPrimary.insert("AAA", 20);

        // 从Primary数据源查询刚才插入的数据，配置正确就可以查询到
        UserPrimary userPrimary = userMapperPrimary.findByName("AAA");
        Assert.assertEquals(20, userPrimary.getAge().intValue());

        // 从Secondary数据源查询刚才插入的数据，配置正确应该是查询不到的
        UserSecondary userSecondary = userMapperSecondary.findByName("AAA");
        Assert.assertNull(userSecondary);

        // 往Secondary数据源插入一条数据
        userMapperSecondary.insert("BBB", 20);

        // 从Primary数据源查询刚才插入的数据，配置正确应该是查询不到的
        userPrimary = userMapperPrimary.findByName("BBB");
        Assert.assertNull(userPrimary);

        // 从Secondary数据源查询刚才插入的数据，配置正确就可以查询到
        userSecondary = userMapperSecondary.findByName("BBB");
        Assert.assertEquals(20, userSecondary.getAge().intValue());
    }

}
```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/Mybatis-Datas



## 十、事务管理入门

### 1.什么是事务

1. 记录失败的位置，问题修复之后，从上一次执行失败的位置开始继续执行后面要做的业务逻辑
2. 在执行失败的时候，回退本次执行的所有过程，让操作恢复到原始状态，带问题修复之后，重新执行原来的业务逻辑

事务就是针对上述方式2的实现。事务，一般是指要做的或所做的事情，就是上面所说的业务人员的一个操作（比如电商系统中，一个创建订单的操作包含了创建订单、商品库存的扣减两个基本操作。如果创建订单成功，库存扣减失败，那么就会出现商品超卖的问题，所以最基本的最发就是需要为这两个操作用事务包括起来，保证这两个操作要么都成功，要么都失败）。



### 2.快速入门

#### ①添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>


<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!--        验证规则包-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```



#### ②创建实体类

```java
@Entity
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @Max(50)
    private Integer age;

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

}
```



#### ③创建UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    User findByName(String name);

    User findByNameAndAge(String name, Integer age);

    @Query("from User u where u.name=:name")
    User findUser(@Param("name") String name);

}
```



#### ④添加配置

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MySQL5InnoDBDialect
spring.jpa.hibernate.ddl-auto=update
```



#### ⑤进行测试

```java
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class AffairApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void test() throws Exception {

        // 创建10条记录
        userRepository.save(new User("AAA", 10));
        userRepository.save(new User("BBB", 20));
        userRepository.save(new User("CCC", 30));
        userRepository.save(new User("DDD", 40));
        userRepository.save(new User("EEE", 50));
        userRepository.save(new User("FFF", 60));
        userRepository.save(new User("GGG", 70));
        userRepository.save(new User("HHH", 80));
        userRepository.save(new User("III", 90));
        userRepository.save(new User("JJJ", 100));

        // 测试findAll, 查询所有记录
        Assert.assertEquals(10, userRepository.findAll().size());

        // 测试findByName, 查询姓名为FFF的User
        Assert.assertEquals(60, userRepository.findByName("FFF").getAge().longValue());

        // 测试findUser, 查询姓名为FFF的User
        Assert.assertEquals(60, userRepository.findUser("FFF").getAge().longValue());

        // 测试findByNameAndAge, 查询姓名为FFF并且年龄为60的User
        Assert.assertEquals("FFF", userRepository.findByNameAndAge("FFF", 60).getName());

        // 测试删除姓名为AAA的User
        userRepository.delete(userRepository.findByName("AAA"));

        // 测试findAll, 查询所有记录, 验证上面的删除是否成功
        Assert.assertEquals(9, userRepository.findAll().size());

    }

}
```





可以看到，在这个单元测试用例中，使用UserRepository对象连续创建了10个User实体到数据库中，下面我们人为的来制造一些异常，看看会发生什么情况。

通过`@Max(50)`来为User的age设置最大值为50，这样通过创建时User实体的age属性超过50的时候就可以触发异常产生。

![image-20201229171239917](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229171239917.png)



* 数据库结果

![image-20201229171446800](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229171446800.png)



#### ⑥添加事务

![image-20201229171530077](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229171530077.png)



![image-20201229171651752](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229171651752.png)

* 数据库结果

![image-20201229171816675](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201229171816675.png)



这里主要通过单元测试演示了如何使用`@Transactional`注解来声明一个函数需要被事务管理，通常我们单元测试为了保证每个测试之间的数据独立，会使用`@Rollback`注解让每个单元测试都能在结束时回滚。而真正在开发业务逻辑时，我们通常在service层接口中使用`@Transactional`来对各个业务逻辑进行事务管理的配置，例如：



```java
public interface UserService {        
    @Transactional    
    User update(String name, String password);    
}
```





### 3.隔离级别

* 隔离级别是指若干个并发的事务之间的隔离程度，与我们开发时候主要相关的场景包括：脏读取、重复读、幻读。
* `org.springframework.transaction.annotation.Isolation`枚举类中定义了五个表示隔离级别的值：



```java
public enum Isolation {
    DEFAULT(-1),
    READ_UNCOMMITTED(1),
    READ_COMMITTED(2),
    REPEATABLE_READ(4),
    SERIALIZABLE(8);
} 

```



- `DEFAULT`：这是默认值，表示使用底层数据库的默认隔离级别。对大部分数据库而言，通常这值就是：`READ_COMMITTED`。
- `READ_UNCOMMITTED`：该隔离级别表示一个事务可以读取另一个事务修改但还没有提交的数据。该级别不能防止脏读和不可重复读，因此很少使用该隔离级别。
- `READ_COMMITTED`：该隔离级别表示一个事务只能读取另一个事务已经提交的数据。该级别可以防止脏读，这也是大多数情况下的推荐值。
- `REPEATABLE_READ`：该隔离级别表示一个事务在整个过程中可以多次重复执行某个查询，并且每次返回的记录都相同。即使在多次查询之间有新增的数据满足该查询，这些新增的记录也会被忽略。该级别可以防止脏读和不可重复读。
- `SERIALIZABLE`：所有的事务依次逐个执行，这样事务之间就完全不可能产生干扰，也就是说，该级别可以防止脏读、不可重复读以及幻读。但是这将严重影响程序的性能。通常情况下也不会用到该级别。



> 使用方法

通过使用`isolation`属性设置，例如：

```java
@Transactional(isolation = Isolation.DEFAULT)
```



### 4.传播行为

* 所谓事务的传播行为是指，如果在开始当前事务之前，一个事务上下文已经存在，此时有若干选项可以指定一个事务性方法的执行行为。
* `org.springframework.transaction.annotation.Propagation`枚举类中定义了6个表示传播行为的枚举值：

```java
public enum Propagation {
    REQUIRED(0),
    SUPPORTS(1),
    MANDATORY(2),
    REQUIRES_NEW(3),
    NOT_SUPPORTED(4),
    NEVER(5),
    NESTED(6);
} 

```

- `REQUIRED`：如果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务。
- `SUPPORTS`：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式继续运行。
- `MANDATORY`：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常。
- `REQUIRES_NEW`：创建一个新的事务，如果当前存在事务，则把当前事务挂起。
- `NOT_SUPPORTED`：以非事务方式运行，如果当前存在事务，则把当前事务挂起。
- `NEVER`：以非事务方式运行，如果当前存在事务，则抛出异常。
- `NESTED`：如果当前存在事务，则创建一个事务作为当前事务的嵌套事务来运行；如果当前没有事务，则该取值等价于`REQUIRED`。



> 使用方法

通过使用`propagation`属性设置，例如：

```java
@Transactional(propagation = Propagation.REQUIRED)
```



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Data/Affair