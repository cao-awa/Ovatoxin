package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests for the [bareTextArea] drawing helper. */
class TextAreaTest {

    @Test
    fun `bareTextArea draws a transparent and configured text area`() {
        val frame = Frame()
        var built: TextArea? = null

        frame.layout {
            built = bareTextArea("hello") { area ->
                layout {
                    width = 100.percent
                    height = 24.px
                }
                style {
                    foreground = Color.web("#162033")
                }
                area.bold = true
            }
        }

        val area = built!!
        assertSame(area, frame.children.single())
        assertEquals("hello", area.text())
        assertEquals(24.px, area.layoutSpec.height)
        assertEquals(Color.TRANSPARENT, area.styleSpec.background.color)
        assertEquals(Color.web("#162033"), area.styleSpec.foreground)
        assertTrue(area.bold)
    }

    @Test
    fun `bareTextArea defaults to empty text`() {
        val frame = Frame()
        var built: TextArea? = null

        frame.layout {
            built = bareTextArea()
        }

        assertEquals("", built!!.text())
    }
}
