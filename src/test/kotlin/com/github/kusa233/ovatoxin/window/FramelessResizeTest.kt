package com.github.kusa233.ovatoxin.window

import kotlin.test.Test
import kotlin.test.assertEquals

class FramelessResizeTest {

    private val width = 400.0
    private val height = 300.0

    @Test
    fun `the pointer in the body is over no edge`() {
        assertEquals(FrameEdge.NONE, edgeAt(200.0, 150.0, width, height))
    }

    @Test
    fun `the pointer on each side is over that edge`() {
        assertEquals(FrameEdge.LEFT, edgeAt(2.0, 150.0, width, height))
        assertEquals(FrameEdge.RIGHT, edgeAt(398.0, 150.0, width, height))
        assertEquals(FrameEdge.TOP, edgeAt(200.0, 2.0, width, height))
        assertEquals(FrameEdge.BOTTOM, edgeAt(200.0, 298.0, width, height))
    }

    @Test
    fun `the pointer on a corner is over that corner`() {
        assertEquals(FrameEdge.TOP_LEFT, edgeAt(2.0, 2.0, width, height))
        assertEquals(FrameEdge.TOP_RIGHT, edgeAt(398.0, 2.0, width, height))
        assertEquals(FrameEdge.BOTTOM_LEFT, edgeAt(2.0, 298.0, width, height))
        assertEquals(FrameEdge.BOTTOM_RIGHT, edgeAt(398.0, 298.0, width, height))
    }

    @Test
    fun `a collapsed scene has no edges`() {
        assertEquals(FrameEdge.NONE, edgeAt(0.0, 0.0, 0.0, 0.0))
    }

    @Test
    fun `dragging the right edge grows the width`() {
        val bounds = resizedBounds(FrameEdge.RIGHT, 10.0, 20.0, width, height, dx = 50.0, dy = 0.0)

        assertEquals(WindowBounds(10.0, 20.0, 450.0, height), bounds)
    }

    @Test
    fun `dragging the left edge shrinks the width and moves the origin`() {
        val bounds = resizedBounds(FrameEdge.LEFT, 10.0, 20.0, width, height, dx = 40.0, dy = 0.0)

        assertEquals(WindowBounds(50.0, 20.0, 360.0, height), bounds)
    }

    @Test
    fun `dragging the top edge shrinks the height and moves the origin`() {
        val bounds = resizedBounds(FrameEdge.TOP, 10.0, 20.0, width, height, dx = 0.0, dy = 30.0)

        assertEquals(WindowBounds(10.0, 50.0, width, 270.0), bounds)
    }

    @Test
    fun `dragging the bottom edge grows the height`() {
        val bounds = resizedBounds(FrameEdge.BOTTOM, 10.0, 20.0, width, height, dx = 0.0, dy = 30.0)

        assertEquals(WindowBounds(10.0, 20.0, width, 330.0), bounds)
    }

    @Test
    fun `dragging a corner resizes both axes`() {
        val bounds = resizedBounds(FrameEdge.TOP_LEFT, 10.0, 20.0, width, height, dx = 25.0, dy = 15.0)

        assertEquals(WindowBounds(35.0, 35.0, 375.0, 285.0), bounds)
    }

    @Test
    fun `a left resize keeps the right edge pinned when clamped to the minimum`() {
        val bounds = resizedBounds(
            FrameEdge.LEFT, 10.0, 20.0, width, height,
            dx = 1000.0, dy = 0.0, minWidth = 100.0,
        )

        assertEquals(100.0, bounds.width)
        assertEquals(10.0 + width - 100.0, bounds.x)
    }

    @Test
    fun `a top resize keeps the bottom edge pinned when clamped to the minimum`() {
        val bounds = resizedBounds(
            FrameEdge.TOP, 10.0, 20.0, width, height,
            dx = 0.0, dy = 1000.0, minHeight = 80.0,
        )

        assertEquals(80.0, bounds.height)
        assertEquals(20.0 + height - 80.0, bounds.y)
    }

    @Test
    fun `a drag on no edge leaves the bounds untouched`() {
        val bounds = resizedBounds(FrameEdge.NONE, 10.0, 20.0, width, height, dx = 55.0, dy = 66.0)

        assertEquals(WindowBounds(10.0, 20.0, width, height), bounds)
    }
}
