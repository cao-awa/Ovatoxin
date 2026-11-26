package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyleVisualTest {

    @Test
    fun `a snapshot freezes the fill with its opacity folded in`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5")
                opacity = 40
            }
            corner = 10.px
            foreground = Color.web("#ffffff")
        }
        val visual = style.resolveVisual(100.0, 40.0)

        // The alpha is the colour's own 1.0 scaled by the 40% opacity.
        assertEquals(0.4, visual.background?.opacity ?: Double.NaN, 1e-6)
        assertEquals(Color.web("#4c8bf5").red, visual.background?.red ?: Double.NaN, 1e-6)
        assertEquals(10.0, visual.corner)
        assertEquals(Color.web("#ffffff"), visual.foreground)
    }

    @Test
    fun `an image leaves no fill to animate`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5")
                image = "file:/tmp/bg.png"
            }
        }
        assertNull(style.resolveVisual(10.0, 10.0).background)
    }

    @Test
    fun `a snapshot renders the same css as the style`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5", 0.5)
                opacity = 50
            }
            corner = 50.percent
            foreground = Color.web("#102030")
        }
        assertEquals(
            style.toCss(200.0, 40.0, 16.0),
            style.renderCss(style.background.image, style.resolveVisual(200.0, 40.0, 16.0)),
        )
    }

    @Test
    fun `the ends of a blend are the source and the target`() {
        val from = StyleVisual(Color.RED, 0.0, null)
        val to = StyleVisual(Color.BLUE, 20.0, Color.WHITE)

        assertTrue(from.lerp(to, 0.0).matches(from))
        assertTrue(from.lerp(to, 1.0).matches(to))
    }

    @Test
    fun `a blend moves every channel halfway`() {
        val from = StyleVisual(Color.color(0.0, 0.0, 0.0, 1.0), 0.0, null)
        val to = StyleVisual(Color.color(1.0, 0.5, 0.0, 1.0), 20.0, null)
        val half = from.lerp(to, 0.5)

        assertEquals(Color.color(0.5, 0.25, 0.0, 1.0), half.background)
        assertEquals(10.0, half.corner)
    }

    @Test
    fun `a missing colour fades against transparency instead of jumping`() {
        val from = StyleVisual(null, 0.0, null)
        val to = StyleVisual(Color.color(1.0, 0.0, 0.0, 1.0), 0.0, null)
        val half = from.lerp(to, 0.5)

        assertEquals(Color.color(1.0, 0.0, 0.0, 0.5), half.background)
    }

    @Test
    fun `fading out halves the alpha`() {
        val from = StyleVisual(Color.color(1.0, 0.0, 0.0, 1.0), 0.0, null)
        val to = StyleVisual(null, 0.0, null)

        assertEquals(Color.color(1.0, 0.0, 0.0, 0.5), from.lerp(to, 0.5).background)
        assertNull(from.lerp(to, 1.0).background)
    }

    @Test
    fun `progress outside zero to one is clamped`() {
        val from = StyleVisual(Color.RED, 0.0, null)
        val to = StyleVisual(Color.BLUE, 10.0, null)

        assertTrue(from.lerp(to, -1.0).matches(from))
        assertTrue(from.lerp(to, 2.0).matches(to))
    }

    @Test
    fun `matching ignores the identity and compares the values`() {
        val a = StyleVisual(Color.RED, 4.0, Color.WHITE)
        val b = StyleVisual(Color.RED, 4.0, Color.WHITE)
        assertTrue(a.matches(b))
        assertTrue(!a.matches(b.copy(corner = 5.0)))
    }
}
