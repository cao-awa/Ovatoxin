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

    /**
     * Runs [block] on the JavaFX application thread when the toolkit is already running, and
     * directly on the calling thread otherwise.
     *
     * Unlike [runOnFxThread], this never starts the toolkit and never waits. It is meant for work
     * produced from other threads — a clock, a network reply, a background computation — that must
     * touch a live scene but must not block its producer. Before the toolkit exists there is no
     * scene to touch, so running in place is safe and keeps the component unit-testable.
     */
    fun dispatchToFxThread(block: () -> Unit) {
        if (!this.started || Platform.isFxApplicationThread()) {
            block()
            return
        }
        try {
            Platform.runLater(block)
        } catch (_: IllegalStateException) {
            // The runtime stopped between the check and the post: there is no scene left to touch.
        }
    }
}
