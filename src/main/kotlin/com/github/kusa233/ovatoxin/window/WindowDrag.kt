package com.github.kusa233.ovatoxin.window

import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

/** A window origin on the screen, in pixels. */
internal data class WindowPosition(
    val x: Double,
    val y: Double,
)

/**
 * Returns the window origin after the pointer moved by [dx] and [dy] from the press position.
 *
 * The offsets are measured in screen pixels, the same coordinate space as a JavaFX [Stage]'s
 * `x`/`y` properties.
 */
internal fun draggedPosition(
    windowX: Double,
    windowY: Double,
    dx: Double,
    dy: Double,
): WindowPosition = WindowPosition(windowX + dx, windowY + dy)

/**
 * Moves the containing window when [surface] itself is pressed and dragged.
 *
 * This turns a painted background into a title-bar-like drag handle without disturbing the controls
 * drawn on top of it: only a press whose [MouseEvent.target] is [surface] starts a drag, so a
 * tab button, scroll bar, or other child keeps its normal click, scroll and drag behaviour. The
 * handlers are event filters, so the drag can take precedence over a scroll pane's drag-to-pan on
 * that same empty background.
 *
 * [enabled] is read at every press rather than at installation time, so the owner can turn window
 * dragging on and off after the node has already been built.
 */
internal object WindowDragger {

    fun install(surface: Node, enabled: () -> Boolean) {
        val drag = Drag()

        surface.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            if (!enabled() || event.button != MouseButton.PRIMARY || event.target !== surface) {
                return@addEventFilter
            }
            val stage = surface.scene?.window as? Stage ?: return@addEventFilter
            drag.start(stage, event.screenX, event.screenY)
            event.consume()
        }

        surface.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            if (!drag.active) return@addEventFilter
            val window = drag.stage ?: return@addEventFilter
            val position = draggedPosition(
                windowX = drag.windowX,
                windowY = drag.windowY,
                dx = event.screenX - drag.pressX,
                dy = event.screenY - drag.pressY,
            )
            window.x = position.x
            window.y = position.y
            event.consume()
        }

        surface.addEventFilter(MouseEvent.MOUSE_RELEASED) { event ->
            if (!drag.active) return@addEventFilter
            drag.active = false
            event.consume()
        }
    }

    /** The window position and pointer position captured when a drag begins. */
    private class Drag {
        var active: Boolean = false
        var stage: Stage? = null
        var windowX: Double = 0.0
        var windowY: Double = 0.0
        var pressX: Double = 0.0
        var pressY: Double = 0.0

        fun start(stage: Stage, pressX: Double, pressY: Double) {
            this.active = true
            this.stage = stage
            this.windowX = stage.x
            this.windowY = stage.y
            this.pressX = pressX
            this.pressY = pressY
        }
    }
}
