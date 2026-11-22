package com.github.kusa233.ovatoxin.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LengthTest {

    @Test
    fun `percent and p alias produce identical lengths`() {
        assertEquals(10.percent, 10.p)
        assertEquals(Length.Kind.PERCENT, 10.percent.kind)
        assertEquals(10.0, 10.percent.value)
    }

    @Test
    fun `fractions are supported`() {
        assertEquals(Length.percent(12.5), 12.5.percent)
        assertEquals(12.5, 12.5.p.value)
    }

    @Test
    fun `pixels are absolute`() {
        assertEquals(Length.Kind.PIXELS, 24.px.kind)
        assertEquals(24.0, 24.px.resolve(500.0))
    }

    @Test
    fun `percentages resolve against the available space`() {
        assertEquals(80.0, 10.percent.resolve(800.0))
        assertEquals(540.0, 90.percent.resolve(600.0))
        assertEquals(0.0, 0.percent.resolve(600.0))
        assertEquals(600.0, Length.FULL.resolve(600.0))
    }

    @Test
    fun `toPixels freezes a relative length`() {
        val frozen = 10.percent.toPixels(800.0)
        assertEquals(Length.Kind.PIXELS, frozen.kind)
        assertEquals(80.0, frozen.resolve(100.0))
    }

    @Test
    fun `toString is human readable`() {
        assertEquals("10%", 10.percent.toString())
        assertEquals("12.5%", 12.5.percent.toString())
        assertEquals("10px", 10.px.toString())
    }
}

class LayoutTest {

    @Test
    fun `defaults are anchored top left without an explicit size`() {
        val layout = Layout()
        assertEquals(Length.ZERO, layout.x)
        assertEquals(Length.ZERO, layout.y)
        assertNull(layout.width)
        assertNull(layout.height)
    }

    @Test
    fun `configure runs the block on the receiver`() {
        val layout = Layout().configure {
            x = 10.percent
            y = 20.p
            width = 80.percent
            height = 10.px
        }
        assertEquals(10.percent, layout.x)
        assertEquals(20.percent, layout.y)
        assertEquals(80.percent, layout.width)
        assertEquals(10.px, layout.height)
    }

    @Test
    fun `copyFrom clones every value`() {
        val source = Layout().configure {
            x = 1.percent
            y = 2.px
            width = 3.percent
            height = 4.px
        }
        val target = Layout().configure { copyFrom(source) }
        assertEquals(source.x, target.x)
        assertEquals(source.y, target.y)
        assertEquals(source.width, target.width)
        assertEquals(source.height, target.height)
    }
}
