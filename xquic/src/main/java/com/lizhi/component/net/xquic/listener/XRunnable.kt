package com.lizhi.component.net.xquic.listener

import java.util.concurrent.ExecutorService

interface XRunnable {
    fun executeOn(executorService: ExecutorService?)
    fun execute()
    fun url(): String
    fun get(): XCall
    fun cancel()
}