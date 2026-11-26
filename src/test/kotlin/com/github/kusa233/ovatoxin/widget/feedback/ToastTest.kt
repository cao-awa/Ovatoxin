package com.github.kusa233.ovatoxin.widget.feedback

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ToastTest {

    @Test
    fun `a toast starts hidden and builds no node`() {
        val toast = Toast()

        assertFalse(toast.isShowing)
        assertNull(toast.message)
        assertFalse(toast.isNodeCreated)
        assertEquals(Toast.DEFAULT_DURATION, toast.duration)
        assertEquals(ToastTone.INFO, toast.tone)
        assertEquals(ToastPosition.BOTTOM_CENTER, toast.position)
        assertFalse(toast.dimOutside)
        assertNull(toast.currentDimOutside)
    }

    @Test
    fun `show records the latest message before the overlay exists`() {
        val toast = Toast()

        toast.show("first")

        assertTrue(toast.isShowing)
        assertEquals("first", toast.message)
        assertEquals(ToastTone.INFO, toast.currentTone)

        toast.show("second", tone = ToastTone.SUCCESS)

        assertEquals("second", toast.message)
        assertEquals(ToastTone.SUCCESS, toast.currentTone)
    }

    @Test
    fun `show accepts a per-message duration and position`() {
        val toast = Toast(duration = 1.seconds, tone = ToastTone.WARNING)

        toast.show("短消息", duration = 4.seconds, position = ToastPosition.TOP_CENTER)

        assertTrue(toast.isShowing)
        assertEquals("短消息", toast.message)
        assertEquals(ToastTone.WARNING, toast.currentTone)
    }

    @Test
    fun `show accepts the centre position and a dimming scrim`() {
        val toast = Toast()

        toast.show("居中提示", position = ToastPosition.CENTER, dimOutside = true)

        assertEquals("居中提示", toast.message)
        assertEquals(ToastPosition.CENTER, toast.currentPosition)
        assertEquals(true, toast.currentDimOutside)
    }

    @Test
    fun `a custom content request records state without running the drawing block`() {
        val toast = Toast()
        var drawn = false

        toast.show(position = ToastPosition.CENTER, dimOutside = true) {
            drawn = true
        }

        assertTrue(toast.isShowing)
        assertNull(toast.message)
        assertEquals(ToastTone.INFO, toast.currentTone)
        assertEquals(true, toast.currentDimOutside)
        assertFalse(drawn)
    }

    @Test
    fun `dismiss clears the pending message`() {
        val toast = Toast()
        toast.show("temporary")

        toast.dismiss()

        assertFalse(toast.isShowing)
        assertNull(toast.message)
    }

    @Test
    fun `a toast is a transparent overlay inside a frame`() {
        val frame = Frame()
        val toast = Toast()

        frame.layout {
            draw(toast) {
                layout {
                    width = 100.percent
                    height = 100.percent
                }
            }
        }

        assertSame(toast, frame.children.single())
        assertEquals(100.percent, toast.layoutSpec.width)
        assertEquals(100.percent, toast.layoutSpec.height)
        assertNull(toast.styleSpec.background.color)
        assertFalse(toast.isNodeCreated)
    }

    @Test
    fun `the toast factory fills its surface`() {
        val frame = Frame()
        lateinit var toast: Toast

        frame.layout {
            toast = toast()
        }

        assertEquals(100.percent, toast.layoutSpec.width)
        assertEquals(100.percent, toast.layoutSpec.height)
        assertFalse(toast.dimOutside)
        assertFalse(toast.isNodeCreated)
    }

    @Test
    fun `the toast factory forwards its defaults`() {
        val frame = Frame()
        lateinit var toast: Toast

        frame.layout {
            toast = toast(position = ToastPosition.CENTER, dimOutside = true)
        }

        assertEquals(ToastPosition.CENTER, toast.position)
        assertTrue(toast.dimOutside)
    }

    @Test
    fun `a tone is just a pair of colours`() {
        val tone = ToastTone(Color.web("#4c8bf5"), Color.WHITE)

        assertEquals(Color.web("#4c8bf5"), tone.background)
        assertEquals(Color.WHITE, tone.foreground)
    }

    @Test
    fun `reset keeps the layer transparent and leaves messages alone`() {
        val toast = Toast()
        toast.show("still here")

        toast.reset()

        assertNull(toast.styleSpec.background.color)
        assertTrue(toast.isShowing)
        assertEquals("still here", toast.message)
    }
}
