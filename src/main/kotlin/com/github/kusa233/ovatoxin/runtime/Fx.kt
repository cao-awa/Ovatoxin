package com.github.kusa233.ovatoxin.runtime

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * Tiny bridge to the JavaFX runtime.
 *
 * The toolkit is started lazily, so a program can build its component tree first and only
 * pay the startup cost when a window is shown.
 */
internal object Fx {

    private val lock = Any()

    @Volatile
    private var started: Boolean = false

    /** Starts the JavaFX runtime once and keeps the JVM alive until the last window closes. */
    fun ensureStarted() {
        if (this.started) return
        synchronized(this.lock) {
            if (this.started) return
            try {
                Platform.startup { }
            } catch (_: IllegalStateException) {
                // The toolkit was already initialized by someone else.
            }
            Platform.setImplicitExit(true)
            this.started = true
        }
    }

    /** Runs [block] on the JavaFX application thread and waits for it to finish. */
    fun runOnFxThread(block: () -> Unit) {
        ensureStarted()
        if (Platform.isFxApplicationThread()) {
            block()
            return
        }

        val failure = AtomicReference<Throwable?>(null)
        val done = CountDownLatch(1)
        Platform.runLater {
            try {
                block()
            } catch (t: Throwable) {
                failure.set(t)
            } finally {
                done.countDown()
            }
        }
        done.await()
        failure.get()?.let { throw it }
    }
}
