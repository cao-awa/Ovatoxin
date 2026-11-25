package com.github.kusa233.ovatoxin.window

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the close subscription of a [Frame].
 *
 * A close request is a plain callback, so these run without a toolkit: the stage hands the request
 * to [Frame.performCloseRequest], and the tests call the same entry point.
 */
class FrameTest {

    @Test
    fun `a frame has no close handler by default`() {
        val frame = Frame()

        assertFalse(frame.hasOnClose)
        // Asking to close with nothing subscribed is a no-op, not a failure.
        frame.performCloseRequest()
    }

    @Test
    fun `onClose runs the handler when the window is asked to close`() {
        val frame = Frame()
        var closes = 0
        frame.onClose { closes++ }

        assertTrue(frame.hasOnClose)
        frame.performCloseRequest()

        assertEquals(1, closes)
    }

    @Test
    fun `onClose keeps every subscriber and runs them in order`() {
        val frame = Frame()
        val order = mutableListOf<String>()
        frame.onClose { order += "first" }
        frame.onClose { order += "second" }

        frame.performCloseRequest()

        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `a close handler receives the frame`() {
        val frame = Frame("Bye", 400.0, 300.0)
        var seen: Frame? = null
        frame.onClose { seen = this }

        frame.performCloseRequest()

        assertSame(frame, seen)
        assertEquals("Bye", seen?.title)
    }

    @Test
    fun `onClose returns the frame so it chains onto the window`() {
        val frame = Frame()

        assertSame(frame, frame.onClose { })
    }

    @Test
    fun `reset clears the close handlers`() {
        val frame = Frame()
        frame.onClose { }

        frame.reset()

        assertFalse(frame.hasOnClose)
    }
}
