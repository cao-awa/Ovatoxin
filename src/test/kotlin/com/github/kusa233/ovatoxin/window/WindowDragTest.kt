package com.github.kusa233.ovatoxin.window

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for the drag arithmetic, without starting a window toolkit. */
class WindowDragTest {

    @Test
    fun `a drag adds the pointer delta to the window origin`() {
        assertEquals(
            WindowPosition(115.0, 80.0),
            draggedPosition(windowX = 100.0, windowY = 50.0, dx = 15.0, dy = 30.0),
        )
    }

    @Test
    fun `a zero drag keeps the window in place`() {
        assertEquals(
            WindowPosition(-20.5, 310.25),
            draggedPosition(windowX = -20.5, windowY = 310.25, dx = 0.0, dy = 0.0),
        )
    }

    @Test
    fun `a drag to the left and up subtracts from the origin`() {
        assertEquals(
            WindowPosition(70.0, 20.0),
            draggedPosition(windowX = 100.0, windowY = 50.0, dx = -30.0, dy = -30.0),
        )
    }
}
