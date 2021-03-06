# TekiXquic

# 简介
tekixquic 是基于 Xquic+libev 进行二次封装的Android sdk库，为了方便理解和使用，沿用了okhttp的封装方式，同时沿用了短链接和长链接的思维。
通过tekixquic 你可以在客户端快速的验证和使用xquic基于udp传输，整个sdk，大小在1M左右，轻量便捷,并且部分支持cdn厂商中转

# 环境
工具

（1）Android studio

（2）ndk （本人用21）

使用到的第三方sdk

（1）xquic （https://github.com/alibaba/xquic) 目前用的是最新的v1.1.0-beta.2版本

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
implementation 'io.github.yangqingyuan:teki-quic:1.0.3-SNAPSHOT'
```

# 版本更新
|  version   | 更新内容  | 时间  |
|  ----  | ----  |----  |
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
        .reuse(true)//是否链接复用，注意要看后端是否支持，能复用，强烈建议复用，在性能上会有非常大的提升，例如：阿里云这些是支持的，默认false
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
val xquicClient = XquicClient.Builder()
    .connectTimeOut(13)
    .setReadTimeOut(30)
    .ccType(CCType.BBR) //可选，拥塞算法
    .pingInterval(5000)//
    //.dns(XDns.SYSTEM)
    //.setAlpnType(AlpnType.ALPN_HQ) //支持协议切换，默认H3
    //.setProtoVersion(ProtoVersion.XQC_IDRAFT_VER_29)//支持协议版本号设置 ，默认XQC_VERSION_V1
    .build()

 val xRequest = XRequest.Builder()
            .url(url)//127.0.0.1:6121 //192.168.10.245:8443
            .addHeader("testA", "testA")
            .addPingListener(object : XPingListener {//可选
                override fun ping(): String {
                    return "ping data"
                }

                override fun pong(data: String) {
                    XLogUtils.info("data=$data")
                }

            })
            .build()

        webSocket = xquicClient.newWebSocket(xRequest, object : XWebSocketListener {
            override fun onOpen(webSocket: XWebSocket, response: XResponse) {

            }

            override fun onMessage(webSocket: XWebSocket, data: ByteArray) {

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



# 架构说明



## 工程结构说明
包说明：

impl->包：逻辑实现类，里面封装了线程池和xquic的底层调用等

native->包：xquic底层JNI实现和回调/参数等

关键类说明：

XquicClient->类： 短链接API入口

XAsyncCall->类：真正的执行逻辑类

XQuicShortNative->类：JNI接口


# 其他
有任何问题，欢迎留言，有兴趣的同学可以一同完善tekixquic！</br>
<img src="https://user-images.githubusercontent.com/6867757/162711742-7cfd5e4b-54d8-4c4f-b80e-4d9c9af34ba5.png" width="150px" height="150px" />
