package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.em
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.container.Align
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.widget.container.FlexBox
import com.github.kusa233.ovatoxin.widget.container.Justify
import com.github.kusa233.ovatoxin.widget.text.Link
import com.github.kusa233.ovatoxin.widget.text.TextArea
import com.github.kusa233.ovatoxin.widget.input.TextField
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
        button.text("Something else")
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
        link.text("Elsewhere")
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
        frame.layout {
            draw(text) {
                layout { x = 50.percent }
                style { corner = 4.px }
            }
        }
        assertEquals(50.percent, text.layoutSpec.x)

        frame.remove(text)
        text.text("Changed while detached")
        frame.layout { draw(text) }

        assertEquals(Length.ZERO, text.layoutSpec.x)
        assertEquals(Style.DEFAULT_CORNER, text.styleSpec.corner)
        // The constructor value is what "default" means for this instance.
        assertEquals("Test", text.text)
    }

    @Test
    fun `a redraw drops what the block no longer sets`() {
        val state = State("zh-cn")
        val frame = Frame()
        val text = TextArea("Test")
        frame.layout {
            draw(text) {
                if (state.value == "zh-cn") {
                    it.text("翻译测试")
                    layout { x = 10.percent }
                    style { background { color = Color.web("#4c8bf5") } }
                }
            }
        }
        text.withState(state)

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
    fun `a redraw restores the padding the block declares`() {
        val state = State("a")
        val frame = Frame()
        val panel = Div()
        frame.layout {
            draw(panel) { div ->
                layout {
                    width = 100.percent
                    hPadding = 12.px
                    vPadding = 6.px
                }
                div.layout {
                    draw(TextArea()) { area -> area.text(state.value) }
                }
            }
        }
        panel.withState(state)

        // The global reset before every redraw clears the padding as well, so the second run of
        // the block has to put it back — otherwise a state change would silently close the inset.
        state.update("b")

        assertEquals(12.px, panel.layoutSpec.hPadding)
        assertEquals(6.px, panel.layoutSpec.vPadding)
        val content = panel.contentBox(200.0, 100.0)
        assertEquals(12.0, content.offsetX)
        assertEquals(6.0, content.offsetY)
        assertEquals(176.0, content.width)
        assertEquals(88.0, content.height)
    }

    @Test
    fun `a redraw keeps the children a replayed container still draws`() {
        val state = State("a")
        val frame = Frame()
        val panel = Div()
        frame.layout {
            draw(panel) { div ->
                div.layout {
                    draw(TextArea()) { area ->
                        layout { width = 50.percent }
                        area.text(state.value)
                    }
                }
            }
        }
        panel.withState(state)

        state.update("b")

        assertEquals(1, panel.children.size)
        val text = panel.children[0] as TextArea
        assertEquals("b", text.text)
        assertEquals(50.percent, text.layoutSpec.width)
    }

    @Test
    fun `a redraw restores the gap and the flow of a flex box`() {
        val state = State("a")
        val frame = Frame()
        val row = FlexBox()
        frame.layout {
            draw(row) { box ->
                layout { width = 100.percent; height = 50.percent }
                box.gap = 8.px
                box.justify = Justify.SPACE_BETWEEN
                box.align = Align.CENTER
                box.layout {
                    draw(TextArea()) { area -> area.text(state.value) }
                }
            }
        }
        row.withState(state)

        // The gap and the two enums belong to the box itself, so the global reset before a redraw
        // does not know about them: the block sets them again, and if it did not, a state change
        // would silently drop them.
        state.update("b")

        assertEquals(8.px, row.gap)
        assertEquals(Justify.SPACE_BETWEEN, row.justify)
        assertEquals(Align.CENTER, row.align)
    }

    @Test
    fun `a flex box without a block falls back to its own defaults`() {
        val row = FlexBox()
        row.gap = 8.px
        row.justify = Justify.END
        row.align = Align.END

        row.reset()

        // A box that is redrawn with a block that says nothing about the flow is a fresh box again.
        assertEquals(Length.ZERO, row.gap)
        assertEquals(Justify.START, row.justify)
        assertEquals(Align.START, row.align)
    }

    @Test
    fun `a redraw drops a font size the block no longer sets`() {
        val state = State("a")
        val frame = Frame()
        val panel = Div()
        frame.layout {
            draw(panel) { box ->
                layout { hPadding = 1.em }
                style { fontSize = 20.px }
                box.layout { draw(TextArea()) { area -> area.text(state.value) } }
            }
        }
        panel.withState(state)
        assertEquals(20.0, panel.resolvedFontSize)

        // The size lives in the style, so it is part of what the reset clears before the block
        // runs again; the block sets it again, and if it did not, the em padding would silently
        // fall back to the 14px default.
        state.update("b")

        assertEquals(20.0, panel.resolvedFontSize)
        assertEquals(20.0, panel.contentBox(200.0, 100.0).offsetX)
    }

    @Test
    fun `a surface without a block falls back to inheriting its font size`() {
        val panel = Div()
        panel.applyStyle { fontSize = 20.px }

        panel.reset()

        assertNull(panel.styleSpec.fontSize)
        assertEquals(Length.DEFAULT_FONT_SIZE, panel.resolvedFontSize)
    }
}
