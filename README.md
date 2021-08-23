# custom-sse

> Email：jorringhsiao_work@163.com 

> Github：https://github.com/JorringHsiao  

> QQ：3129600569

[![Travis](https://img.shields.io/badge/language-Java%20&%20JavaScript-yellow.svg)](https://github.com/JorringHsiao)

> 🔑 关键词：SSE, 服务端推送, 实时推送, 进度条

### 📣 本项目的目的
+ 以进度条的功能需求为例，引出 SSE 技术
+ 了解 & 手撕 SSE （EventSource）
+ 实现自定义的 SSE （基于 XMLHttpRequest）
+ 以现有的工具库为基础实现自定义的 SSE（基于 axios）

### 👀 喂！先看这里！
在座的各位大哥，要会点 Spring Boot（好像不会也行，不太影响，只是demo用的是 Spring，后端部分没办法兼顾到那么多不同语言的同学们哈），
还要会一点点前端（主要是 Javascript ）。  

如果都不熟悉的话也没关系，小弟我争取说明白，让你学废！😁

本项目 demo 是基于 Spring Boot 写的，前端同学要看代码的话，在目录： **src/main/resource/static**

demo 页面地址： <http://127.0.0.1:8080/index.html>

### 🎤 情景还原
如果项目中有个接口处理起来需要一点时间，传统的菊花🌼转圈圈loading肯定不行的，你老板怎么样都会让你弄个进度条吧？是吧！是吧！是吧！

#### # CASE 1：
后端同学不想弄这玩意，那么前端同学只能自己弄个假进度条了（😂 xswl），响应前各种随机，把进度条弄得很真的样子，响应后设置为100%

#### # CASE 2:
后端同学大展拳脚，原接口改为异步，丢线程池中执行；用全局变量记录处理进度；再新增一个查询进度的接口。
前端同学轮询查询进度，这样就可以弄真实的进度条了。

#### # CASE 3:
如果后端服务是分布式/微服务架构，一个服务有多个实例的情况下，那么就需要将全局变量存到数据库/Redis之类的地方，
否则LB到其它的服务实例就查不到进度了。

#### # CASE 4:
有些年轻的后端小伙子，精力旺盛，可能会将接口改成WebSocket，服务端主动推送进度给前端，这样就不需要前端去轮询了，也不需要“全局变量”保存进度了。

#### # CASE 5:
他来了！他来了！主角登场了！没错，就是 SSE。（这是啥？继续往下look look吧。）

## 📚 Server-Sent Events
**Server-Sent Events (SSE)** 是一种服务器推送技术，通常用于服务端向浏览器客户端发送消息更新或连续的数据流。
浏览器客户端通过一个名为 **EventSource** 的JavaScript API请求特定的URL(Response Content-Type: text/event-stream)来接收事件流。
**EventSource** 是W3C标准化的HTML5的一部分

> 本文定义如下名词，  
> **标准SSE**：表示后端响应text/event-stream格式内容，前端JavaScript使用 EventSource API

> 那么“标准SSE”就不介绍啦，下面几篇文章自己去看看吧，（全栈的同学应该）几分钟就能看完看懂😂。
#### 介绍 & 定义 & 前端
+ <https://en.wikipedia.org/wiki/Server-sent_events>
+ <https://developer.mozilla.org/zh-CN/docs/Web/API/EventSource>
+ <https://www.ruanyifeng.com/blog/2017/05/server-sent_events.html>
+ <https://www.runoob.com/html/html5-serversentevents.html>
#### 后端
+ Spring MVC： <https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-sse>
+ 其它语言的这里找找对应的封装/实现： <https://en.wikipedia.org/wiki/Server-sent_events#Libraries>

> 当你学废SSE之后，我们来个总结吧...

SSE 到底有什么用呢？  
以上面说的进度条功能需求为例，可以在同一个请求连接中，服务端返回（推送）多个事件给浏览器客户端，根据业务需求在事件中携带必要的数据（消息），
前端根据不断接收到的事件消息实现进度条，或展示后端处理中的任务的实时状态。

当然也可以做在线聊天之类的功能。发挥自己的想象力吧！

#### # 只支持 **GET** 请求
这意味着不能传很多很多的参数，因为URL长度有限制。
当然，如果一定要传很大的参数也不是没办法解决，在服务端改一下URL长度的限制咯。
而上传文件处理长任务+进度条的功能需求就一定没办法实现了，因为 **GET** 请求没有请求体。

#### # 不能设置请求头
如果后端系统依赖请求头的一些Token参数进行鉴权，这也GG了

#### # 一直循环请求
只要EventSource对象创建之后：
+ 每次请求结束之后，就会自动重新请求
+ 网络连接断开后，也会自动重连（重新发起请求）

这个可能就是所谓的WebSocket不会自动重连，而SSE能自动重连的的特点，但不知道这样设计的意图是什么。
如果是单纯的客户端浏览器通过EventSource不断接收来自服务端的消息的场景，这可能是不错的特点。
但对于“任务型”的接口或需要“幂等”的这种不需要自动重新连接（请求）的接口，改造成“标准SSE”恐怕不合适。

EventSource没有提供连接结束后的回调，也就没办法在第一次请求结束后手动关闭；也没有提供连接前的回调，因此也没办法做到第二次连接前手动关闭。

如果与后端协商确定以特定的事件消息内容作为关闭EventSource的信号，如果
+ 连接断开、重连；
+ 服务端在结束之前就出现了异常，并且异常没有被catch到；

服务端就无法发送“关闭”事件消息，导致前端EventSource将无法关闭，一直请求。

## 💪 手撕 SSE
### “推”什么
服务端设置HTTP响应头。
```
Content-Type: text/event-stream
```
响应体内容必须是UTF-8编码的文本，数据格式如下：
+ 每一个事件消息之间用两个换行 `\n\n` 分隔
+ 每个事件消息由多个键值对组成
+ 键和值用冒号 `:` 分隔
+ 键，即字段，有四个可取值，分别为：`data`, `event`, `id`, `retry`   
（每个字段的含义与用法自己去查吧，这里不解释了哈）
+ 每个键值对之间用一个换行 `\n` 分隔
+ 此外，还可以以冒号 `:` 作为行的开头，该行表示注释

> 当然还有其它的规则，但不影响我们去理解原理，这里就不再啰嗦了

客户端（EventSource）读取响应流的内容，并根据这些数据格式（规则）进行切割，就可以得到一个个的“事件”对象了。

### 怎么“推”
抛开各种语言、开发框架的区别，一个HTTP服务器，在响应阶段无非是以下几个操作：
+ write - 在“流”中写数据
+ flush - 对“流”进行刷新，一般是将缓存中的内容发送到客户端（可能有些语言的“流”在写数据的时候就已经flush了）
+ close - 关闭“流”

眼睛犀利的同学可能已经看出来了，就是那个 `flush`，先记住它，等会儿再叫它出来。

“推”不就是将“事件”的文本数据包写（write）到“流”，并且刷（flush）一下它么！搞定！So Easy！妈妈再也不用担心了！

### 怎么“监听”
EventSource的源码没看过，我不知道它怎么实现的。
> 主要是不知道在哪里能看到这玩意的源码，知道的同学吱一声，悄咪咪告诉我吧！

😏 接下来就是我的solo时间了。

前端同学肯定知道 `XMLHttpRequest` 这玩意吧，前端 AJAX 请求都靠它。
对于 AJAX 请求，可能现在都用封装好的工具库了吧，如 jQuery、 **axios（记住它，等会儿还要叫它出来的）**。

下面是原生 `XMLHttpRequest` 的简单示例：
```javascript
const xhr = new XMLHttpRequest()
xhr.open('GET', '/sse?param=xxx', true)
xhr.onreadystatechange = function() {
    // 请求已结束，响应内容下载完成，并且HTTP响应码是200时
    if (xhr.readyState === 4 && xhr.status === 200) {
        res = JSON.parse(xhr.responseText)
        // do something... 干点啥
    }   
}
xhr.send()
``` 

重点就是 `XMLHttpRequest` 中的 `onreadystatechange` 和 `readyState` 这两个属性。每当 `readyState` 的值发生改变时，就会执行回调 `onreadystatechange`。

`XMLHttpRequest.readyState` 属性返回一个 `XMLHttpRequest` 当前所处的状态。一个 XHR 总是处于下列状态中的一个：

| 值 | 状态 | 描述 |
| --- | --- | --- |
| 0 | UNSENT | 代理被创建，但尚未调用 open() 方法。 |
| 1 | OPENED | open() 方法已经被调用。 |
| 2 | HEADERS_RECEIVED | send() 方法已经被调用，并且头部和状态已经可获得。 |
| 3 | LOADING | 下载中； responseText 属性已经包含部分数据。 |
| 4 | DONE | 下载操作已完成。 |

> 📢 喂！flush 出来啦！

XMLHttpRequest.readyState = 3 的时候比较特殊，表示正在接收服务端返回的数据。 
 
**每当服务端刷（flush）一下，回调方法 XMLHttpRequest.onreadystatechange 就会被执行一次。**

> ✌ 这不就破案了吗！！！

服务端（后端），每向“流”中 write + flush 一个“事件”；  
浏览器（前端），在 `XMLHttpRequest.onreadystatechange` 中对响应内容 `XMLHttpRequest.responseText` 进行解析，取出每一个“事件”对象，
并根据“事件名”匹配 listener进行回调，这不就实现了SSE了吗！！！

🏃 走起！！！

## 🤘 实现自定义的 SSE
先定义数据格式：
+ 事件之间用两个换行 `\n\n` 分隔
+ 每个事件固定由两个部分组成，分别是“事件名”和“数据”
+ 两个部分之间用一个换行分隔 `\n`
+ “事件名”不能包含换行符
+ “数据”是对象类型的json字符串

（示例）响应流中的内容如下：
```
message
{"message": "hello world 1"}

success
{"message": "1", "date": "2021-08-22 21:38:00"}

message
{"message": "hello world 2"}
```

大概思路如下
```javascript
const xhr = new XMLHttpRequest()
xhr.open('GET', '/sse?param=xxx', true)
// 用于保存上一次切到的位置
let lastIndex = 0
// 事件监听器
const listeners = {
    message: e => {
        console.log('on message event', e)
    },
    success: e => {
        console.log('on success event', e)
    }
}
xhr.onreadystatechange = function() {
    // 等于3时，服务端每flush一次，xhr都触发一次回调
    if (xhr.readyState >= 3) {
        const text = xhr.responseText
        let nextIndex = -1
        while (true) {
            // 从 lastIndex 上一次的位置继续查找
            nextIndex = text.indexOf('\n\n', lastIndex)
            if (nextIndex < 0) {
                break
            }
            // 将本次找到的“事件”字符串“块”切出来
            const chunk = text.substring(lastIndex, nextIndex);
            // 记录本次切的位置，2 就是两个换行符的长度
            lastIndex = nextIndex + 2
            const parts = chunk.split('\n');
            // 匹配到监听器并执行
            if (listeners[parts[0]]) {
                listeners[parts[0]]({
                    // 第一部分是事件名
                    name: parts[0],
                    // 第二部分是对象类型的json字符串
                    data: parts[1] ? JSON.parse(parts[1]) : {}
                })
            }
        }
    }   
}
xhr.send()
```

是不是很简单，这就已经实现了自定义的SSE的客户端部分（服务端就看demo代码吧，不再讲啦）。继续下一part！

> 📢 喂！axios 出来啦！

使用 `axios` 时，为了做统一的鉴权处理，会在拦截器上设置请求头、Token 之类的。SSE也是基于HTTP实现的，请求到后端同样需要鉴权。
因此需要在 `axios` 的基础上实现自定义的SSE，否则鉴权部分的代码需要维护两份（如果用xhr实现SSE的话）。

看了一下 `axios` 的源码，总结一下，大概就是一个“接口”或“框架”的东西吧，在浏览器中适配 `XMLHttpRequest`，在 Node 中适配 `http`。  

`axios` 已经对 `XMLHttpRequest` 进行了封装，没办法以 `XMLHttpRequest.onreadystatechange` 作为切入点，实现自己的SSE。

> 还没结束呢！有希望！

再继续看了下的源码，发现了一个看起来挺有希望的参数 `onDownloadProgress`。  

> 为什么说看起来挺有希望的呢？看名字，第一感觉，很明显是要来处理“下载文件”的，应该是一个要来弄下载进度条的回调吧。  
>
> 根据经验，要实现下载进度，首先需要知道文件的大小，对应响应头的 `Content-Length`，而当前下载了多少，那也只能从响应体得到它的长度吧？  
>
> 如果是我所想的这样，这不就撞个正着了嘛！“响应体”不正是我所需要的吗！！！

下面是我“相中”部分的源码，将 `onDownloadProgress` 回调方法设置为 XMLHttpRequest 的 progress 事件的监听器。
```javascript
module.exports = function xhrAdapter(config) {
    return new Promise(function dispatchXhrRequest(resolve, reject) {
        var request = new XMLHttpRequest();
        // more ...

        // 
        if (typeof config.onDownloadProgress === 'function') {
            request.addEventListener('progress', config.onDownloadProgress);
        }

        // more ...
    })
}
```

XMLHttpRequest 的 progress 事件监听器是在 `XMLHttpRequest.readyState` = `3` 时被回调的方法，也就是说，下面两种写法是一样效果的。
```javascript
// case 1
xhr.addEventListener('progress', e => {
    console.log(e.currentTarget)
})

// case 2
xhr.onreadystatechange = function() {
    if (xhr.readyState === 3) {
        console.log(xhr)
    }
}
```

`XMLHttpRequest` 有 `abort`, `error`, `load`, `loadend`, `loadstart`, `progress`, `timeout` 这些事件，
事件监听器（回调）会传入一个 `ProgressEvent` 事件对象, 而 `ProgressEvent.currentTarget` 就是 `XMLHttpRequest` 实例对象，
那么就可以获取到 `XMLHttpRequest.responseText` 了。

> 🙊 哇噢！  
> 🙊 哇噢！  
> 🙊 哇噢！  

赶紧clone下来玩玩吧！

> 📍 我的疑问？？？

因为没看过 `EventSource` 的源码，不知道是如何处理 “响应流” 的？  

不知道有没有哪位同学发现，通过 `XMLHttpRequest` 实现 SSE 是存在 BUG 的，因为 `XMLHttpRequest.responseText` 保存着当前请求的所有响应内容。  
如果连接不断开，请求不结束，服务端一直推送事件消息，那么 `XMLHttpRequest.responseText` 将会越来越大。因此基于 `XMLHttpRequest` 实现的 SSE
不适用于 “无限流”，仅适用于有限的、不太大的响应内容（事件消息）。有解决方案的前端大佬们~吱~一声呗！
