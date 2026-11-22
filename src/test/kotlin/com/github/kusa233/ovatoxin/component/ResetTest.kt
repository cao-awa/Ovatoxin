package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.widget.TextField
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins down the "defaults before every draw" rule: a component must never carry what an earlier
 * draw set into the next one.
 */
class ResetTest {

    @Test
    fun `a component resets its layout and style`() {
        val text = TextArea()
        text.layoutSpec.x = 50.percent
        text.layoutSpec.width = 20.px
        text.styleSpec.corner = 4.px
        text.styleSpec.background { color = Color.RED }

        text.reset()

        assertEquals(Length.ZERO, text.layoutSpec.x)
        assertEquals(Length.ZERO, text.layoutSpec.y)
        assertNull(text.layoutSpec.width)
        assertNull(text.layoutSpec.height)
        assertEquals(Style.DEFAULT_CORNER, text.styleSpec.corner)
        assertEquals(Style.DEFAULT_COLOR, text.styleSpec.background.color)
        assertNull(text.styleSpec.background.image)
        assertEquals(100, text.styleSpec.background.opacity)
        assertNull(text.styleSpec.foreground)
    }

    @Test
    fun `a component resets its visibility and click handler`() {
        val button = Button("Ok").onClick { println("clicked") }
        button.visible = false
        assertTrue(button.hasOnClick)

        button.reset()

        assertTrue(button.visible)
        assertFalse(button.hasOnClick)
    }

    @Test
    fun `a button resets to the caption it was created with`() {
        val button = Button("Ok")
        button.text = "Something else"
        button.enabled = false

        button.reset()

        assertEquals("Ok", button.text)
        assertTrue(button.enabled)
    }

    @Test
    fun `a text area resets its content and typography`() {
        val text = TextArea("Test")
        text.text("Changed")
        text.fontSize = 30.0
        text.bold = true
        text.align = TextAlignment.CENTER
        text.wrap = true
        text.fontFamily = "Some Font"

        text.reset()

        assertEquals("Test", text.text)
        assertEquals(TextArea.DEFAULT_FONT_SIZE, text.fontSize)
        assertFalse(text.bold)
        assertEquals(TextAlignment.LEFT, text.align)
        assertFalse(text.wrap)
        assertEquals(Font.getDefault().family, text.fontFamily)
    }

    @Test
    fun `a text field resets its content, hint and handlers`() {
        val field = TextField("initial")
        var changes = 0
        field.hint = "hint"
        field.editable = false
        field.onChange { changes++ }
        field.onSubmit { changes++ }
        field.text("typed")

        field.reset()

        assertEquals("initial", field.text)
        assertEquals("", field.hint)
        assertTrue(field.editable)
        // A reset reports nothing: the previous handlers are gone before the value changes.
        assertEquals(1, changes)
        field.text("typed again")
        field.performSubmit()
        assertEquals(1, changes)
    }

    @Test
    fun `a link resets its caption, url and default colour`() {
        val link = Link("Ovatoxin", "https://example.com")
        link.text = "Elsewhere"
        link.url = "https://example.org"
        link.styleSpec.foreground = Color.RED

        link.reset()

        assertEquals("Ovatoxin", link.text)
        assertEquals("https://example.com", link.url)
        assertEquals(Link.DEFAULT_FOREGROUND, link.styleSpec.foreground)
    }

    @Test
    fun `a div resets its clip and keeps its transparent fill`() {
        val div = Div()
        div.clip = false
        div.styleSpec.background { color = Color.RED }

        div.reset()

        assertTrue(div.clip)
        // A div frames a region, it never falls back to the white default fill.
        assertNull(div.styleSpec.background.color)
    }

    @Test
    fun `a frame resets its title and size`() {
        val frame = Frame("Title", 400.0, 300.0)
        frame.title = "Changed"
        frame.width = 1200.0
        frame.height = 900.0

        frame.reset()

        assertEquals("Title", frame.title)
        assertEquals(400.0, frame.width)
        assertEquals(300.0, frame.height)
        assertEquals(Frame.WINDOW_BACKGROUND, frame.styleSpec.background.color)
        assertEquals(0.px, frame.styleSpec.corner)
    }

    @Test
    fun `drawing the same component twice starts from the defaults`() {
        val frame = Frame()
        val text = TextArea("Test")
        frame.draw(text) {
            layout { x = 50.percent }
            style { corner = 4.px }
        }
        assertEquals(50.percent, text.layoutSpec.x)

        frame.remove(text)
        text.text("Changed while detached")
        frame.draw(text)

        assertEquals(Length.ZERO, text.layoutSpec.x)
        assertEquals(Style.DEFAULT_CORNER, text.styleSpec.corner)
        // The constructor value is what "default" means for this instance.
        assertEquals("Test", text.text)
    }

    @Test
    fun `a redraw drops what the block no longer sets`() {
        val state = State("zh-cn")
        val frame = Frame()
        val text = frame.draw(TextArea("Test")) {
            if (state.value == "zh-cn") {
                it.text("翻译测试")
                layout { x = 10.percent }
                style { background { color = Color.web("#4c8bf5") } }
            }
        }.withState(state)

        assertEquals("翻译测试", text.text)
        assertEquals(10.percent, text.layoutSpec.x)
        assertEquals(Color.web("#4c8bf5"), text.styleSpec.background.color)

        // The second run of the block is entered from a clean component, so everything the block
        // no longer sets falls back: the caption to "Test", the layout to zero, the fill to the
        // default white instead of the previous blue.
        state.update("en-us")

        assertEquals("Test", text.text)
        assertEquals(Length.ZERO, text.layoutSpec.x)
        assertEquals(Style.DEFAULT_COLOR, text.styleSpec.background.color)
    }

    @Test
    fun `a redraw keeps the children a replayed container still draws`() {
        val state = State("a")
        val frame = Frame()
        val panel = frame.draw(Div()) { div ->
            div.layout {
                draw(TextArea()) { area ->
                    layout { width = 50.percent }
                    area.text(state.value)
                }
            }
        }.withState(state)

        state.update("b")

        assertEquals(1, panel.children.size)
        val text = panel.children[0] as TextArea
        assertEquals("b", text.text)
        assertEquals(50.percent, text.layoutSpec.width)
    }
}
