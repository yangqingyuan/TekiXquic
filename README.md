# XquicDemo

# 简介
基于 Xquic 进行二次封装的Android sdk库，为了方便理解和使用，沿用了okhttp的封装方式模式

# 环境
工具

（1）Android studio

（2）ndk （本人用21）

使用到的第三方sdk

（1）xquic （https://github.com/alibaba/xquic) 目前用的是最新的1.0.1版本

（2）libev

# 使用方式

 
          val xquicClient = XquicClient.Builder()
                .connectTimeOut(13)
                .setReadTimeOut(23)
                .writeTimeout(15)
                .pingInterval(15)
                .build()
            val xRequest = XRequest.Builder()
                .url("https://192.168.10.245:8443")
                .get() //Default
                .build()

            val startTime = System.currentTimeMillis()
            xquicClient.newCall(xRequest).enqueue(object : XCallBack {
                override fun onFailure(call: XCall, exception: Exception) {
                    exception.printStackTrace()
                    XLogUtils.error(exception.message)
                }

                override fun onResponse(call: XCall, xResponse: XResponse) {

                    XLogUtils.info(
                        " java 花费时间 ${(System.currentTimeMillis() - startTime)} ms,content=${xResponse.xResponseBody?.getData()}"
                    )
                }
            })


# 规划

（1）1.0 版本（支持短链接），进度：疯狂码中，最新（dev分支）

（2）2.0 版本（支持长链接），进度：未开始

（3）3.0 版本（支持自定义协议，例如rtmp等），进度：未开始



