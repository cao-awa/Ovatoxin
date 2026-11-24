package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.widget.input.TextField
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class LabelTest {

    @Test
    fun `text supports both property and call style`() {
        val label = Label()
        assertEquals("", label.text)

        label.text = "User name"
        assertEquals("User name", label.text())

        label.text("Password")
        assertEquals("Password", label.text)
    }

    @Test
    fun `labelFor supports both property and call style`() {
        val field = TextField()
        val label = Label("User name")

        assertNull(label.labelFor)
        label.labelFor(field)
        assertSame(field, label.labelFor())

        label.labelFor = null
        assertNull(label.labelFor)
    }

    @Test
    fun `a label annotates the control it was created for`() {
        val field = TextField()
        assertSame(field, Label("User name", labelFor = field).labelFor())
    }

    @Test
    fun `a label whose control is a sibling is wired by construction`() {
        val frame = Frame()
        lateinit var field: TextField
        frame.layout {
            field = draw(TextField()) { input ->
                layout(
                    45.percent, 10.percent, 45.percent, 6.percent
                )
                input.hint = "your name"
            }
            draw(Label("User name", labelFor = field)) { label ->
                layout(
                    10.percent, 10.percent, 30.percent, 6.percent
                )
            }
        }

        val label = frame.children[1] as Label
        assertEquals("User name", label.text)
        assertSame(field, label.labelFor)
        assertFalse(label.isNodeCreated, "a label alone must not need a toolkit")
        assertFalse(field.isNodeCreated, "a label must not build its control early")
    }

    @Test
    fun `reset restores the caption and the association`() {
        val field = TextField()
        val label = Label("User name", labelFor = field)

        label.text = "changed"
        label.labelFor(null)
        label.reset()

        assertEquals("User name", label.text)
        assertSame(field, label.labelFor)
    }

    @Test
    fun `reset drops a later association but keeps the original one`() {
        val field = TextField()
        val other = TextField()
        val label = Label("User name")
        label.labelFor(field)
        label.labelFor(other)

        label.reset()

        assertNull(label.labelFor, "the label was created without a control")
    }

    @Test
    fun `a label is a caption, a text area is content`() {
        // The two are siblings, not a rename: a label names a neighbour, a text area carries text.
        val label = Label("User name")
        val body = TextArea("a paragraph of body text")

        assertNull(label.labelFor, "an unassociated label annotates nothing")
        assertEquals("User name", label.text)
        assertEquals("a paragraph of body text", body.text)
    }
}
