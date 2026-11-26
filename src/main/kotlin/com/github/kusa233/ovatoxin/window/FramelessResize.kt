package com.github.kusa233.ovatoxin.window

import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

/**
 * Which edge (or corner) of a borderless window the pointer is over.
 *
 * An undecorated JavaFX window has no native frame, so the resize handles a decorated window gets
 * for free are gone. The frame installs a [FramelessResizer] to bring them back.
 */
internal enum class FrameEdge {
    NONE,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

/** A window position and size, in screen pixels. */
internal data class WindowBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

/** How wide, in pixels, the draggable border around an undecorated window is. */
internal const val FRAME_RESIZE_MARGIN: Double = 5.0

/** The smallest width and height a resize may leave an undecorated window with. */
internal const val FRAME_MIN_SIZE: Double = 16.0

/**
 * Returns the edge or corner under a pointer position, or [FrameEdge.NONE] when it is in the body.
 *
 * The position is in scene coordinates: `(0, 0)` is the top-left corner of the client area, so the
 * right and bottom checks use the [width] and [height] of the scene.
 */
internal fun edgeAt(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    margin: Double = FRAME_RESIZE_MARGIN,
): FrameEdge {
    if (width <= 0.0 || height <= 0.0) return FrameEdge.NONE
    val left = x <= margin
    val right = x >= width - margin
    val top = y <= margin
    val bottom = y >= height - margin
    return when {
        top && left -> FrameEdge.TOP_LEFT
        top && right -> FrameEdge.TOP_RIGHT
        bottom && left -> FrameEdge.BOTTOM_LEFT
        bottom && right -> FrameEdge.BOTTOM_RIGHT
        left -> FrameEdge.LEFT
        right -> FrameEdge.RIGHT
        top -> FrameEdge.TOP
        bottom -> FrameEdge.BOTTOM
        else -> FrameEdge.NONE
    }
}

/**
 * Applies a resize drag to a window rectangle.
 *
 * [dx] and [dy] are the total pointer movement since the drag began, in screen pixels. Dragging a
 * left or top edge moves the window origin as well, so the opposite edge stays put; the window never
 * shrinks below [minWidth] / [minHeight] and never grows above [maxWidth] / [maxHeight].
 */
internal fun resizedBounds(
    edge: FrameEdge,
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    dx: Double,
    dy: Double,
    minWidth: Double = FRAME_MIN_SIZE,
    minHeight: Double = FRAME_MIN_SIZE,
    maxWidth: Double = Double.MAX_VALUE,
    maxHeight: Double = Double.MAX_VALUE,
): WindowBounds {
    var newX = x
    var newY = y
    var newWidth = width
    var newHeight = height

    val lowerWidth = minWidth.coerceAtLeast(0.0)
    val upperWidth = maxWidth.coerceAtLeast(lowerWidth)
    val lowerHeight = minHeight.coerceAtLeast(0.0)
    val upperHeight = maxHeight.coerceAtLeast(lowerHeight)

    val resizeLeft = edge == FrameEdge.LEFT || edge == FrameEdge.TOP_LEFT || edge == FrameEdge.BOTTOM_LEFT
    val resizeRight = edge == FrameEdge.RIGHT || edge == FrameEdge.TOP_RIGHT || edge == FrameEdge.BOTTOM_RIGHT
    val resizeTop = edge == FrameEdge.TOP || edge == FrameEdge.TOP_LEFT || edge == FrameEdge.TOP_RIGHT
    val resizeBottom = edge == FrameEdge.BOTTOM || edge == FrameEdge.BOTTOM_LEFT || edge == FrameEdge.BOTTOM_RIGHT

    if (resizeLeft) {
        newWidth = (width - dx).coerceIn(lowerWidth, upperWidth)
        // Move the origin with the edge, pinning the right edge, even once the width is clamped.
        newX = x + width - newWidth
    } else if (resizeRight) {
        newWidth = (width + dx).coerceIn(lowerWidth, upperWidth)
    }

    if (resizeTop) {
        newHeight = (height - dy).coerceIn(lowerHeight, upperHeight)
        newY = y + height - newHeight
    } else if (resizeBottom) {
        newHeight = (height + dy).coerceIn(lowerHeight, upperHeight)
    }

    return WindowBounds(newX, newY, newWidth, newHeight)
}

/** The pointer shape for an edge or corner. */
internal fun cursorFor(edge: FrameEdge): Cursor = when (edge) {
    FrameEdge.NONE -> Cursor.DEFAULT
    FrameEdge.TOP -> Cursor.N_RESIZE
    FrameEdge.BOTTOM -> Cursor.S_RESIZE
    FrameEdge.LEFT -> Cursor.W_RESIZE
    FrameEdge.RIGHT -> Cursor.E_RESIZE
    FrameEdge.TOP_LEFT -> Cursor.NW_RESIZE
    FrameEdge.TOP_RIGHT -> Cursor.NE_RESIZE
    FrameEdge.BOTTOM_LEFT -> Cursor.SW_RESIZE
    FrameEdge.BOTTOM_RIGHT -> Cursor.SE_RESIZE
}

/**
 * Adds mouse-drag resizing to an undecorated window.
 *
 * The handlers are event *filters* on the scene, so they see the pointer before a child consumes it:
 * hovering an edge updates the cursor, and pressing within the margin starts a resize that outlives
 * the pointer leaving the window (JavaFX keeps delivering the drag to the grab).
 */
internal object FramelessResizer {

    fun install(stage: Stage, scene: Scene) {
        val drag = Drag()

        scene.addEventFilter(MouseEvent.MOUSE_MOVED) { event ->
            scene.cursor = cursorFor(edgeAt(event.sceneX, event.sceneY, scene.width, scene.height))
        }
        scene.addEventFilter(MouseEvent.MOUSE_EXITED) { scene.cursor = Cursor.DEFAULT }

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            if (event.button != MouseButton.PRIMARY) return@addEventFilter
            val edge = edgeAt(event.sceneX, event.sceneY, scene.width, scene.height)
            if (edge == FrameEdge.NONE) return@addEventFilter
            drag.start(edge, event.screenX, event.screenY, stage)
            event.consume()
        }

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            if (!drag.active) return@addEventFilter
            val bounds = resizedBounds(
                edge = drag.edge,
                x = drag.x,
                y = drag.y,
                width = drag.width,
                height = drag.height,
                dx = event.screenX - drag.pressX,
                dy = event.screenY - drag.pressY,
                minWidth = maxOf(stage.minWidth, FRAME_MIN_SIZE),
                minHeight = maxOf(stage.minHeight, FRAME_MIN_SIZE),
                maxWidth = stage.maxWidth,
                maxHeight = stage.maxHeight,
            )
            stage.x = bounds.x
            stage.y = bounds.y
            stage.width = bounds.width
            stage.height = bounds.height
            event.consume()
        }

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED) { event ->
            if (!drag.active) return@addEventFilter
            drag.active = false
            event.consume()
        }
    }

    /** The rectangle and pointer position captured when a resize drag begins. */
    private class Drag {
        var active: Boolean = false
        var edge: FrameEdge = FrameEdge.NONE
        var x: Double = 0.0
        var y: Double = 0.0
        var width: Double = 0.0
        var height: Double = 0.0
        var pressX: Double = 0.0
        var pressY: Double = 0.0

        fun start(edge: FrameEdge, pressX: Double, pressY: Double, stage: Stage) {
            this.active = true
            this.edge = edge
            this.x = stage.x
            this.y = stage.y
            this.width = stage.width
            this.height = stage.height
            this.pressX = pressX
            this.pressY = pressY
        }
    }
}
