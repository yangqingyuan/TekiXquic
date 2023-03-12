package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.mode.XRequest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * 作用: 链接池
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
class XConnectionPool(
    private val maxIdleConnections: Int = 5,
    private val keepAliveDuration: Long = 2,
    private val timeUnit: TimeUnit = TimeUnit.MINUTES
) {

    private val keepAliveDurationNs: Long = timeUnit.toNanos(keepAliveDuration)

    init {
        // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
        require(keepAliveDuration > 0) { "keepAliveDuration <= 0: $keepAliveDuration" }
    }

    private val connections: MutableList<XConnection> = CopyOnWriteArrayList()

    private fun cleanUp(now: Long): Long {
        var idleConnectionCount = 0
        var longestIdleConnection: XConnection? = null
        var longestIdleDurationNs = Long.MIN_VALUE

        // Find either a connection to evict, or the time that the next eviction is due.
        synchronized(this) {
            val i: Iterator<XConnection> = connections.iterator()
            while (i.hasNext()) {
                val connection: XConnection = i.next()

                if (connection.isDestroy) {
                    connections.remove(longestIdleConnection)
                    return -1
                }

                idleConnectionCount++

                // If the connection is ready to be evicted, we're done.
                val idleDurationNs: Long = now - connection.idleAtNanos

                if (idleDurationNs > longestIdleDurationNs) {
                    longestIdleDurationNs = idleDurationNs
                    longestIdleConnection = connection
                }
            }
            if (longestIdleDurationNs >= this.keepAliveDurationNs
                || idleConnectionCount > this.maxIdleConnections
            ) {
                // We've found a connection to evict. Remove it from the list, then close it below (outside
                // of the synchronized block).
                connections.remove(longestIdleConnection)
            } else {
                // No connections, idle or in use.
                return -1
            }
        }
        longestIdleConnection?.close()
        // Cleanup again immediately.
        return 0
    }

    fun get(request: XRequest): XConnection? {
        synchronized(this) {
            for (connection in connections) {
                if (connection.isDestroy) {
                    connections.remove(connection)
                } else {
                    if (connection.isEligible(request)) {
                        return connection
                    }
                }
            }
            return null
        }
    }

    fun get(url: String): XConnection? {
        return get(XRequest.Builder().url(url).build())
    }

    fun put(connection: XConnection) {
        synchronized(this) {
            cleanUp(System.nanoTime())
            connections.add(connection)
        }
    }

    fun remove(connection: XConnection) {
        synchronized(this) {
            connections.remove(connection)
            cleanUp(System.nanoTime())
        }
    }
}