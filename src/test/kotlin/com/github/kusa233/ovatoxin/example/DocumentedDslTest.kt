package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.runtime.Browser
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Align
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.CheckBox
import com.github.kusa233.ovatoxin.widget.ComboBox
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.FlexBox
import com.github.kusa233.ovatoxin.widget.Justify
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.ProgressBar
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.widget.TextField
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runs the documented DSL snippets, so the documentation cannot drift away from the API.
 */
class DocumentedDslTest {

    @Test
    fun `the basic snippet compiles and wires up`() {
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
    fun `the div snippet compiles and nests`() {
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
    fun `the style snippet compiles and applies`() {
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
    fun `the rounded div snippet compiles and applies`() {
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
    fun `the image background snippet compiles and wins over the colour`() {
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
    fun `the click snippet compiles and runs`() {
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
    fun `the link snippet compiles and opens the url`() {
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
    fun `the replaced link snippet compiles without opening a browser`() {
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
    fun `the opacity snippet compiles and scales the fill`() {
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
    fun `the out of range opacity really throws`() {
        // Both ends of the range are rejected; keep it that way.
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = 101 } }
        }
        assertFailsWith<IllegalArgumentException> {
            Style().configure { background { opacity = -1 } }
        }
    }

    @Test
    fun `the state snippet compiles and rebinds on a new value`() {
        val state = State("zh-cn")

        val frame = Frame()
        frame.layout {
            val textShow = draw(TextArea()) { text ->
                layout {
                    x = 10.percent
                    y = 10.percent
                    width = 80.percent
                    height = 10.percent
                }
                val translation = I18N.languages[state.value]?.get("test.translation") ?: "No translation"
                text.text(translation)
            }.withState(state)
        }

        val text = frame.children[0] as TextArea
        assertEquals("翻译测试", text.text())
        assertEquals("zh-cn", state.value)

        // The state is the only trigger: nothing below touches the component directly.
        state.update("en-us")

        assertEquals("en-us", state.value)
        assertEquals("Test translation", text.text())
    }

    @Test
    fun `the state feeding snippet compiles and updates the bound label`() {
        val state = State("zh-cn")
        val frame = Frame()
        frame.layout {
            val textShow = draw(TextArea()) { text ->
                val translation = I18N.languages[state.value]?.get("test.translation") ?: "No translation"
                text.text(translation)
            }.withState(state)

            draw(TextField()) { input ->
                layout {
                    x = 10.percent
                    y = 30.percent
                    width = 80.percent
                    height = 10.percent
                }
                input.hint = "Type here..."
                input.onChange {
                    state.update(it)
                }
            }
        }

        val text = frame.children[0] as TextArea
        val input = frame.children[1] as TextField

        // Typing is exactly what the change handler sees.
        input.text("en-us")

        assertEquals("en-us", state.value)
        assertEquals("Test translation", text.text())
        assertEquals("en-us", input.text())
    }

    @Test
    fun `the equal value rule really keeps the tree untouched`() {
        val state = State("zh-cn")
        val frame = Frame()
        frame.layout {
            draw(TextArea()) { text ->
                text.text(I18N.languages[state.value]?.get("test.translation") ?: "No translation")
            }.withState(state)
        }
        val text = frame.children[0] as TextArea
        val before = text.text()

        state.update("zh-cn")

        assertEquals(before, text.text())
        assertEquals("zh-cn", state.value)
    }

    @Test
    fun `the progress bar snippet compiles and follows its state`() {
        val done = State(Percent.ZERO)
        val frame = Frame()
        frame.layout {
            draw(ProgressBar(40.percent)) { bar ->
                layout(
                    10.percent, 10.percent, 80.percent, 4.percent
                )
                style { corner = 50.percent }
                // The drawn block reads the state, so a redraw refills the bar.
                bar.progress = done.value
            }.withState(done)
        }

        val bar = frame.children[0] as ProgressBar
        assertEquals(Percent.ZERO, bar.progress)
        assertFalse(bar.isNodeCreated, "a progress alone must not need a toolkit")

        done.update(50.percent)

        assertEquals(50.percent, bar.progress)
    }

    @Test
    fun `the progress bar takes its start in percent as well as in a fraction`() {
        assertEquals(40.percent, ProgressBar(40.percent).progress)
        assertEquals(ProgressBar(0.4).progress, ProgressBar(40.percent).progress)
        assertEquals(100.percent, ProgressBar(400.percent).progress)
    }

    @Test
    fun `the check box snippet compiles and reports a click`() {
        val reported = mutableListOf<Boolean>()
        val frame = Frame()
        frame.layout {
            draw(CheckBox("Remember me")) { box ->
                layout(
                    10.percent, 20.percent, 80.percent, 6.percent
                )
                box.onChange { checked -> reported += checked }
            }
        }

        val box = frame.children[0] as CheckBox
        assertEquals("Remember me", box.text)
        box.performClick()

        assertEquals(listOf(true), reported)
        assertTrue(box.checked)
    }

    @Test
    fun `the combo box snippet compiles and reports the picked item`() {
        val picked = mutableListOf<String>()
        val frame = Frame()
        frame.layout {
            draw(ComboBox(listOf("Small", "Medium", "Large"))) { combo ->
                layout(
                    10.percent, 30.percent, 80.percent, 7.percent
                )
                combo.prompt = "Pick a size"
                combo.selectedIndex = 1
                combo.onSelectItem { size -> picked += size }
            }
        }

        val combo = frame.children[0] as ComboBox<*>
        assertEquals("Medium", combo.selectedItem)
        assertEquals("Pick a size", combo.prompt)
        assertTrue(picked.isEmpty(), "a selection the caller made is silent")

        combo.selectFromUser(2)

        assertEquals(listOf("Large"), picked)
    }

    @Test
    fun `the formatted combo box snippet compiles and reads items through the formatter`() {
        val combo = ComboBox(listOf(Size("Small"), Size("Medium"))) { it.name }
        combo.selectedIndex = 1

        assertEquals("Medium", combo.selectedText)
    }

    @Test
    fun `the live combo box snippet compiles and grows`() {
        val combo = ComboBox(listOf("Small", "Medium", "Large"))

        combo.items.add("Extra Large")

        assertEquals(4, combo.size)
        assertEquals("Extra Large", combo.item(3))
    }

    @Test
    fun `the padding snippet compiles and insets the content`() {
        val frame = Frame()
        // A frame is nobody's child, so its layout is applied to it directly; a surface that is
        // drawn inside another one declares the same values in the `layout { }` block of its draw.
        frame.applyLayout {
            hPadding = 24.px
            vPadding = 16.px
        }
        frame.layout {
            draw(Button()) { button ->
                layout {
                    x = 0.px
                    y = 0.px
                    width = 100.percent
                    height = 20.percent
                }
                button.text = "inset"
            }
        }

        // Padding is a value of the layout the surface is laid out with, and the block keeps its
        // children.
        assertEquals(24.px, frame.layoutSpec.hPadding)
        assertEquals(16.px, frame.layoutSpec.vPadding)
        assertEquals(1, frame.children.size)

        // The room left over is twenty-four pixels narrower on both axes, and the content starts
        // one padding in, so a child at 100% still ends one padding short of the edge.
        val content = frame.contentBox(800.0, 600.0)
        assertEquals(24.0, content.offsetX)
        assertEquals(16.0, content.offsetY)
        assertEquals(752.0, content.width)
        assertEquals(568.0, content.height)
    }

    @Test
    fun `the div padding snippet compiles and rescopes the percentages`() {
        val frame = Frame()
        frame.layout {
            draw(Div()) { panel ->
                layout {
                    width = 80.percent
                    height = 50.percent
                    hPadding = 12.px
                    vPadding = 12.px
                }
                panel.layout {
                    draw(TextArea()) { text ->
                        layout {
                            width = 100.percent
                            height = 100.percent
                        }
                        text.text("12px from the edge")
                    }
                }
            }
        }

        val panel = frame.children[0] as Div
        val text = panel.children[0] as TextArea
        assertEquals(12.px, panel.layoutSpec.hPadding)
        assertEquals(12.px, panel.layoutSpec.vPadding)
        assertEquals(100.percent, text.layoutSpec.width)
        // 100% of the panel is its own width minus the padding on both sides.
        val content = panel.contentBox(400.0, 200.0)
        assertEquals(376.0, content.width)
        assertEquals(176.0, content.height)
    }

    @Test
    fun `the flex box snippet compiles and wraps its children`() {
        val frame = Frame()
        frame.layout {
            draw(FlexBox()) { row ->
                layout {
                    x = 10.percent
                    y = 10.percent
                    width = 80.percent
                    height = 40.percent
                    // 内边距只是内缩，和子组件之间的间距无关
                    hPadding = 8.px
                    vPadding = 8.px
                }
                // 间距和分布写在 FlexBox 自己身上
                row.gap = 8.px
                row.justify = Justify.SPACE_BETWEEN
                row.align = Align.CENTER
                row.layout {
                    for (index in 1..12) {
                        draw(Button()) { cell ->
                            layout {
                                width = 18.percent
                                height = 30.px
                            }
                            cell.text = "$index"
                        }
                    }
                }
            }
        }

        val row = frame.children[0] as FlexBox
        assertEquals(12, row.children.size)
        assertEquals("1", (row.children[0] as Button).text)

        // Eighteen percent of the inset box is 141.12px, and five of them plus the gaps still fit
        // into 784px, so twelve buttons need three lines of five, five and two.
        val content = row.contentBox(800.0, 240.0)
        assertEquals(8.0, content.offsetX)
        assertEquals(784.0, content.width)
        row.beginPass(content, row.children)
        assertEquals(8.0, row.slotFor(row.children[0], content).x)
        assertEquals(8.0, row.slotFor(row.children[0], content).y)
        // The sixth cell cannot join the first line and opens the second one, below the tallest
        // child of the first line rather than at the top of the box.
        assertEquals(8.0, row.slotFor(row.children[5], content).x)
        assertEquals(8.0 + 30.0 + 8.0, row.slotFor(row.children[5], content).y)
        // The eleventh cell opens the third line, one line further down again.
        assertEquals(8.0, row.slotFor(row.children[10], content).x)
        assertEquals(8.0 + (30.0 + 8.0) * 2, row.slotFor(row.children[10], content).y)

        // SPACE_BETWEEN pushes the last cell of a full line against the opposite inset: five cells
        // and four gaps use 737.6px, and the 46.4px left over are shared between the four gaps.
        val fifth = row.children[4].layoutSpec.width!!.resolve(content.width)
        assertEquals(content.offsetX + content.width, row.slotFor(row.children[4], content).x + fifth)
    }

    @Test
    fun `a flex child keeps its own offset instead of losing it`() {
        val frame = Frame()
        frame.layout {
            draw(FlexBox()) { row ->
                layout {
                    width = 80.percent
                    height = 40.percent
                    hPadding = 8.px
                    vPadding = 8.px
                }
                row.layout {
                    draw(Button()) { cell ->
                        layout {
                            x = 5.px
                            y = 3.px
                            width = 20.percent
                            height = 20.px
                        }
                        cell.text = "nudged"
                    }
                }
            }
        }

        val row = frame.children[0] as FlexBox
        val content = row.contentBox(800.0, 240.0)
        row.beginPass(content, row.children)

        // The slot is the position of the flow, i.e. the inset corner; the `x` and `y` the child
        // declares are added on top when it is placed, so a flow is nudged but never overridden.
        val slot = row.slotFor(row.children[0], content)
        assertEquals(8.0, slot.x)
        assertEquals(8.0, slot.y)
        assertEquals(5.px, row.children[0].layoutSpec.x)
        assertEquals(3.px, row.children[0].layoutSpec.y)
    }

    /** A custom item type, as the formatted snippet uses. */
    private data class Size(val name: String)
}
