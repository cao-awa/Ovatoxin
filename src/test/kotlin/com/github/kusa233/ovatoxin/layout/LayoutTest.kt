package com.github.kusa233.ovatoxin.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LengthTest {

    @Test
    fun `percent and p alias produce identical lengths`() {
        assertEquals(10.percent, 10.p)
        assertIs<Percent>(10.percent)
        assertEquals(10.0, 10.percent.value)
    }

    @Test
    fun `fractions are supported`() {
        assertEquals(Length.percent(12.5), 12.5.percent)
        assertEquals(12.5, 12.5.p.value)
    }

    @Test
    fun `pixels are absolute`() {
        assertIs<Pixel>(24.px)
        assertEquals(24.0, 24.px.resolve(500.0))
    }

    @Test
    fun `em and its factory produce identical lengths`() {
        assertEquals(2.em, Length.ems(2.0))
        assertEquals(2.0, 2.em.value)
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
        assertIs<Pixel>(frozen)
        assertEquals(80.0, frozen.resolve(100.0))
    }

    @Test
    fun `an em resolves against the font size`() {
        assertIs<Em>(1.5.em)
        assertEquals(1.5, 1.5.em.value)
        assertEquals(21.0, 1.5.em.resolve(800.0, 14.0))
        assertEquals(36.0, 1.5.em.resolve(800.0, 24.0))
        assertEquals(0.0, 0.em.resolve(800.0, 24.0))
    }

    @Test
    fun `an em ignores the parent space`() {
        assertEquals(1.em.resolve(100.0, 16.0), 1.em.resolve(9999.0, 16.0))
        assertEquals(16.0, 1.em.resolve(100.0, 16.0))
    }

    @Test
    fun `an em without a font size falls back to the library default`() {
        assertEquals(14.0, 1.em.resolve(100.0))
        assertEquals(21.0, 1.5.em.resolve(100.0))
        assertEquals(Length.DEFAULT_FONT_SIZE, 1.em.resolve(100.0, Length.DEFAULT_FONT_SIZE))
        assertEquals(21.0, 1.5.em.toPixels(100.0).resolve(0.0))
    }

    @Test
    fun `toString is human readable`() {
        assertEquals("10%", 10.percent.toString())
        assertEquals("12.5%", 12.5.percent.toString())
        assertEquals("10px", 10.px.toString())
        assertEquals("1.5em", 1.5.em.toString())
    }

    @Test
    fun `zero is empty in any unit`() {
        assertEquals(0.px, Pixel.ZERO)
        assertEquals(0.percent, Percent.ZERO)
        assertEquals(0.em, Em.ZERO)
        assertEquals(Pixel.ZERO, Length.ZERO)
        assertTrue(Length.ZERO.isZero)
        assertTrue(Percent.ZERO.isZero)
        assertTrue(Em.ZERO.isZero)
        assertTrue(!Length.FULL.isZero)
    }

    @Test
    fun `the zero of one unit is not the zero of another`() {
        val emptyPercent: Length = Percent.ZERO
        val emptyPixel: Length = Pixel.ZERO
        val emptyEm: Length = Em.ZERO

        // Both are nothing, but they are not the same length, so neither can feed the other's
        // operators.
        assertNotEquals(emptyPercent, emptyPixel)
        assertNotEquals(emptyPercent, emptyEm)
        assertNotEquals(emptyPixel, emptyEm)
    }

    @Test
    fun `lengths of the same unit compare`() {
        assertTrue(1.percent <= 1.percent)
        assertTrue(1.percent >= 1.percent)
        assertTrue(2.percent > 1.percent)
        assertTrue(1.percent < 2.percent)
        assertTrue(10.px > 4.px)
        assertTrue(2.em > 1.em)
        assertEquals(0, 4.px.compareTo(4.px))
        assertEquals(0, 4.em.compareTo(4.em))
    }

    @Test
    fun `lengths of the same unit add and subtract`() {
        assertEquals(3.percent, 1.percent + 2.percent)
        assertEquals(-1.percent, 1.percent - 2.percent)
        assertEquals(6.px, 2.px + 4.px)
        assertEquals(2.px, 6.px - 4.px)
        assertEquals(2.em, 1.5.em + 0.5.em)
        assertEquals(-1.em, 1.em - 2.em)
    }

    @Test
    fun `a zero stays in its own unit`() {
        assertEquals(1.percent, Percent.ZERO + 1.percent)
        assertEquals(1.px, Pixel.ZERO + 1.px)
        assertEquals(1.em, Em.ZERO + 1.em)
    }

    @Test
    fun `arithmetic never changes the unit`() {
        assertIs<Percent>(1.percent + 2.percent)
        assertIs<Percent>(Percent.ZERO + 1.percent)
        assertIs<Percent>(Percent.ZERO - 1.percent)
        assertIs<Percent>(Percent.ZERO * 2)

        assertIs<Pixel>(1.px + 2.px)
        assertIs<Pixel>(Pixel.ZERO + 1.px)
        assertIs<Pixel>(Pixel.ZERO - 1.px)
        assertIs<Pixel>(Pixel.ZERO * 2)

        assertIs<Em>(1.em + 2.em)
        assertIs<Em>(Em.ZERO + 1.em)
        assertIs<Em>(Em.ZERO - 1.em)
        assertIs<Em>(Em.ZERO * 2)
    }

    @Test
    fun `scaling stays in the same unit`() {
        assertEquals(20.percent, 10.percent * 2)
        assertEquals(20.percent, 10.percent * 2.0)
        assertEquals(20.px, 10.px * 2)
        assertEquals(5.percent, 10.percent / 2)
        assertEquals(5.px, 10.px / 2.0)
        assertEquals(20.percent, 2 * 10.percent)
        assertEquals(20.px, 2.0 * 10.px)
        assertEquals(-10.percent, -(10.percent))
        assertEquals(3.em, 1.5.em * 2)
        assertEquals(3.em, 2 * 1.5.em)
        assertEquals(1.5.em, 3.em / 2)
        assertEquals(-1.em, -(1.em))
    }

    @Test
    fun `dividing two lengths gives a ratio`() {
        assertEquals(4.0, 100.percent / 25.percent)
        assertEquals(2.5, 10.px / 4.px)
        assertEquals(2.0, 3.em / 1.5.em)
    }

    @Test
    fun `a progress loop advances in percent`() {
        var done = Percent.ZERO
        while (done < Length.FULL) {
            done += 1.percent
        }
        assertEquals(Length.FULL, done)
        assertEquals(0, done.compareTo(100.percent))
    }

    @Test
    fun `the default zero agrees with the pixel zero`() {
        // `Pixel` is a `Length`, so using a pixel initialises the superclass first. The zero of the
        // superclass must therefore not be a field holding `Pixel.ZERO`: it would be read while the
        // companion of `Pixel` is still empty, and every pixel length would follow it into a
        // failure. Reading it here, next to the unit it mirrors, is what pins the two together.
        assertTrue(Length.ZERO.isZero)
        assertEquals(Pixel.ZERO, Length.ZERO)
        assertEquals(0.0, Length.ZERO.resolve(100.0, 20.0))
    }
}

class LayoutTest {

    @Test
    fun `defaults are anchored top left without an explicit size or padding`() {
        val layout = Layout()
        assertEquals(Length.ZERO, layout.x)
        assertEquals(Length.ZERO, layout.y)
        assertNull(layout.width)
        assertNull(layout.height)
        assertEquals(Length.ZERO, layout.hPadding)
        assertEquals(Length.ZERO, layout.vPadding)
    }

    @Test
    fun `configure runs the block on the receiver`() {
        val layout = Layout().configure {
            x = 10.percent
            y = 20.p
            width = 80.percent
            height = 10.px
            hPadding = 12.px
            vPadding = 6.px
        }
        assertEquals(10.percent, layout.x)
        assertEquals(20.percent, layout.y)
        assertEquals(80.percent, layout.width)
        assertEquals(10.px, layout.height)
        assertEquals(12.px, layout.hPadding)
        assertEquals(6.px, layout.vPadding)
    }

    @Test
    fun `reset clears the padding together with everything else`() {
        val layout = Layout().configure {
            x = 10.percent
            width = 5.px
            hPadding = 12.px
            vPadding = 6.px
        }

        layout.reset()

        assertEquals(Length.ZERO, layout.x)
        assertNull(layout.width)
        assertEquals(Length.ZERO, layout.hPadding)
        assertEquals(Length.ZERO, layout.vPadding)
    }

    @Test
    fun `copyFrom clones every value`() {
        val source = Layout().configure {
            x = 1.percent
            y = 2.px
            width = 3.percent
            height = 4.px
            hPadding = 5.px
            vPadding = 6.percent
        }
        val target = Layout().configure { copyFrom(source) }
        assertEquals(source.x, target.x)
        assertEquals(source.y, target.y)
        assertEquals(source.width, target.width)
        assertEquals(source.height, target.height)
        assertEquals(source.hPadding, target.hPadding)
        assertEquals(source.vPadding, target.vPadding)
    }
}
