package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ProgressBarTest {

    @Test
    fun `a bar starts on the value it was created with`() {
        assertEquals(0.4, ProgressBar(0.4).progress)
        assertEquals(0.4, ProgressBar(0.4).progress())
        assertEquals(ProgressBar.DEFAULT_PROGRESS, ProgressBar().progress)
    }

    @Test
    fun `the bar is written through both the call and the property`() {
        val bar = ProgressBar()
        bar.progress(0.25)
        assertEquals(0.25, bar.progress())

        bar.progress = 0.75
        assertEquals(0.75, bar.progress)
    }

    @Test
    fun `a value above range is clamped to a full bar`() {
        val bar = ProgressBar()
        bar.progress = 1.5
        assertEquals(1.0, bar.progress)

        bar.progress = 42.0
        assertEquals(1.0, bar.progress)
    }

    @Test
    fun `a negative value is clamped to an empty bar`() {
        val bar = ProgressBar(0.5)
        bar.progress = -0.5
        assertEquals(0.0, bar.progress)
    }

    @Test
    fun `a bar created out of range is clamped too`() {
        assertEquals(0.0, ProgressBar(-3.0).progress)
        assertEquals(1.0, ProgressBar(3.0).progress)
    }

    @Test
    fun `a NaN value reads as an empty bar`() {
        val bar = ProgressBar(Double.NaN)
        assertEquals(0.0, bar.progress)

        bar.progress = 0.5
        bar.progress = Double.NaN
        assertEquals(0.0, bar.progress)
    }

    @Test
    fun `a reset puts the original value back`() {
        val bar = ProgressBar(0.3)
        bar.progress = 0.9

        bar.reset()

        assertEquals(0.3, bar.progress)
    }

    @Test
    fun `the default a reset restores is the clamped one`() {
        val bar = ProgressBar(5.0)
        assertEquals(1.0, bar.progress)

        bar.progress = 0.0
        bar.reset()

        assertEquals(1.0, bar.progress)
    }

    @Test
    fun `a bar carries its style class and needs no toolkit to be measured`() {
        val bar = ProgressBar()

        assertFalse(bar.isNodeCreated, "a value alone must not build a JavaFX control")
        assertEquals("ovatoxin-progress", ProgressBar.STYLE_CLASS)
    }

    @Test
    fun `a bar follows the state its draw block reads`() {
        val state = State(0.0)
        val bar = ProgressBar()
        val frame = Frame()
        frame.layout {
            draw(bar) { component ->
                layout { width = 80.px }
                component.progress = state.value
            }.withState(state)
        }

        state.value = 0.6

        // A redraw re-runs the block, which reads the state again and writes the bar.
        assertEquals(0.6, bar.progress)
    }

    @Test
    fun `withState returns the same bar so calls chain`() {
        val bar = ProgressBar()
        assertSame(bar, bar.withState(State(0.5)))
    }

    @Test
    fun `a bar cannot be drawn twice`() {
        val bar = ProgressBar()
        Frame().draw(bar)

        assertFailsWith<IllegalArgumentException> { Frame().draw(bar) }
    }
}
