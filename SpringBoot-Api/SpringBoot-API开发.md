





#  SpringBoot学习-API开发

## 参考文章

Spring Boot 2.x基础教程http://blog.didispace.com/spring-boot-learning-2x/

[TOC]



##  一、构建RESTful API与单元测试

###  1.RESTful API是什么？

> 
>
> **REST**，表示性状态转移（representation state transfer）。简单来说，就是用`URI`表示资源，用HTTP方法(GET, POST, PUT, DELETE)表征对这些资源的操作。



- Resource: 资源，即数据，存在互联网上的可被访问的实体
- Representation： 数据的某种表现形式，如HTML, JSON。
- State Transfer：状态变化，HTTP方法实现



RESTful 是典型的基于HTTP的协议

1. 资源。首先要明确资源就是网络上的一个实体，可以是文本、图片、音频、视频。资源总是以一定的格式来表现自己。文本用txt、html；图片用JPG、JPEG等等。而JSON是RESTful API中最常用的资源表现格式。
2. 统一接口。对于业务数据的CRUD，RESTful 用HTTP方法与之对应。

![](https://upload-images.jianshu.io/upload_images/13880937-18695122bde092bd.png?imageMogr2/auto-orient/strip|imageView2/2/w/509/format/webp)



3. URI。统一资源标识符，它可以唯一标识一个资源。注意到，URL(统一资源定位符)是一种URI，因为它可以唯一标志资源。但**URL != URI**。应该说URL 是URI的子集。因为URL使用路径来唯一标识资源，这只是唯一标识资源的一种方式。还可以用一个唯一编号来标识资源，如example.html.**fuce2da23**。只不过这种方式并不被广泛使用。总之，要在概念上对URL和URI有所区分。
4. 无状态。 所谓无状态是指所有资源都可以用URI定位，而且这个定位与其他资源无关，不会因为其他资源的变动而变化。这里引入一个**幂等性**的概念：无论一个操作被执行一次还是多次，执行后的效果都相同。比如对某资源发送GET请求，如果访问一次和访问十次获得的数据一样，那么就说这个请求具有幂等性。
5. URL中只能有名词，不能出现动词。这是因为在REST要求对资源的操作由HTTP 方法给出，而方法是由HTTP 请求报文头部给出的，自然不需要在URL中暴露操作方式。





###  2.SpringBoot注解

- `@Controller`：修饰class，用来创建处理http请求的对象
- `@RestController`：Spring4之后加入的注解，原来在`@Controller`中返回json需要`@ResponseBody`来配合，如果直接用`@RestController`替代`@Controller`就不需要再配置`@ResponseBody`，默认返回json格式
- `@RequestMapping`：配置url映射。现在更多的也会直接用以Http Method直接关联的映射注解来定义，比如：`GetMapping`、`PostMapping`、`DeleteMapping`、`PutMapping`等
- 测试类采用`@RunWith(SpringRunner.class)`和`@SpringBootTest`修饰启动；
- <font color='orange'>@EnableSwagger2Doc</font>整合Swagger2



##  二、使用Swagger2构建强大的API文档



<font color='orange'>Swagger2</font>可以轻松的整合到Spring Boot中，并与Spring MVC程序配合组织出强大<font color='red'>RESTful API</font>文档。它既可以减少我们创建文档的工作量，同时说明内容又整合入实现代码中，让维护文档和修改代码整合为一体，可以让我们在修改代码逻辑的同时方便的修改文档说明。另外Swagger2也提供了强大的页面<font color='red'>测试</font>功能来调试每个RESTful API。

###  1.添加swagger-spring-boot-starter依赖

```xml
<dependency>
    <groupId>com.spring4all</groupId>
    <artifactId>swagger-spring-boot-starter</artifactId>
    <version>1.9.0.RELEASE</version>
</dependency>
```



###  2.应用主类中添加`@EnableSwagger2Doc`注解，具体如下

```java
@EnableSwagger2Doc
@SpringBootApplication
public class Chapter22Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter22Application.class, args);
    }

}
```



###  3.`application.properties`中配置文档相关内容，比如

```properties
#swagger.title：标题
swagger.title=spring-boot-starter-swagger
#swagger.description：描述
swagger.description=Starter for swagger 2.x
#swagger.version：版本
swagger.version=1.9.0.RELEASE
#swagger.license：许可证
swagger.license=Apache License, Version 2.0
#swagger.licenseUrl：许可证URL
swagger.licenseUrl=https://www.apache.org/licenses/LICENSE-2.0.html
#swagger.termsOfServiceUrl：服务条款URL
swagger.termsOfServiceUrl=https://github.com/dyc87112/spring-boot-starter-swagger
#swagger.contact.name：维护人
swagger.contact.name=didi
#swagger.contact.url：维护人URL
swagger.contact.url=http://blog.didispace.com
#swagger.contact.email：维护人email
swagger.contact.email=dyc87112@qq.com
#swagger.base-package：swagger扫描的基础包，默认：全扫描
swagger.base-package=com.didispace
#swagger.base-path：需要处理的基础URL规则，默认：/**
swagger.base-path=/**
```



###  4.启动应用，访问：`http://localhost:8080/swagger-ui.html`，就可以看到如下的接口文档页面：

![img](http://img.didispace.com/Frp7Fhk44jt5NzkRM5qxJqoXMWiS)

###  5.添加文档内容

给文档添加说明。

```java
@ApiModel(description="用户实体")

@ApiOperation(value = "更新用户详细信息", notes = "根据url的id来指定更新对象，并根据传过来的user信息来更新用户详细信息")

@ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "用户编号", required = true, example = "1")




```

例如：



```java
@Data
@ApiModel(description="用户实体")
public class User {

    @ApiModelProperty("用户编号")
    private Long id;
    @ApiModelProperty("用户姓名")
    private String name;
    @ApiModelProperty("用户年龄")
    private Integer age;

}
```

![img](http://img.didispace.com/FoxwzIgdkIIx6Z5_U8DZq5MqVQf_)



```java
@GetMapping("/{id}")
@ApiOperation(value = "获取用户详细信息", notes = "根据url的id来获取用户详细信息")
public User getUser(@PathVariable Long id) {
    return users.get(id);
}
```

![image-20201228095804995](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228095804995.png)



```java
@PutMapping("/{id}")
@ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "用户编号", required = true, example = "1")
@ApiOperation(value = "更新用户详细信息", notes = "根据url的id来指定更新对象，并根据传过来的user信息来更新用户详细信息")
public String putUser(@PathVariable Long id, @RequestBody User user)
```

![image-20201228100109896](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228100109896.png)

![img](http://img.didispace.com/Fjc9yvgYhnQCrM9-2VaQiGwK0v6M)





##  三、JSR-303实现请求参数校验

###  1.什么是JSR？

* <font color='red'>JSR</font>是Java Specification Requests的缩写，意思是Java 规范提案。

* 是指向<font color='red'>JCP</font>(Java Community Process)提出新增一个标准化技术规范的正式请求。



####  **Bean Validation中内置的constraint**

![img](http://img.didispace.com/Fugzgq1zvxjKur4qdm_N-xV5twMj)



####  **Hibernate Validator附加的constraint**

![img](http://img.didispace.com/FnNRRGx1eWbniJFHQz2m-pUIEWKa)



### 2.快速入门

#### （1）在要校验的字段上添加上`@NotNull`注解，具体如下：

实体类User的属性name添加@NotNull

```java
@NotNull
@ApiModelProperty("用户姓名")
private String name;
```



#### （2）在需要校验的参数实体前添加`@Valid`注解，具体如下：

请求参数user前添加@Valid

```java
@PostMapping("/")
@ApiOperation(value = "创建用户", notes = "根据User对象创建用户")
public String postUser(@Valid @RequestBody User user) {
    users.put(user.getId(), user);
    return "success";
}
```



#### （3）在Postman下验证

并用POST请求访问`localhost:8080/users/`接口，body使用一个空对象，`{}`。

![image-20201228105057478](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228105057478.png)

结果：

```json
{
    "timestamp": "2020-12-28T02:36:27.428+0000",
    "status": 400,
    "error": "Bad Request",
    "errors": [
        {
            "codes": [
                "NotNull.user.age",
                "NotNull.age",
                "NotNull.java.lang.Integer",
                "NotNull"
            ],
            "arguments": [
                {
                    "codes": [
                        "user.age",
                        "age"
                    ],
                    "arguments": null,
                    "defaultMessage": "age",
                    "code": "age"
                }
            ],
            "defaultMessage": "不能为null",
            "objectName": "user",
            "field": "age",
            "rejectedValue": null,
            "bindingFailure": false,
            "code": "NotNull"
        },
        {
            "codes": [
                "NotNull.user.email",
                "NotNull.email",
                "NotNull.java.lang.String",
                "NotNull"
            ],
            "arguments": [
                {
                    "codes": [
                        "user.email",
                        "email"
                    ],
                    "arguments": null,
                    "defaultMessage": "email",
                    "code": "email"
                }
            ],
            "defaultMessage": "不能为null",
            "objectName": "user",
            "field": "email",
            "rejectedValue": null,
            "bindingFailure": false,
            "code": "NotNull"
        },
        {
            "codes": [
                "NotNull.user.name",
                "NotNull.name",
                "NotNull.java.lang.String",
                "NotNull"
            ],
            "arguments": [
                {
                    "codes": [
                        "user.name",
                        "name"
                    ],
                    "arguments": null,
                    "defaultMessage": "name",
                    "code": "name"
                }
            ],
            "defaultMessage": "不能为null",
            "objectName": "user",
            "field": "name",
            "rejectedValue": null,
            "bindingFailure": false,
            "code": "NotNull"
        }
    ],
    "message": "Validation failed for object='user'. Error count: 3",
    "path": "/users/"
}
```

- `timestamp`：请求时间
- `status`：HTTP返回的状态码，这里返回400，即：请求无效、错误的请求，通常参数校验不通过均为400
- `error`：HTTP返回的错误描述，这里对应的就是400状态的错误描述：Bad Request
- `errors`：具体错误原因，是一个数组类型；因为错误校验可能存在多个字段的错误，比如这里因为定义了两个参数不能为`Null`，所以存在两条错误记录信息
- `message`：概要错误消息，返回内容中很容易可以知道，这里的错误原因是对user对象的校验失败，其中错误数量为`2`，而具体的错误信息就定义在上面的`errors`数组中
- `path`：请求路径



### 3.Swagger文档中的体现

<font color='orange'>Swagger</font>共支持以下几个注解：`@NotNull`、`@Max`、`@Min`、`@Size`、`@Pattern`。在实际开发过程中，我们需要分情况来处理，对于Swagger支自动生成的可以利用原生支持来产生，如果有部分字段无法产生，则可以在`@ApiModelProperty`注解的描述中他，添加相应的校验说明，以便于使用方查看。

```java
@Data
@ApiModel(description = "用户实体")
public class User {
    @ApiModelProperty("用户编号")
    private Long id;

    @NotNull
    @Size(min = 2, max = 5)
    @ApiModelProperty("用户姓名")
    private String name;

    @NotNull
    @Max(100)
    @Min(10)
    @ApiModelProperty("用户年龄")
    private Integer age;

    @NotNull
    @Email
    @ApiModelProperty("用户邮箱")
    private String email;

}
```

![img](http://img.didispace.com/FjCx_hTf40k4A5EqtZPsi6wR69xq)



### 4.番外

不需要引入`spring-boot-starter-validation`依赖

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

`spring-boot-starter-validation`依赖主要是为了引入下面这个依赖：

```xml
<dependency> 
    <groupId>org.hibernate.validator</groupId>  
    <artifactId>hibernate-validator</artifactId> 
    <version>6.0.14.Final</version>   
    <scope>compile</scope>
</dependency>
```



## 四、Swagger接口分类与各元素排序问题详解

### 1.接口分组

<font color='orange'>Spring Boot</font>中定义各个接口是以`Controller`作为第一级维度来进行组织的，`Controller`与具体接口之间的关系是<font color='red'>一对多</font>的关系。我们可以将同属一个模块的接口定义在一个`Controller`里。默认情况下，Swagger是以`Controller`为单位，对接口进行分组管理的。这个分组的元素在<font color='orange'>Swagger</font>中称为`Tag`，但是这里的`Tag`与接口的关系并不是一对多的，它支持更丰富的<font color='red'>多对多</font>关系。



### 2.默认分组

在<font color='red'>Application启动类下</font>定义两个`Controller`，分别负责教师管理与学生管理接口



```java
@RestController
@RequestMapping(value = "/teacher")
static class TeacherController {

    @GetMapping("/xxx")
    public String xxx() {
        return "xxx";
    }

}

@RestController
@RequestMapping(value = "/student")
static class StudentController {

    @ApiOperation("获取学生清单")
    @GetMapping("/list")
    public String bbb() {
        return "bbb";
    }

    @ApiOperation("获取教某个学生的老师清单")
    @GetMapping("/his-teachers")
    public String ccc() {
        return "ccc";
    }

    @ApiOperation("创建一个学生")
    @PostMapping("/aaa")
    public String aaa() {
        return "aaa";
    }

}
```



**SwaggerUI界面**

<img src="C:\Users\10618\Pictures\笔记图片\FpBD_IuM7mukpyPg7Pbs77XwJQmD" alt="img" style="zoom:50%;" />





### 3.自定义默认分组的名称

通过`@Api`注解来自定义`Tag`，添加以下注解

<font color='orange'>@Api(tags = "教师管理")</font>

<font color='orange'>@Api(tags = "学生管理")</font>

```java
@Api(tags = "教师管理")
@RestController@RequestMapping(value = "/teacher")
static class TeacherController {    
    // ...}
    
@Api(tags = "学生管理")
@RestController@RequestMapping(value = "/student")
static class StudentController {    
    // ...}
```



**SwaggerUI界面**

<img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228112533961.png" alt="image-20201228112533961" style="zoom:50%;" />

### 4.分组排序

#### （1）tag排序

<font color='red'>Swagger</font>只提供了一个选项，就是按<font color='red'>字母</font>顺序排列。

```properties
swagger.ui-config.tags-sorter=alpha
```



由于原本存在按字母排序的机制在，通过命名中增加数字来帮助排序，可以简单而粗暴的解决分组问题

```java
@Api(tags = {"1-教师管理","3-教学管理"})
@RestController
@RequestMapping(value = "/teacher")
static class TeacherController {
	//...
}

@Api(tags = {"2-学生管理"})
@RestController
@RequestMapping(value = "/student")
static class StudentController {

    @ApiOperation(value = "获取学生清单", tags = "3-教学管理")
    @GetMapping("/list")
    public String bbb() {
        return "bbb";
    }
	//...

}
```



**swagger界面**

<img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228121901266.png" alt="image-20201228121901266" style="zoom:67%;" />



#### （2）接口排序

提供了两个配置项：`alpha`和`method`，分别代表了按字母表排序以及按方法定义顺序排序。当我们不配置的时候，改配置默认为`alpha`。

```properties
swagger.ui-config.operations-sorter=alpha
```

```properties
swagger.ui-config.operations-sorter=method
```



<img src="C:\Users\10618\Pictures\笔记图片\FkVlyiqqmpHMGoU6WlN4uNzEfKh4.jpg" alt="img" style="zoom:67%;" />



#### (3)参数排序

默认情况下，Swagger对Model参数内容的展现也是按字母顺序排列的。

```java
@ApiModelProperty(value = "用户编号", position = 1)
private Long id;

@NotNull
@Size(min = 2, max = 5)
@ApiModelProperty(value = "用户姓名", position = 2)
private String name;

@NotNull
@Max(100)
@Min(10)
@ApiModelProperty(value = "用户年龄", position = 3)
private Integer age;

@NotNull
@Email
@ApiModelProperty(value = "用户邮箱", position = 4)
private String email;
```

![image-20201228123644607](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228123644607.png)



## 五、Swagger静态文档的生成



### 1.Swagger2Markup简介

<font color='orange'>Swagger2Markup</font>是Github上的一个开源项目。该项目主要用来将<font color='orange'>Swagger</font>自动生成的文档转换成几种流行的格式以便于静态部署和使用，比如：<font color='red'>AsciiDoc</font>、<font color='red'>Markdown</font>、<font color='red'>Confluence</font>。



### 2.生成 AsciiDoc 文档

#### （1）通过Java代码来生成

#####  ①编辑`pom.xml`增加需要使用的相关依赖和仓库

本身这个工具主要就临时用一下，所以这里我们把`scope`设置为test，这样这个依赖就不会打包到正常运行环境中去。<scope>test</scope>

```xml
<dependency>
    <groupId>io.github.swagger2markup</groupId>
    <artifactId>swagger2markup</artifactId>
    <version>1.3.3</version>
	<scope>test</scope>
</dependency>
```

```xml
<repositories>
		<repository>
			<snapshots>
				<enabled>false</enabled>
			</snapshots>
			<id>jcenter-releases</id>
			<name>jcenter</name>
			<url>http://jcenter.bintray.com</url>
		</repository>
	</repositories>
```



#####  ②编写一个单元测试用例来生成执行生成文档的代码

```java
@RunWith(SpringRunner.class)
//DEFINED_PORT ： 加载一个EmbeddedWebApplicationContext并提供一个真正的servlet环境。嵌入式servlet容器启动并监听定义的端口（即从application.properties或默认端口8080）。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DemoApplicationTests {
    @Test
    public void generateAsciiDocs() throws Exception {

        URL remoteSwaggerFile = new URL("http://localhost:8080/v2/api-docs");
        Path outputDirectory = Paths.get("src/docs/asciidoc/generated");

        //    输出Ascii格式
        Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder()
                .withMarkupLanguage(MarkupLanguage.ASCIIDOC)
                .build();


        Swagger2MarkupConverter.from(remoteSwaggerFile)
                .withConfig(config)
                .build()
                .toFolder(outputDirectory);
    }
}
```



使用**@SpringBootTest**的**webEnvironment**属性来进一步优化测试的运行方式：

- **MOCK** ： 加载一个WebApplicationContext并提供一个模拟servlet环境。嵌入式servlet容器在使用此注释时不会启动。如果servlet API不在你的类路径上，这个模式将透明地回退到创建一个常规的非web应用程序上下文。可以与@AutoConfigureMockMvc结合使用，用于基于MockMvc的应用程序测试。
- **RANDOM_PORT ：** 加载一个EmbeddedWebApplicationContext并提供一个真正的servlet环境。嵌入式servlet容器启动并在随机端口上侦听。
- **DEFINED_PORT ：** 加载一个EmbeddedWebApplicationContext并提供一个真正的servlet环境。嵌入式servlet容器启动并监听定义的端口（即从application.properties或默认端口8080）。
- **NONE ：** 使用SpringApplication加载ApplicationContext，但不提供任何servlet环境（模拟或其他）。



- `MarkupLanguage.ASCIIDOC`：指定了要输出的最终格式。除了`ASCIIDOC`之外，还有`MARKDOWN`和`CONFLUENCE_MARKUP`。
- `from(remoteSwaggerFile`：指定了生成静态部署文档的源头配置，可以是这样的URL形式，也可以是符合Swagger规范的String类型或者从文件中读取的流。如果是对当前使用的Swagger项目，我们通过使用访问本地Swagger接口的方式，如果是从外部获取的Swagger文档配置文件，就可以通过字符串或读文件的方式
- `toFolder(outputDirectory)`：指定最终生成文件的具体*目录*位置



执行结果：

<img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228133152753.png" alt="image-20201228133152753" style="zoom:67%;" />



#### (2)通过 Maven 插件来生成

```xml
 <plugin>
                <groupId>io.github.swagger2markup</groupId>
                <artifactId>swagger2markup-maven-plugin</artifactId>
                <version>1.3.3</version>
                <configuration>
<!--                    URL remoteSwaggerFile = new URL("http://localhost:8080/v2/api-docs");-->
                    <swaggerInput>http://localhost:8080/v2/api-docs</swaggerInput>
<!--                    Path outputDirectory = Paths.get("src/docs/asciidoc/generated");-->
                    <outputDir>src/docs/asciidoc/generated-by-plugin</outputDir>
                    <config>
                        <swagger2markup.markupLanguage>ASCIIDOC</swagger2markup.markupLanguage>
                    </config>
                </configuration>
            </plugin>
```

在使用插件生成前，需要先启动应用。然后执行插件，就可以在`src/docs/asciidoc/generated-by-plugin`目录下看到也生成了上面一样的adoc文件了。

<font color='red'>运行失败</font>





### 3.生成HTML

添加插件

```xml
<plugin>
    <groupId>org.asciidoctor</groupId>
    <artifactId>asciidoctor-maven-plugin</artifactId>
    <version>1.5.6</version>
    <configuration>
        <!--输入目录-->
        <sourceDirectory>src/docs/asciidoc/generated</sourceDirectory>
        <!--输出目录-->
        <outputDirectory>src/docs/asciidoc/html</outputDirectory>
        <backend>html</backend>
        <sourceHighlighter>coderay</sourceHighlighter>
        <attributes>
            <toc>left</toc>
        </attributes>
    </configuration>
</plugin>
```



* 执行该插件的`asciidoctor:process-asciidoc`命令之后

  <img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228140012706.png" alt="image-20201228140012706" style="zoom:67%;" />

* 在`src/docs/asciidoc/html`目录下生成最终可用的静态部署HTML了

  <img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228135956905.png" alt="image-20201228135956905" style="zoom:50%;" />

<font color='red'>注意：</font>需要首先生成AsciiDoc文档



###  4.生成 Markdown 和 Confluence 文档

通过Java代码来生成：只需要修改`withMarkupLanguage`属性来指定不同的格式以及`toFolder`属性为结果指定不同的输出目录。

* <font color='orange'>Markdown</font>

```java
Path outputDirectory = Paths.get("src/docs/markdown/generated");

Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
```

* <font color='orange'>Confluence</font>

```java
Path outputDirectory = Paths.get("src/docs/confluence/generated");

Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder()
                .withMarkupLanguage(MarkupLanguage.CONFLUENCE_MARKUP)
                .build();
```



## 六、找回启动日志中的请求路径列表

日志接口信息是由`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping`类在启动的时候，通过扫描Spring MVC的`@Controller`、`@RequestMapping`等注解去发现应用提供的所有接口信息。然后在日志中打印，以方便开发者排查关于接口相关的启动是否正确。

这些日志的打印级别做了调整：从原来的`INFO`调整为`TRACE`。所以，当我们希望在应用启动的时候打印这些信息的话，只需要在配置文件增增加对`RequestMappingHandlerMapping`类的打印级别设置即可，比如在`application.properties`中增加下面这行配置：

```properties
logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping=trace
```

![image-20201228143126128](C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228143126128.png)



## 七、使用SpringFox 3生成Swagger文档（建议使用，代替二的方法）



### 1.导入依赖

<font color='red'> SpringFox 3.0.0，Swagger包</font>

```xml
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```



<font color='red'>验证规则包</font>

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```



### 2.应用主类增加注解`@EnableOpenApi`

```java
@EnableOpenApi
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
```



### 3.接口事例

<font color='orange'>UserController</font>

```java
@Api(tags = "用户管理")
@RestController
@RequestMapping(value = "/users")     // 通过这里配置使下面的映射都在/users下
public class UserController {

    // 创建线程安全的Map，模拟users信息的存储
    static Map<Long, User> users = Collections.synchronizedMap(new HashMap<>());

    @GetMapping("/")
    @ApiOperation(value = "获取用户列表")
    public List<User> getUserList() {
        List<User> r = new ArrayList<>(users.values());
        return r;
    }

    @PostMapping("/")
    @ApiOperation(value = "创建用户", notes = "根据User对象创建用户")
    public String postUser(@Valid @RequestBody User user) {
        users.put(user.getId(), user);
        return "success";
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "获取用户详细信息", notes = "根据url的id来获取用户详细信息")
    public User getUser(@PathVariable Long id) {
        return users.get(id);
    }

    @PutMapping("/{id}")
    @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "用户编号", required = true, example = "1")
    @ApiOperation(value = "更新用户详细信息", notes = "根据url的id来指定更新对象，并根据传过来的user信息来更新用户详细信息")
    public String putUser(@PathVariable Long id, @RequestBody User user) {
        User u = users.get(id);
        u.setName(user.getName());
        u.setAge(user.getAge());
        users.put(id, u);
        return "success";
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除用户", notes = "根据url的id来指定删除对象")
    public String deleteUser(@PathVariable Long id) {
        users.remove(id);
        return "success";
    }

}
```



<font color='orange'>User</font>

```java
@Data
@ApiModel(description = "用户实体")
public class User {

    @ApiModelProperty(value = "用户编号", position = 1)
    private Long id;

    @NotNull
    @Size(min = 2, max = 5)
    @ApiModelProperty(value = "用户姓名", position = 2)
    private String name;

    @NotNull
    @Max(100)
    @Min(10)
    @ApiModelProperty(value = "用户年龄", position = 3)
    private Integer age;

    @NotNull
    @Email
    @ApiModelProperty(value = "用户邮箱", position = 4)
    private String email;

}
```



### 4.启动应用！访问swagger页面

`http://localhost:8080/swagger-ui/index.html`

<img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228152351174.png" alt="image-20201228152351174" style="zoom:67%;" />



<font color='red'>注意：</font>

1. 移除了原来默认的swagger页面路径：`http://host/context-path/swagger-ui.html`，新增了两个可访问路径：`http://host/context-path/swagger-ui/index.html`和`http://host/context-path/swagger-ui/`
2. 通过调整日志级别，还可以看到新版本的swagger文档接口也有新增，除了以前老版本的文档接口`/v2/api-docs`之外，还多了一个新版本的`/v3/api-docs`接口。



<img src="C:\Users\10618\AppData\Roaming\Typora\typora-user-images\image-20201228152513342.png" alt="image-20201228152513342" />



### 5.优点

与Swagger2相比

* 减少了配置
* 导包数量少
* 丰富 open API 3.0 规范



### 代码

https://github.com/yezhinao/SpringBoot-Learn/tree/main/SpringBoot-Api/SpringFox