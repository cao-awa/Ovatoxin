package com.github.kusa233.ovatoxin.window

import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.runtime.Fx
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage

/**
 * The root container of a component tree, backed by a JavaFX [Stage].
 *
 * ```kotlin
 * val frame = Frame()
 * frame.layout {
 *     draw(TextArea()) { ... }
 * }
 * frame.show()
 * ```
 *
 * The size of the frame is the reference size for percentage based layouts of its children.
 * Percentages are resolved against the current window size and re-resolved on resize.
 */
class Frame(
    /** The title bar text of the window. */
    title: String = DEFAULT_TITLE,
    /** The initial window width in pixels. */
    width: Double = DEFAULT_WIDTH,
    /** The initial window height in pixels. */
    height: Double = DEFAULT_HEIGHT,
) : Container<Frame>() {

    /** The title bar text of the window. */
    var title: String = title
        set(value) {
            field = value
            this.stage?.title = value
        }

    /** The window width in pixels, used as basis for horizontal percentages. */
    var width: Double = width
        set(value) {
            field = value
            this.stage?.width = value
        }

    /** The window height in pixels, used as basis for vertical percentages. */
    var height: Double = height
        set(value) {
            field = value
            this.stage?.height = value
        }

    /** The underlying stage, or `null` before the first [show]. */
    var stage: Stage? = null
        private set

    private var root: Pane? = null

    init {
        // The window surface is its own colour; it is not a widget box.
        this.styleSpec.background { color = WINDOW_BACKGROUND }
        this.styleSpec.corner = 0.px
    }

    override fun buildNode(): Node {
        val pane = Pane()
        this.root = pane
        return pane
    }

    /** A frame cannot be styled as a box: it is the window, so its style is its background. */
    override fun applyStyle(width: Double, height: Double) {
        val pane = this.root ?: return
        pane.style = this.styleSpec.toCss(width, height)
    }

    /**
     * Shows the window, starting the JavaFX runtime if necessary.
     *
     * The frame stays interactive until the window is closed, at which point the JavaFX
     * runtime stops and the JVM terminates.
     */
    fun show() {
        Fx.runOnFxThread {
            val existing = this.stage
            if (existing != null) {
                existing.show()
                existing.toFront()
                return@runOnFxThread
            }

            val pane = this.root ?: (this.node as Pane)
            val stage = Stage()
            stage.title = this.title
            stage.scene = Scene(pane, this.width, this.height)

            // Keep the logical size in sync with the real one so percentages stay correct.
            stage.widthProperty().addListener { _, _, value -> this.width = value.toDouble() }
            stage.heightProperty().addListener { _, _, value -> this.height = value.toDouble() }

            this.stage = stage
            // The pane is bound by Container.onNodeCreated, so children are already tracked.
            stage.show()
        }
    }

    /** Hides the window without stopping the JavaFX runtime. */
    fun hide() {
        Fx.runOnFxThread { this.stage?.hide() }
    }

    /** Closes the window; the runtime stops once the last window is gone. */
    fun close() {
        Fx.runOnFxThread {
            this.stage?.close()
            this.stage = null
        }
    }

    companion object {
        /** The default title of a new frame. */
        const val DEFAULT_TITLE: String = "Ovatoxin"

        /** The default width of a new frame. */
        const val DEFAULT_WIDTH: Double = 800.0

        /** The default height of a new frame. */
        const val DEFAULT_HEIGHT: Double = 600.0

        /** The colour of the window surface behind the components. */
        val WINDOW_BACKGROUND: Color = Color.web("#f7f7f8")
    }
}
