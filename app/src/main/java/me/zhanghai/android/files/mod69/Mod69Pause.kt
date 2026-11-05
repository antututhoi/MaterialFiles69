package me.zhanghai.android.files.mod69

import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap

/** 69 mod: pause / resume copy, move and other file jobs (from their notification). */
object Mod69Pause {
    private val jobThreads = ConcurrentHashMap<Int, Thread>()
    private val paused: MutableSet<Thread> = ConcurrentHashMap.newKeySet()

    fun register(jobId: Int) {
        jobThreads[jobId] = Thread.currentThread()
    }

    fun toggle(jobId: Int) {
        val t = jobThreads[jobId] ?: return
        if (!paused.remove(t)) paused.add(t)
    }

    fun isPaused(jobId: Int): Boolean = jobThreads[jobId]?.let { it in paused } ?: false

    /** Called often by running jobs; waits while the job is paused. */
    @Throws(InterruptedIOException::class)
    fun waitIfPaused() {
        val t = Thread.currentThread()
        while (t in paused) {
            try {
                Thread.sleep(250)
            } catch (e: InterruptedException) {
                paused.remove(t)
                throw InterruptedIOException()
            }
        }
    }
}
