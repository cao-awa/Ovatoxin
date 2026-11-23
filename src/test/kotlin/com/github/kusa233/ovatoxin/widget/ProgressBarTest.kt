package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.percent
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
        assertEquals(40.percent, ProgressBar(0.4).progress)
        assertEquals(0.4, ProgressBar(0.4).progress())
        assertEquals(Percent.ZERO, ProgressBar().progress)
        assertEquals(ProgressBar.DEFAULT_PROGRESS, ProgressBar().progress())
    }

    @Test
    fun `the bar is written through both the call and the property`() {
        val bar = ProgressBar()
        bar.progress(0.25)
        assertEquals(0.25, bar.progress())

        bar.progress = 75.percent
        assertEquals(75.percent, bar.progress)
        assertEquals(0.75, bar.progress())
    }

    @Test
    fun `the percent overload writes the same fraction the number does`() {
        val bar = ProgressBar()
        bar.progress(40.percent)
        assertEquals(0.4, bar.progress())

        bar.progress(0.5)
        assertEquals(50.percent, bar.progress)
    }

    @Test
    fun `a value above range is clamped to a full bar`() {
        val bar = ProgressBar()
        bar.progress = 150.percent
        assertEquals(100.percent, bar.progress)

        bar.progress = 4200.percent
        assertEquals(100.percent, bar.progress)
    }

    @Test
    fun `a negative value is clamped to an empty bar`() {
        val bar = ProgressBar(0.5)
        bar.progress = (-50).percent
        assertEquals(0.percent, bar.progress)
    }

    @Test
    fun `a bar created out of range is clamped too`() {
        assertEquals(0.percent, ProgressBar(-3.0).progress)
        assertEquals(100.percent, ProgressBar(3.0).progress)
    }

    @Test
    fun `a NaN value reads as an empty bar`() {
        val bar = ProgressBar(Double.NaN)
        assertEquals(0.percent, bar.progress)

        bar.progress = 50.percent
        bar.progress(Double.NaN)
        assertEquals(0.percent, bar.progress)
    }

    @Test
    fun `a reset puts the original value back`() {
        val bar = ProgressBar(0.3)
        bar.progress = 90.percent

        bar.reset()

        assertEquals(30.percent, bar.progress)
    }

    @Test
    fun `the default a reset restores is the clamped one`() {
        val bar = ProgressBar(5.0)
        assertEquals(100.percent, bar.progress)

        bar.progress = 0.percent
        bar.reset()

        assertEquals(100.percent, bar.progress)
    }

    @Test
    fun `a bar carries its style class and needs no toolkit to be measured`() {
        val bar = ProgressBar()

        assertFalse(bar.isNodeCreated, "a value alone must not build a JavaFX control")
        assertEquals("ovatoxin-progress", ProgressBar.STYLE_CLASS)
    }

    @Test
    fun `a bar follows the state its draw block reads`() {
        val state = State(0.percent)
        val bar = ProgressBar()
        val frame = Frame()
        frame.layout {
            draw(bar) { component ->
                layout { width = 80.px }
                component.progress = state.value
            }.withState(state)
        }

        state.value = 60.percent

        // A redraw re-runs the block, which reads the state again and writes the bar.
        assertEquals(60.percent, bar.progress)
    }

    @Test
    fun `withState returns the same bar so calls chain`() {
        val bar = ProgressBar()
        assertSame(bar, bar.withState(State(50.percent)))
    }

    @Test
    fun `a bar cannot be drawn twice`() {
        val bar = ProgressBar()
        Frame().layout { draw(bar) }

        assertFailsWith<IllegalArgumentException> { Frame().layout { draw(bar) } }
    }
}
