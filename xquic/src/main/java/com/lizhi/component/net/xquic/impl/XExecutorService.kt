package com.lizhi.component.net.xquic.impl

import java.util.concurrent.*

/**
 * common thread pool
 */
class XExecutorService {

    /** Executes calls. Created lazily.  */
    var executorService: ExecutorService = ThreadPoolExecutor(
        0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
        SynchronousQueue(), threadFactory("TekiXquic Dispatcher", false)
    )

    private fun threadFactory(name: String?, daemon: Boolean): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, name)
            result.isDaemon = daemon
            result
        }
    }
}