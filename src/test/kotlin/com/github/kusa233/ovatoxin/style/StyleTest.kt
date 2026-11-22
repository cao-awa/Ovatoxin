package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StyleTest {

    @Test
    fun `defaults are a solid fill with square corners`() {
        val style = Style()
        assertEquals(Style.DEFAULT_COLOR, style.background.color)
        assertNull(style.background.image)
        assertEquals(0.px, style.corner)
        assertEquals(0.0, style.cornerPixels(100.0, 40.0))
    }

    @Test
    fun `the documented background block sets the colour`() {
        val style = Style().configure {
            background {
                color = Style.DEFAULT_COLOR
            }
        }
        assertEquals(Style.DEFAULT_COLOR, style.background.color)
        assertEquals("-fx-background-color: #ffffff", style.toCss(10.0, 10.0).substringBefore(";"))
    }

    @Test
    fun `the background block configures the same background in place`() {
        val style = Style()
        val background = style.background
        style.background { color = Color.web("#123456") }
        assertSame(background, style.background)
        assertEquals(Color.web("#123456"), background.color)
    }

    @Test
    fun `a percentage corner resolves against the shorter side`() {
        val style = Style().configure { corner = 50.percent }

        // Wide box: the short side is the height.
        assertEquals(20.0, style.cornerPixels(200.0, 40.0))
        // Tall box: the short side is the width.
        assertEquals(20.0, style.cornerPixels(40.0, 200.0))
        // Square box: both are the same.
        assertEquals(20.0, style.cornerPixels(40.0, 40.0))
    }

    @Test
    fun `a pixel corner is absolute`() {
        val style = Style().configure { corner = 10.px }
        assertEquals(10.0, style.cornerPixels(200.0, 40.0))
        assertEquals(10.0, style.cornerPixels(40.0, 200.0))
    }

    @Test
    fun `a negative corner is clamped to zero`() {
        val style = Style().configure { corner = (-5).px }
        assertEquals(0.0, style.cornerPixels(100.0, 100.0))
    }

    @Test
    fun `percent corner scales with the box`() {
        val style = Style().configure { corner = 10.percent }
        assertEquals(4.0, style.cornerPixels(200.0, 40.0))
        assertEquals(8.0, style.cornerPixels(400.0, 80.0))
    }

    @Test
    fun `css always suppresses the border and the focus ring`() {
        val css = Style().toCss(100.0, 40.0)
        assertTrue(css.contains("-fx-border-width: 0"), css)
        assertTrue(css.contains("-fx-focus-color: transparent"), css)
        assertTrue(css.contains("-fx-faint-focus-color: transparent"), css)
        assertTrue(css.contains("-fx-background-insets: 0"), css)
    }

    @Test
    fun `css carries the resolved radius and the solid fill`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5")
            }
            corner = 50.percent
        }
        val css = style.toCss(200.0, 40.0)
        assertTrue(css.contains("-fx-background-radius: 20px"), css)
        assertTrue(css.contains("-fx-background-color: #4c8bf5"), css)
    }

    @Test
    fun `a null colour renders transparent`() {
        val style = Style().configure {
            background {
                color = null
            }
        }
        assertTrue(style.toCss(10.0, 10.0).contains("-fx-background-color: transparent"))
        assertNull(style.background.color)
    }

    @Test
    fun `translucent colors keep their alpha`() {
        val style = Style().configure {
            background {
                color = Color.color(1.0, 0.0, 0.0, 0.5)
            }
        }
        val css = style.toCss(10.0, 10.0)
        assertTrue(css.contains("rgba(255, 0, 0, 0.5)"), css)
    }

    @Test
    fun `an image is preferred over the colour`() {
        val style = Style().configure {
            background {
                image = "file:/tmp/bg.png"
                color = Color.web("#4c8bf5")
            }
        }

        val css = style.toCss(200.0, 40.0)
        // The image paints, and the colour behind it is gone instead of tinting it.
        assertTrue(css.contains("-fx-background-image: url(\"file:/tmp/bg.png\")"), css)
        assertTrue(css.contains("-fx-background-size: cover"), css)
        assertTrue(css.contains("-fx-background-repeat: no-repeat"), css)
        assertTrue(css.contains("-fx-background-color: transparent"), css)
        assertTrue(!css.contains("#4c8bf5"), css)
        // The colour is kept as the fallback for when the image is removed again.
        assertEquals(Color.web("#4c8bf5"), style.background.color)
    }

    @Test
    fun `the colour comes back when the image is removed`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5")
                image = "file:/tmp/bg.png"
            }
            background {
                image = null
            }
        }
        assertTrue(style.toCss(10.0, 10.0).contains("-fx-background-color: #4c8bf5"))
        assertTrue(!style.toCss(10.0, 10.0).contains("-fx-background-image"))
    }

    @Test
    fun `a background is fully opaque by default`() {
        assertEquals(100, Style().background.opacity)
        assertEquals(100, Background.DEFAULT_OPACITY)
    }

    @Test
    fun `opacity is a whole percentage`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5")
                opacity = 40
            }
        }
        assertEquals(40, style.background.opacity)
        // A plain Int, not a Length: the value reads as 40, not as pixels or a fraction.
        assertTrue(
            style.toCss(10.0, 10.0).contains("-fx-background-color: rgba(76, 139, 245, 0.4);"),
            style.toCss(10.0, 10.0),
        )
    }

    @Test
    fun `the ends of the opacity range are allowed`() {
        val clear = Style().configure { background { opacity = 0 } }
        assertTrue(
            clear.toCss(10.0, 10.0).contains("-fx-background-color: rgba(255, 255, 255, 0);"),
            clear.toCss(10.0, 10.0),
        )

        val solid = Style().configure { background { opacity = 100 } }
        assertTrue(solid.toCss(10.0, 10.0).contains("-fx-background-color: #ffffff;"))
    }

    @Test
    fun `an opacity below zero is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = -1 } }
        }
    }

    @Test
    fun `an opacity above one hundred is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = 101 } }
        }
    }

    @Test
    fun `a rejected opacity leaves the old value in place`() {
        val background = Background().configure { opacity = 60 }
        assertFailsWith<IllegalArgumentException> { background.opacity = 250 }
        assertEquals(60, background.opacity)
    }

    @Test
    fun `opacity scales the alpha the colour already carries`() {
        val style = Style().configure {
            background {
                color = Color.web("#4c8bf5", 0.5)
                opacity = 50
            }
        }
        assertTrue(
            style.toCss(10.0, 10.0).contains("-fx-background-color: rgba(76, 139, 245, 0.25);"),
            style.toCss(10.0, 10.0),
        )
    }

    @Test
    fun `opacity leaves the foreground alone`() {
        val style = Style().configure {
            background {
                opacity = 20
            }
            foreground = Color.web("#4c8bf5")
        }
        val css = style.toCss(10.0, 10.0)
        assertTrue(css.contains("-fx-background-color: rgba(255, 255, 255, 0.2);"), css)
        assertTrue(css.contains("-fx-text-fill: #4c8bf5;"), css)
    }

    @Test
    fun `copyFrom carries the opacity`() {
        val source = Style().configure { background { opacity = 30 } }
        val target = Style()
        target.copyFrom(source)
        assertEquals(30, target.background.opacity)
    }

    @Test
    fun `an image url is escaped for css`() {
        val style = Style().configure {
            background {
                image = "file:/tmp/my \"quoted\" bg.png"
            }
        }
        assertTrue(
            style.toCss(10.0, 10.0).contains("url(\"file:/tmp/my \\\"quoted\\\" bg.png\")"),
            style.toCss(10.0, 10.0),
        )
    }

    @Test
    fun `copyFrom duplicates every property`() {
        val source = Style().configure {
            background {
                color = Color.web("#102030")
                image = "file:/tmp/bg.png"
            }
            corner = 4.px
            foreground = Color.web("#4c8bf5")
        }
        val target = Style()
        target.copyFrom(source)

        assertEquals(source.background.color, target.background.color)
        assertEquals(source.background.image, target.background.image)
        assertEquals(source.corner, target.corner)
        assertEquals(Color.web("#4c8bf5"), target.foreground)
        // The lengths are values, not shared boxes.
        assertEquals(Length.pixels(4.0), target.corner)
    }

    @Test
    fun `there is no foreground until it is set`() {
        assertNull(Style().foreground)
        assertTrue(!Style().toCss(10.0, 10.0).contains("-fx-text-fill"), "no text colour by default")
    }

    @Test
    fun `foreground renders as the text fill`() {
        val style = Style().configure {
            foreground = Color.web("#4c8bf5")
        }
        assertTrue(
            style.toCss(10.0, 10.0).contains("-fx-text-fill: #4c8bf5;"),
            style.toCss(10.0, 10.0),
        )
    }

    @Test
    fun `a translucent foreground keeps its alpha`() {
        val style = Style().configure {
            foreground = Color.web("#4c8bf5", 0.5)
        }
        assertTrue(
            style.toCss(10.0, 10.0).contains("-fx-text-fill: rgba(76, 139, 245, 0.5);"),
            style.toCss(10.0, 10.0),
        )
    }

    @Test
    fun `a null foreground removes the text fill again`() {
        val style = Style().configure {
            foreground = Color.web("#4c8bf5")
            foreground = null
        }
        assertTrue(!style.toCss(10.0, 10.0).contains("-fx-text-fill"))
    }

    @Test
    fun `the foreground block in the documentation compiles`() {
        val style = Style().configure {
            background {
                color = Color.web("#ffffff")
            }
            corner = 4.px
            foreground = Color.web("#4c8bf5")
        }
        val css = style.toCss(100.0, 40.0)
        assertTrue(css.contains("-fx-text-fill: #4c8bf5;"), css)
        assertTrue(css.contains("-fx-background-color: #ffffff;"), css)
    }
}
