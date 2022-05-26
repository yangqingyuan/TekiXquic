package com.lizhi.component.net.xquic.listener

import java.net.InetAddress
import java.net.UnknownHostException

interface XDns {
    @Throws(UnknownHostException::class)
    fun lookup(hostname: String): List<InetAddress>

    companion object {
        /**
         * A DNS that uses [InetAddress.getAllByName] to ask the underlying operating system to
         * lookup IP addresses. Most custom [Dns] implementations should delegate to this instance.
         */
        @JvmField
        val SYSTEM: XDns = DnsSystem()

        private class DnsSystem : XDns {
            override fun lookup(hostname: String): List<InetAddress> {
                try {
                    return InetAddress.getAllByName(hostname).toList()
                } catch (e: NullPointerException) {
                    throw UnknownHostException("Broken system behaviour for dns lookup of $hostname").apply {
                        initCause(e)
                    }
                }
            }
        }
    }
}