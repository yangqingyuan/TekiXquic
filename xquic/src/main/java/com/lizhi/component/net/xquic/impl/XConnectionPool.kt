package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.mode.XRequest
import com.lizhi.component.net.xquic.utils.XLogUtils
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 作用: 链接池
 * 作者: yqy
 * 创建日期: 2022/5/26.
 */
class XConnectionPool(
    private val maxIdleConnections: Int = 5,
    private val keepAliveDuration: Long = 2,
    timeUnit: TimeUnit = TimeUnit.MINUTES
) : Object() {
    private lateinit var xDispatcher: XDispatcher
    private val keepAliveDurationNs: Long = timeUnit.toNanos(keepAliveDuration)

    init {
        // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
        require(keepAliveDuration > 0) { "keepAliveDuration <= 0: $keepAliveDuration" }
    }

    fun setDispatcher(xDispatcher: XDispatcher) {
        this.xDispatcher = xDispatcher
    }

    private val connections: Deque<XConnection> = ArrayDeque()
    var cleanupRunning = false

    private val cleanupRunnable = Runnable {
        while (true) {
            var waitNanos: Long = cleanup(System.nanoTime())
            if (waitNanos == -1L) return@Runnable
            if (waitNanos > 0) {
                val waitMillis = waitNanos / 1000000L
                waitNanos -= waitMillis * 1000000L
                synchronized(this@XConnectionPool) {
                    try {
                        this@XConnectionPool.wait(waitMillis, waitNanos.toInt())
                    } catch (ignored: InterruptedException) {
                    }
                }
            }
        }
    }

    private fun cleanup(now: Long): Long {
        var inUseConnectionCount = 0
        var idleConnectionCount = 0
        var longestIdleConnection: XConnection? = null
        var longestIdleDurationNs = Long.MIN_VALUE

        // Find either a connection to evict, or the time that the next eviction is due.
        synchronized(this) {
            val i: Iterator<XConnection> = connections.iterator()
            while (i.hasNext()) {
                val connection: XConnection = i.next()

                // If the connection is in use, keep searching.
                /*if (pruneAndGetAllocationCount(connection, now) > 0) {
                    inUseConnectionCount++
                    continue
                }*/
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
            } else if (idleConnectionCount > 0) {
                // A connection will be ready to evict soon.
                return keepAliveDurationNs - longestIdleDurationNs
            } else if (inUseConnectionCount > 0) {
                // All connections are in use. It'll be at least the keep alive duration 'til we run again.
                return keepAliveDurationNs
            } else {
                // No connections, idle or in use.
                cleanupRunning = false
                return -1
            }
        }
        //XLogUtils.error("remove longestIdleConnection")
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


    fun put(connection: XConnection) {
        synchronized(this) {
            if (!cleanupRunning) {
                cleanupRunning = true
                xDispatcher.executorService()?.execute(cleanupRunnable)
            }
            connections.add(connection)
        }
    }
}