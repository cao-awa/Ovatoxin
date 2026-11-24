package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.widget.text.TextArea
import com.github.kusa233.ovatoxin.widget.input.TextField
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ComponentTreeTest {

    @Test
    fun `draw attaches children and applies their layout`() {
        val frame = Frame()
        frame.layout {
            draw(TextArea()) { text ->
                layout {
                    x = 10.percent
                    y = 90.percent
                    width = 80.percent
                    height = 7.percent
                }
                text.text("Test")
            }
        }

        assertEquals(1, frame.children.size)
        val text = frame.children[0] as TextArea
        assertEquals("Test", text.text())
        assertEquals(10.percent, text.layoutSpec.x)
        assertEquals(90.percent, text.layoutSpec.y)
        assertEquals(80.percent, text.layoutSpec.width)
        assertEquals(7.percent, text.layoutSpec.height)
        assertSame(frame, text.parent)
    }

    @Test
    fun `layout returns the container so blocks can be chained`() {
        val frame = Frame()
        assertSame(frame, frame.layout { })
    }

    @Test
    fun `building a tree does not require a running toolkit`() {
        val frame = Frame()
        frame.layout {
            draw(TextArea()) { it.text("a") }
            draw(Button()) { it.text = "b" }
        }
        assertEquals(2, frame.children.size)
        assertFalse(frame.children[0].isNodeCreated)
        assertFalse(frame.children[1].isNodeCreated)
    }

    @Test
    fun `children keep insertion order`() {
        val frame = Frame()
        frame.layout {
            draw(TextArea())
            draw(Button())
            draw(TextField())
        }
        assertTrue(frame.children[0] is TextArea)
        assertTrue(frame.children[1] is Button)
        assertTrue(frame.children[2] is TextField)
    }

    @Test
    fun `remove detaches a child`() {
        val frame = Frame()
        val text = TextArea()
        frame.layout { draw(text) }
        assertSame(frame, text.parent)

        frame.remove(text)
        assertTrue(frame.children.isEmpty())
        assertNull(text.parent)
    }

    @Test
    fun `onClick runs with the button as receiver`() {
        var printed: String? = null
        val button = Button()
        button.text = "Button"
        button.onClick {
            printed = this.text
        }
        button.performClick()
        assertEquals("Button", printed)
    }

    @Test
    fun `the documented example wires the button to the label`() {
        val frame = Frame()
        val output = StringBuilder()
        frame.layout {
            val text1 = draw(TextArea()) { text ->
                layout { x = 10.percent }
                text.text("Test")
            }
            draw(Button()) { button ->
                button.text = "Button"
                button.onClick {
                    output.append(text1.text())
                }
            }
        }
        (frame.children[1] as Button).performClick()
        assertEquals("Test", output.toString())
    }

    @Test
    fun `disabled buttons ignore clicks`() {
        var clicks = 0
        val button = Button()
        button.onClick { clicks++ }

        button.enabled = false
        button.performClick()
        assertEquals(0, clicks)

        button.enabled = true
        button.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `a component cannot be attached twice`() {
        val frame = Frame()
        val text = TextArea()
        frame.layout { draw(text) }

        val failure = runCatching { Frame().layout { draw(text) } }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `text supports both property and call style`() {
        val text = TextArea()
        text.text = "a"
        assertEquals("a", text.text())
        text.text("b")
        assertEquals("b", text.text)
    }

    @Test
    fun `text field reports changes and submissions`() {
        var changed: String? = null
        var submitted: String? = null
        val field = TextField()
            .onChange { changed = it }
            .onSubmit { submitted = it }

        field.text("typed")
        assertEquals("typed", changed)

        field.performSubmit()
        assertEquals("typed", submitted)
    }

    @Test
    fun `every component can be clicked`() {
        val clicks = mutableListOf<String>()
        val text = TextArea("label").also { it.onClick { clicks += "text:${this.text}" } }
        val field = TextField().also { it.onClick { clicks += "field:${this.text}" } }
        val area = Div().also { it.onClick { clicks += "div" } }
        val frame = Frame().also { it.onClick { clicks += "frame:${this.title}" } }

        text.performClick()
        field.performClick()
        area.performClick()
        frame.performClick()

        assertEquals(listOf("text:label", "field:", "div", "frame:Ovatoxin"), clicks)
    }

    @Test
    fun `the click handler receives the concrete component type`() {
        val area = Div()
        area.onClick {
            // `this` is a Div, so its own members are reachable without a cast.
            this.clip = false
        }
        area.performClick()
        assertFalse(area.clip)
    }

    @Test
    fun `a component without a handler ignores clicks`() {
        val text = TextArea("untouched")
        assertFalse(text.hasOnClick)
        text.performClick()
        assertEquals("untouched", text.text())
    }

    @Test
    fun `onClick replaces the previous handler`() {
        var calls = ""
        val text = TextArea()
        text.onClick { calls += "first" }
        text.onClick { calls += "second" }

        text.performClick()
        assertEquals("second", calls)
    }

    @Test
    fun `onClick returns the component so calls chain`() {
        val text = TextArea()
        assertSame(text, text.onClick { })
    }
}
