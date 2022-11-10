# TekiXquic

# 简介
tekixquic 是基于 Xquic+libev 进行二次封装的Android sdk库，为了方便理解和使用，沿用了okhttp的封装方式，同时沿用了短链接和长链接的思维。
通过tekixquic 你可以在客户端快速的验证和使用xquic基于udp传输，整个sdk，大小在1M左右，轻量便捷,并且部分支持cdn厂商中转

# 环境
工具

（1）Android studio

（2）ndk （本人用21）

使用到的第三方sdk

（1）xquic （https://github.com/alibaba/xquic)

（2）libev 4.33版本


**注意：tekixquic跟其他开源 server 互通测试**
|  server   | 互通结果  | 备注  |
|  ----  | ----  |----  |
| quic-go  | https://zhuanlan.zhihu.com/p/502352169 |升级sdk到1.0.2版本或者关闭accpetToken可以正常通讯 |
| 阿里云  | 正常通讯 | |
| cloudflare  | 正常通讯 | |
| 其他服务  | 注意服务端支持的alpn版本跟Proto版本号然后调用对应的api设置 | |

# sdk 接入

第一步：引入maven 在allprojects中引入

```
 mavenCentral()
 
 // snapshot 版本需要
 maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }
```

第二步：导入sdk</br>
```
implementation 'io.github.yangqingyuan:teki-quic:1.0.7'
```

# 版本更新
|  version   | 更新内容  | 时间  |
|  ----  | ----  |----  |
| 1.0.7   | 1.代码调整和优化 </br> 2.完善场景使用 </br> 3.增加超时机制和网络监听 </br> 4.修复其他问题 </br> |2022/11/12|
| 1.0.6（正式使用）  | 1.支持stream复用 </br> 2.完善webSocket使用 </br> 3.修复其他问题 </br> |2022/10/17|
| 1.0.5  | 1.升级xquic到1.2.0 </br> 2.修复复用断网重连问题 |2022/09/17|
| 1.0.4-SNAPSHOT  | 1.hq 支持0Rtt </br> 2.支持x86 </br> 3.升级xquic到xquic-1.1.0-stable |2022/08/23|
| 1.0.3-SNAPSHOT  | 1.支持Hq协议，支持设置alpn</br> 2.优化java->jni 传输性能，支持传输byte </br> 3.其他优化 |2022/06/30|
| 1.0.2-SNAPSHOT  | 1.支持链接复用</br> 2.升级xquic到v1.1.0-beta.2 </br> 3.修复若干问题</br> 4. 优化逻辑 </br> 5. 支持DNS替换 </br> | 2022/06/15 |
| 1.0.1  | 1.支持长链接</br> 2.支持生命周期感知</br> 3.支持取消</br> 4. 其他优化等 </br>| 2022/05/07 |
| 1.0.0  | 支持短链接 |2022/04/21|

# 使用方式
## 短链接
### Get 请求
 ```
  val xquicClient = XquicClient.Builder()
        .connectTimeOut(13)
        .setReadTimeOut(30)
        .ccType(CCType.BBR) //可选，拥塞算法
        .reuse(true)//是否长链接复用，注意要看后端是否支持，能复用，强烈建议复用，在性能上会有非常大的提升，例如：阿里云这些是支持的，默认false
        .setCryptoFlag(CryptoFlag.WITHOUT_CRYPTO)//是否加密，默认加密
        //.dns(XDns.SYSTEM)
        //.setAlpnType(AlpnType.ALPN_HQ) //支持协议切换，默认H3
        //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//支持协议版本号设置 ，默认XQC_VERSION_V1
        .build()
    val xRequest = XRequest.Builder()
        .url("https://192.168.10.245:8443")
        .life(this)//可选，如果传递这个参数，内部可以根据activity的生命周期取消没有执行的任务或者正在执行的任务，例如超时
        .addHeader("testA", "testA")// 可选，携带自定义头信息
        .get() //Default
        .tag("tag")//可选
        .build()

    val startTime = System.currentTimeMillis()
    xquicClient.newCall(xRequest).enqueue(object : XCallBack {
        override fun onFailure(call: XCall, exception: Exception) {
            XLogUtils.error(exception.message)
        }

        override fun onResponse(call: XCall, xResponse: XResponse) {
            XLogUtils.info(
                " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody.body()}"
            )
        }
    })
```
### POST 请求
```
val xquicClient = XquicClient.Builder()
    .connectTimeOut(13)
    .setReadTimeOut(30)
    .ccType(CCType.BBR) //可选，拥塞算法
    .reuse(true)//是否链接复用，注意要看后端是否支持，能复用，强烈建议复用，在性能上会有非常大的提升，例如：阿里云这些是支持的，默认false
    .setCryptoFlag(CryptoFlag.WITHOUT_CRYPTO)//是否加密，默认加密
    //.dns(XDns.SYSTEM)
    //.setAlpnType(AlpnType.ALPN_HQ) //支持协议切换，默认H3
    //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//支持协议版本号设置 ，默认XQC_VERSION_V1
    .build()

val xRequestBody =XRequestBody.create(XMediaType.parse(XMediaType.MEDIA_TYPE_TEXT), "test")
val xRequest = XRequest.Builder()
    .url("https://192.168.10.245:8443")
    .life(this)//可选，如果传递这个参数，内部可以根据activity的生命周期取消没有执行的任务或者正在执行的任务，例如超时
    .addHeader("testA", "testA")// 可选，携带自定义头信息
    .post(xRequestBody)
    .tag("tag")//可选
    .build()

val startTime = System.currentTimeMillis()
xquicClient.newCall(xRequest).enqueue(object : XCallBack {
    override fun onFailure(call: XCall, exception: Exception) {
        XLogUtils.error(exception.message)
    }

    override fun onResponse(call: XCall, xResponse: XResponse) {
        XLogUtils.info(
            " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody.body()}"
        )
    }
})

```

## 长链接

```
 /**
  * 针对请求响应的场景，建议不直接使用该函数，使用XquicClient短链接，并将复用打开，内部也是使用了XRealWebSocket来实现
  */
val xquicClient = XquicClient.Builder()
    .connectTimeOut(13)
    .setReadTimeOut(30)
    .ccType(CCType.BBR) //可选，拥塞算法
    .pingInterval(5000)//
    .setCryptoFlag(CryptoFlag.WITHOUT_CRYPTO)//是否加密，默认加密
    //.dns(XDns.SYSTEM)
    //.setAlpnType(AlpnType.ALPN_HQ) //支持协议切换，默认H3
    //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//支持协议版本号设置 ，默认XQC_VERSION_V1
    .build()

 val xRequest = XRequest.Builder()
            .url(url)//127.0.0.1:6121 //192.168.10.245:8443
            .addHeader("testA", "testA")
            .build()

        webSocket = xquicClient.newWebSocket(xRequest, object : XWebSocketListener {
            override fun onOpen(webSocket: XWebSocket, response: XResponse) {
                //握手成功后进行回调
            }

            override fun onMessage(webSocket: XWebSocket, response: XResponse) {
                //接收到消息后进行回调
                //var body = response.xResponseBody.body() or
                var bodyByteArray = response.xResponseBody.byteBody()
            }

            override fun onClosed(webSocket: XWebSocket, code: Int, reason: String?) {
                //链接关闭后回调
            }
            
            override fun onFailure(
                webSocket: XWebSocket,
                exception: Throwable,
                response: XResponse
            ) {
                exception.printStackTrace()
                XLogUtils.error(exception.message)
            }
        })

```

## log check
可以过滤tag lzXquic 会打印所有跟tekixquic相关的log，比如下图

![image](https://user-images.githubusercontent.com/6867757/162715655-ef6f864a-1f83-4ae8-bad5-1691acfb7f67.png)



# 帮助文档
|  说明 | 链接 |
|  ----  |----  |
| 关于跟quic-go互通调试| https://zhuanlan.zhihu.com/p/502352169|
| tekixquic跟Okhttp 传输性能测试| https://zhuanlan.zhihu.com/p/556931821|

# API说明
## XquicClient.Builder
| 参数说明 | 用处 | 备注 |
|  ----  |----  |----  |
|  readTimeout  | 链接超时，单位秒，默认30s |  |
|  connectTimeOut  | 读超时，单位秒，默认30s | 注意：readTimeout时间包含connectTimeOut |
|  pingInterval  | ping的时间间隔，单位毫秒，如果大于0，则发送心跳 |  |
|  ccType  | 拥塞算法：BBR/CUBIC/RENO， 默认CUBIC |  |
|  protoVersion  | 协议版本号 ，默认XQC_VERSION_V1 | 建议：可以设置XQC_VERSION_MAX，表示都支持，可以避免后面协议升级后新旧版本兼容问题 |
|  dns  | 自定义dns解析，例如：可以提前将域名解析成ip后再传递到底层 | 目前没有真正用起来 |
|  reuse  | 长链接复用：默认false，复用后相同域名的请求会共用已有的连接 |  |
|  xRttInfoListener  | token跟session返回接口，可以本地化，用于0Rtt，sdk内部是内存缓存，并且跟随xquicClick生命周期 |  |
|  alpnType  | 应用层协议类型，HQ/H3，默认H3 | HQ（http 0.9 透传） |
|  setFinishFlag  | 是否结束流，只对HQ，webSocket场景有效，每次发送数据后不会关闭stream，下次发送会继续使用已有的stream，以达到stream复用 | 跟H3不同，H3每次请求都会创建新的stream |
|  addPingListener  | 应用层ping，增加自定义的发送ping内容，只对webSocket 场景有效，长连接情况下，默认已经打开内部心跳，15秒 | 注意：内部心跳跟当前这个心跳不是同一个，内部心跳是xquic内部自己维护 |
|  pingInterval  | 设置ping的时间间隔，应用层有默认实现，默认发送内容为test，如果设置小于等于0，为不发送应用层ping |  |
|  setCryptoFlag  | 内容是否加密，默认加密 |  |

## XWebSocket
| 函数 | 作用 | 备注 |
|  ----  |----  |----  |
|  fun send(text: String): Boolean  |发送文本  |  |
|  fun send(text: String, tag: String): Boolean  |可以携带tag的方式，接受到数据返回,可以用于区分请求,每一个发送会转化成一个请求，跟短链接唯一的区别是共用一个链接  |  |
|  fun send(byteArray: ByteArray): Boolean |发送byte数组  |  |
|  fun send(message: XRealWebSocket.Message): Boolean  |发送消息，不管byteArray/string，内部都是统一转成message  |  |
|  fun cancel()  |跟close的区别是cancel会在onFailed中返回  |  |
|  fun close(code: Int, reason: String?)  | 跟cancel的区别是close会在onClose，将传递的参数逐一返回，并且底层错误也是在该函数返回  |  |
|  fun isClose(): Boolean  | 判断是否已经关闭，连接关闭后，需要重新连接才才可以继续发送 |  |

# 其他
有任何问题，欢迎留言，有兴趣的同学可以一同完善tekixquic！，加微请备注：tekiXquic</br>
<img src="https://user-images.githubusercontent.com/6867757/162711742-7cfd5e4b-54d8-4c4f-b80e-4d9c9af34ba5.png" width="150px" height="150px" />
