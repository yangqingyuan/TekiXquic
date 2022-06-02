package com.lizhi.component.net.xquic.impl

import com.lizhi.component.net.xquic.listener.XCall
import com.lizhi.component.net.xquic.listener.XRunnable
import java.util.*
import java.util.concurrent.*

/**
 * 作用: 调度器
 * 作者: yqy
 * 创建日期: 2022/4/1.
 */
class XDispatcher {

    private val maxRequests = 64
    private val maxRequestsPerHost = 5
    private val idleCallback: Runnable? = null

    /** Executes calls. Created lazily.  */
    private var executorService: ExecutorService? = null

    /** Ready async calls in the order they'll be run.  */
    private val readyAsyncCalls: Deque<XRunnable> = ArrayDeque()

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet.  */
    private val runningAsyncCalls: Deque<XRunnable> = ArrayDeque()

    /** Running synchronous calls. Includes canceled calls that haven't finished yet.  */
    private val runningSyncCalls: Deque<XRealCall> = ArrayDeque()


    private fun threadFactory(name: String?, daemon: Boolean): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, name)
            result.isDaemon = daemon
            result
        }
    }

    @Synchronized
    fun executorService(): ExecutorService? {
        if (executorService == null) {
            executorService = ThreadPoolExecutor(
                0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                SynchronousQueue(), threadFactory("OkHttp Dispatcher", false)
            )
        }
        return executorService
    }

    fun enqueue(xAsyncCall: XRunnable) {
        synchronized(this) {
            readyAsyncCalls.add(xAsyncCall)
        }
        promoteAndExecute()
    }


    private fun promoteAndExecute(): Boolean {
        assert(!Thread.holdsLock(this))
        val executableCalls: MutableList<XRunnable> = ArrayList<XRunnable>()
        var isRunning: Boolean
        synchronized(this) {
            val i: MutableIterator<XRunnable> = readyAsyncCalls.iterator()
            while (i.hasNext()) {
                val asyncCall: XRunnable = i.next()
                if (runningAsyncCalls.size >= maxRequests) break // Max capacity.
                if (runningCallsForHost(asyncCall) >= maxRequestsPerHost) {
                    //XLogUtils.error("太多了")
                    continue  // Host max capacity.
                }
                i.remove()
                executableCalls.add(asyncCall)
                runningAsyncCalls.add(asyncCall)
            }
            isRunning = runningCallsCount() > 0
        }
        var i = 0
        val size = executableCalls.size
        while (i < size) {
            val asyncCall: XRunnable = executableCalls[i]
            asyncCall.executeOn(executorService())
            i++
        }
        return isRunning
    }

    private fun runningCallsForHost(call: XRunnable): Int {
        var result = 0
        for (c in runningAsyncCalls) {
            if (c.url() == call.url()) result++
        }
        return result
    }


    @Synchronized
    fun cancelAll() {
        for (call in readyAsyncCalls) {
            call.get().cancel()
        }

        for (call in runningAsyncCalls) {
            call.get().cancel()
        }

        for (call in runningSyncCalls) {
            call.cancel()
        }
    }


    @Synchronized
    fun queuedCalls(): List<XCall> {
        val result: MutableList<XCall> = ArrayList<XCall>()
        for (asyncCall in readyAsyncCalls) {
            result.add(asyncCall.get())
        }
        for (asyncCall in runningAsyncCalls) {
            result.add(asyncCall.get())
        }
        return Collections.unmodifiableList(result)
    }

    @Synchronized
    fun runningCallsCount(): Int {
        return runningAsyncCalls.size + runningSyncCalls.size
    }

    fun finished(xRunnable: XRunnable) {
        if (runningAsyncCalls.contains(xRunnable)) {
            finished(runningAsyncCalls, xRunnable)
        } else {
            synchronized(this) {
                //移除没有执行的
                readyAsyncCalls.remove(xRunnable)
            }
        }
    }


    private fun <T> finished(calls: Deque<T>, call: T) {
        var idleCallback: Runnable?
        synchronized(this) {
            if (!calls.remove(call)) throw AssertionError("Call wasn't in-flight!")
            idleCallback = this.idleCallback
        }
        val isRunning = promoteAndExecute()
        if (!isRunning && idleCallback != null) {
            idleCallback!!.run()
        }
    }


}