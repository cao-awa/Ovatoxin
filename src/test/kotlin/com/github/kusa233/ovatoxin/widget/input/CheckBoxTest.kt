package com.github.kusa233.ovatoxin.widget.input

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CheckBoxTest {

    @Test
    fun `a box starts with the caption it was created with and unticked`() {
        val box = CheckBox("Remember me")

        assertEquals("Remember me", box.text)
        assertEquals("Remember me", box.text())
        assertFalse(box.checked)
        assertTrue(box.enabled)
    }

    @Test
    fun `a box without a caption is allowed`() {
        val box = CheckBox()

        assertEquals("", box.text)
        assertFalse(box.checked)
    }

    @Test
    fun `the caption is written through the call and read through the property`() {
        val box = CheckBox()
        box.text("A")
        assertEquals("A", box.text)

        box.text("B")
        assertEquals("B", box.text())
    }

    @Test
    fun `the tick can be set and read`() {
        val box = CheckBox("x")

        box.checked = true
        assertTrue(box.checked)

        box.checked = false
        assertFalse(box.checked)
    }

    @Test
    fun `writing the tick is silent`() {
        val reported = mutableListOf<Boolean>()
        val box = CheckBox("x").onChange { reported += it }

        box.checked = true
        box.checked = false

        assertTrue(reported.isEmpty(), "only a click reports: $reported")
    }

    @Test
    fun `a click reports the state the box now has`() {
        val reported = mutableListOf<Boolean>()
        val box = CheckBox("x").onChange { reported += it }

        // With a node the control has already flipped itself; without one the click flips the
        // field, so a test never has to build a JavaFX control to exercise the handler.
        box.performClick()
        box.performClick()

        assertEquals(listOf(true, false), reported)
        assertFalse(box.checked)
    }

    @Test
    fun `the field is already up to date when the handler runs`() {
        var seen: Boolean? = null
        val box: CheckBox = CheckBox("x")
        box.onChange { seen = box.checked }

        box.performClick()

        assertEquals(true, seen)
    }

    @Test
    fun `onClick runs on the same click as onChange`() {
        var changes = 0
        var clicks = 0
        val box = CheckBox("x")
        box.onChange { changes++ }
        box.onClick { clicks++ }

        box.performClick()

        assertEquals(1, changes)
        assertEquals(1, clicks)
    }

    @Test
    fun `a disabled box reports nothing`() {
        var changes = 0
        var clicks = 0
        val box = CheckBox("x")
        box.onChange { changes++ }
        box.onClick { clicks++ }
        box.enabled = false

        box.performClick()

        assertEquals(0, changes)
        assertEquals(0, clicks)
    }

    @Test
    fun `onChange returns the box so calls chain`() {
        val box = CheckBox()
        assertSame(box, box.onChange { })
        assertSame(box, box.onClick { })
    }

    @Test
    fun `a reset restores the caption, the tick and the handlers`() {
        var changes = 0
        val box = CheckBox("Keep open")
        box.text("Changed")
        box.checked = true
        box.enabled = false
        box.onChange { changes++ }

        box.reset()

        assertEquals("Keep open", box.text, "the caption goes back to the construction value")
        assertFalse(box.checked)
        assertTrue(box.enabled)

        box.performClick()
        assertEquals(0, changes, "a reset drops the handler")
    }

    @Test
    fun `a reset is silent`() {
        val reported = mutableListOf<Boolean>()
        val box = CheckBox("x")
        box.checked = true
        box.onChange { reported += it }

        box.reset()

        assertTrue(reported.isEmpty(), reported.toString())
    }

    @Test
    fun `a box carries its style class and builds no node until it is shown`() {
        val box = CheckBox("x")

        assertFalse(box.isNodeCreated, "a checked flag alone must not build a JavaFX control")
        assertEquals("ovatoxin-checkbox", CheckBox.STYLE_CLASS)
    }

    @Test
    fun `a box can be drawn and clicked like any other component`() {
        var clicked = 0
        val frame = Frame()
        frame.layout {
            draw(CheckBox("Remember me")) { box ->
                layout {
                    x = 10.percent
                    y = 70.percent
                }
                box.onClick { clicked++ }
            }
        }

        (frame.children[0] as CheckBox).performClick()

        assertEquals(1, clicked)
    }
}
