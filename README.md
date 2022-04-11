# TekiXquic

# 简介
tekixquic 是基于 Xquic 进行二次封装的Android sdk库，为了方便理解和使用，沿用了okhttp的封装方式模式，同时沿用了短链接和长链接的思维。


# 环境
工具

（1）Android studio

（2）ndk （本人用21）

使用到的第三方sdk

（1）xquic （https://github.com/alibaba/xquic) 目前用的是最新的1.0.1版本

（2）libev 4.33版本

# sdk 接入

第一步：引入maven 在allprojects中引入（目前是snapshot版本所有暂时需要）

maven { url "https://s01.oss.sonatype.org/content/repositories/snapshots/" }

第二步：导入sdk

implementation 'io.github.yangqingyuan:teki-quic:1.0.0.1-SNAPSHOT'


# 使用方式
## Get 请求
 
          val xquicClient = XquicClient.Builder()
                .connectTimeOut(13)
                .setReadTimeOut(23)//TODO 待实现
                .writeTimeout(15)//TODO 待实现
                .pingInterval(15)//TODO 待实现
                .ccType(CCType.BBR) //拥塞算法
                .build()
            val xRequest = XRequest.Builder()
                .url("https://192.168.10.245:8443")
                .addHeader("testA", "testA")// 可选，携带自定义头信息
                .get() //Default
                .build()

            val startTime = System.currentTimeMillis()
            xquicClient.newCall(xRequest).enqueue(object : XCallBack {
                override fun onFailure(call: XCall, exception: Exception) {
                    XLogUtils.error(exception.message)
                }

                override fun onResponse(call: XCall, xResponse: XResponse) {
                    XLogUtils.info(
                        " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody.getData()}"
                    )
                }
            })

## POST 请求
          val xquicClient = XquicClient.Builder()
                .connectTimeOut(13)
                .setReadTimeOut(23)//TODO 待实现
                .writeTimeout(15)//TODO 待实现
                .pingInterval(15)//TODO 待实现
                .ccType(CCType.BBR) //拥塞算法
                .build()
                
            val xRequestBody =XRequestBody.create(XMediaType.parse(XMediaType.MEDIA_TYPE_TEXT), "test")
            val xRequest = XRequest.Builder()
                .url("https://192.168.10.245:8443")
                .addHeader("testA", "testA")// 可选，携带自定义头信息
                .post(xRequestBody)
                .build()

            val startTime = System.currentTimeMillis()
            xquicClient.newCall(xRequest).enqueue(object : XCallBack {
                override fun onFailure(call: XCall, exception: Exception) {
                    XLogUtils.error(exception.message)
                }

                override fun onResponse(call: XCall, xResponse: XResponse) {
                    XLogUtils.info(
                        " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody.getData()}"
                    )
                }
            })


# 规划

（1）1.0 版本（支持短链接），进度：疯狂码中，最新（master分支）

（2）2.0 版本（支持长链接），进度：未开始

（3）3.0 版本（支持自定义协议，例如rtmp等），进度：未开始


# 架构说明



## 工程结构说明
包说明：
impl->包：逻辑实现类，里面封装了线程池和xquic的底层调用等
native->包：xquic底层JNI实现和回调/参数等

关键类说明：
XquicClient->类： 端链接API入口
XAsyncCall->类：真正的执行逻辑类
XQuicShortNative->类：JNI接口

# 其他
有任何问题，欢迎留言，同时也希望找志同道合的人，一同完善tekixquic，毕竟一个人的力量是有限的！！本人wx，➕V请标注
![image](https://user-images.githubusercontent.com/6867757/162711742-7cfd5e4b-54d8-4c4f-b80e-4d9c9af34ba5.png)





