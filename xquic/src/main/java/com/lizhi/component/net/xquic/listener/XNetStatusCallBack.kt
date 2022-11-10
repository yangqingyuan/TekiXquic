package com.lizhi.component.net.xquic.listener

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.annotation.RequiresApi
import com.lizhi.component.net.xquic.utils.XLogUtils

/**
 * 保持全局一个
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class XNetStatusCallBack : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        isAvailable = true
        netHashCode = network.hashCode()
        XLogUtils.debug("onAvailable:${network}")
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        isAvailable = false
        XLogUtils.debug("onLost:${network}")
    }

    companion object {
        @Volatile
        var isAvailable: Boolean = false

        @Volatile
        var netHashCode: Int = -1

        @Volatile
        var isRegister = false

        val xNetStatusManager = XNetStatusCallBack()
    }
}