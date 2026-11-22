package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.runtime.Browser
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Compiles `doc/DSL.md` verbatim, so the documentation cannot drift away from the API.
 */
class DocumentedDslTest {

    @Test
    fun `the snippet from doc DSL md compiles and wires up`() {
        val frame = Frame()
        frame.layout {
            val text1 = draw(TextArea()) { text ->
                layout {
                    x = 10.percent // Or 10.p
                    y = 90.percent
                    width = 80.percent
                    height = 70.percent
                }
                text.text("Test")
            }
            draw(Button()) { button ->
                layout {
                    x = 10.percent
                    y = 10.percent
                    width = 80.percent
                    height = 10.percent
                }
                button.text = "Button"
                button.onClick {
                    println(text1.text())
                }
            }
        }

        assertEquals(2, frame.children.size)
        val text = frame.children[0] as TextArea
        val button = frame.children[1] as Button
        assertEquals("Test", text.text())
        assertEquals("Button", button.text)
        assertSame(frame, text.parent)
        assertSame(frame, button.parent)
    }

    @Test
    fun `the div snippet from doc DSL md compiles and nests`() {
        val frame = Frame()
        frame.layout {
            draw(Div()) { panel ->
                layout { // the geometry of the panel itself
                    x = 10.percent
                    y = 10.percent
                    width = 80.percent
                    height = 50.percent
                }
                panel.layout { // the contents of the panel
                    draw(TextArea()) { text ->
                        layout { // 50% of the panel, not of the window
                            x = 50.percent
                            width = 50.percent
                        }
                        text.text("inside the panel")
                    }
                }
            }
        }

        assertEquals(1, frame.children.size)
        val panel = frame.children[0] as Div
        val text = panel.children[0] as TextArea
        assertSame(frame, panel.parent)
        assertSame(panel, text.parent)
        assertEquals("inside the panel", text.text())
        assertEquals(80.percent, panel.layoutSpec.width)
        assertEquals(50.percent, text.layoutSpec.x)
    }

    @Test
    fun `the style snippet from doc DSL md compiles and applies`() {
        val frame = Frame()
        frame.layout {
            draw(Button()) { button ->
                layout {
                    x = 10.percent
                    y = 10.percent
                    width = 80.percent
                    height = 10.percent
                }
                style {
                    background {
                        color = Color.web("#4c8bf5")
                    }
                    corner = 50.percent       // A pill; or 10.px for a fixed radius
                    foreground = Color.web("#ffffff")   // The text colour
                }
                button.text = "Button"
            }
        }

        val button = frame.children[0] as Button
        assertEquals(Color.web("#4c8bf5"), button.styleSpec.background.color)
        assertEquals(50.percent, button.styleSpec.corner)
        assertEquals(Color.web("#ffffff"), button.styleSpec.foreground)
        assertEquals("Button", button.text)
        // Half of the shorter side, i.e. the roundest possible corner.
        assertEquals(30.0, button.styleSpec.cornerPixels(640.0, 60.0))
    }

    @Test
    fun `the rounded div snippet from doc DSL md compiles and applies`() {
        val frame = Frame()
        frame.layout {
            draw(Div()) { panel ->
                layout {
                    width = 80.percent
                    height = 24.percent
                }
                style {
                    background {
                        color = Style.DEFAULT_COLOR
                    }
                    corner = 12.percent
                }
                panel.layout {
                    draw(TextArea()) { text ->
                        layout { x = 50.percent }
                        text.text("inside")
                    }
                }
            }
        }

        val panel = frame.children[0] as Div
        assertEquals(Style.DEFAULT_COLOR, panel.styleSpec.background.color)
        assertEquals(12.percent, panel.styleSpec.corner)
        assertSame(panel, panel.children[0].parent)
    }

    @Test
    fun `the image background snippet from doc DSL md compiles and wins over the colour`() {
        val frame = Frame()
        frame.layout {
            draw(Div()) { panel ->
                layout {
                    width = 80.percent
                    height = 24.percent
                }
                style {
                    background {
                        image = "file:/pictures/bg.png"
                        color = Style.DEFAULT_COLOR
                        opacity = 40                      // 40%, not 40.percent
                    }
                }
            }
        }

        val panel = frame.children[0] as Div
        val background = panel.styleSpec.background
        assertEquals("file:/pictures/bg.png", background.image)
        assertEquals(Style.DEFAULT_COLOR, background.color)
        assertEquals(40, background.opacity)

        // The image is the layer that paints; the colour stays as a fallback for when it is gone.
        val css = panel.styleSpec.toCss(400.0, 100.0)
        assertTrue(css.contains("-fx-background-image: url(\"file:/pictures/bg.png\")"), css)
        assertTrue(css.contains("-fx-background-color: transparent"), css)
        assertTrue(!css.contains("#ffffff"), css)
    }

    @Test
    fun `the click snippet from doc DSL md compiles and runs`() {
        val printed = StringBuilder()
        val frame = Frame()
        frame.layout {
            draw(TextArea()) { text ->
                layout {
                    x = 10.percent
                    y = 10.percent
                }
                text.text("Click me")
                text.onClick {
                    printed.append(this.text) // `this` is the TextArea that was clicked
                }
            }
        }

        (frame.children[0] as TextArea).performClick()
        assertEquals("Click me", printed.toString())
    }

    @Test
    fun `the link snippet from doc DSL md compiles and opens the url`() {
        val opened = mutableListOf<String>()
        val previous = Browser.opener
        Browser.opener = { url -> opened += url }
        try {
            val frame = Frame()
            frame.layout {
                draw(Link("Ovatoxin on GitHub", "https://github.com/kusa233/Ovatoxin")) { link ->
                    layout {
                        x = 10.percent
                        y = 90.percent
                    }
                }
            }

            val link = frame.children[0] as Link
            assertEquals("https://github.com/kusa233/Ovatoxin", link.url)
            link.performClick()
            assertEquals(listOf("https://github.com/kusa233/Ovatoxin"), opened)
        } finally {
            Browser.opener = previous
        }
    }

    @Test
    fun `the replaced link snippet from doc DSL md compiles without opening a browser`() {
        val opened = mutableListOf<String>()
        val printed = StringBuilder()
        val previous = Browser.opener
        Browser.opener = { url -> opened += url }
        try {
            val frame = Frame()
            frame.layout {
                draw(Link("Do something else", "")) { link ->
                    layout {
                        y = 90.percent
                    }
                    link.onClick {
                        printed.append("no browser, custom logic instead")
                    }
                }
            }

            (frame.children[0] as Link).performClick()
            assertEquals("no browser, custom logic instead", printed.toString())
            assertTrue(opened.isEmpty(), "the browser must not be opened")
        } finally {
            Browser.opener = previous
        }
    }

    @Test
    fun `the opacity snippet from doc DSL md compiles and scales the fill`() {
        val frame = Frame()
        frame.layout {
            draw(Div()) { panel ->
                layout {
                    width = 80.percent
                    height = 24.percent
                }
                style {
                    background {
                        color = Color.web("#4c8bf5")
                        opacity = 40 // 40%, not 40.percent
                    }
                    corner = 50.percent
                }
            }
        }

        val panel = frame.children[0] as Div
        val background = panel.styleSpec.background
        assertEquals(40, background.opacity)
        val css = panel.styleSpec.toCss(200.0, 40.0)
        assertTrue(css.contains("-fx-background-color: rgba(76, 139, 245, 0.4);"), css)
    }

    @Test
    fun `the out of range opacity snippet from doc DSL md really throws`() {
        // The documentation claims both of these fail; keep it honest.
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = 101 } }
        }
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = -1 } }
        }
    }
}
