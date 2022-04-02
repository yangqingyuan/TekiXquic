# XquicDemo

# 简介
基于 Xquic 进行二次封装的Android sdk库，使用了okhttp的封装方式

# 环境
使用到的第三方sdk
（1）xquic （https://github.com/alibaba/xquic）
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



