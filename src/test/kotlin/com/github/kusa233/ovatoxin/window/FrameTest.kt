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

    @Test
    fun `a frame shows its title bar by default`() {
        assertTrue(Frame().titleBarEnabled)
    }

    @Test
    fun `the constructor can start a frame without a title bar`() {
        assertFalse(Frame(titleBarEnabled = false).titleBarEnabled)
    }

    @Test
    fun `a frame has no explicit size limits by default`() {
        val frame = Frame()

        assertEquals(0.0, frame.minWidth)
        assertEquals(0.0, frame.minHeight)
        assertEquals(Double.MAX_VALUE, frame.maxWidth)
        assertEquals(Double.MAX_VALUE, frame.maxHeight)
    }

    @Test
    fun `the constructor configures the resize limits`() {
        val frame = Frame(
            minWidth = 320.0,
            minHeight = 240.0,
            maxWidth = 1280.0,
            maxHeight = 800.0,
        )

        assertEquals(320.0, frame.minWidth)
        assertEquals(240.0, frame.minHeight)
        assertEquals(1280.0, frame.maxWidth)
        assertEquals(800.0, frame.maxHeight)
    }

    @Test
    fun `reset restores the constructor resize limits`() {
        val frame = Frame(minWidth = 320.0, minHeight = 240.0, maxWidth = 1280.0, maxHeight = 800.0)
        frame.minWidth = 400.0
        frame.minHeight = 300.0
        frame.maxWidth = 1000.0
        frame.maxHeight = 700.0

        frame.reset()

        assertEquals(320.0, frame.minWidth)
        assertEquals(240.0, frame.minHeight)
        assertEquals(1280.0, frame.maxWidth)
        assertEquals(800.0, frame.maxHeight)
    }
}
