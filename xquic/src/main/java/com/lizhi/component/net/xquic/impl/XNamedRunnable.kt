package com.lizhi.component.net.xquic.impl


abstract class XNamedRunnable : Runnable {
    var name: String? = null

    override fun run() {
        val oldName = Thread.currentThread().name
        Thread.currentThread().name = name
        try {
            execute()
        } finally {
            Thread.currentThread().name = oldName
        }
    }

    protected abstract fun execute()
}