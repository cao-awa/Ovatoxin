package com.github.kusa233.ovatoxin.window

import com.github.kusa233.ovatoxin.component.ComponentDrawable
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
 *
 * A frame is a drawing surface like a
 * [Div][com.github.kusa233.ovatoxin.widget.container.Div] or a
 * [FlexBox][com.github.kusa233.ovatoxin.widget.container.FlexBox], so `draw(...)` is available inside its
 * [layout][com.github.kusa233.ovatoxin.component.ComponentDrawable.layout] block — see
 * [ComponentDrawable].
 */
class Frame(
    /** The title bar text of the window. */
    title: String = DEFAULT_TITLE,
    /** The initial window width in pixels. */
    width: Double = DEFAULT_WIDTH,
    /** The initial window height in pixels. */
    height: Double = DEFAULT_HEIGHT,
) : ComponentDrawable<Frame>() {

    private val defaultTitle: String = title
    private val defaultWidth: Double = width
    private val defaultHeight: Double = height

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

    /** Every handler registered with [onClose], in the order they were added. */
    private val closeHandlers: MutableList<Frame.() -> Unit> = mutableListOf()

    /** Whether at least one [onClose] handler is registered. */
    val hasOnClose: Boolean get() = this.closeHandlers.isNotEmpty()

    init {
        // The window surface is its own colour; it is not a widget box.
        this.styleSpec.background { color = WINDOW_BACKGROUND }
        this.styleSpec.corner = 0.px
    }

    /**
     * Restores the title and the size this frame was created with, plus the window surface colour.
     *
     * The size is part of the defaults, so a live window is resized back to it; the listeners
     * installed by [show] then keep the logical size in sync with the real one as usual.
     */
    override fun reset() {
        super.reset()
        this.title = this.defaultTitle
        this.width = this.defaultWidth
        this.height = this.defaultHeight
        this.styleSpec.background { color = WINDOW_BACKGROUND }
        this.styleSpec.corner = 0.px
        this.closeHandlers.clear()
    }

    override fun buildNode(): Node {
        val pane = Pane()
        this.root = pane
        return pane
    }

    /** A frame cannot be styled as a box: it is the window, so its style is its background. */
    override fun applyStyle(width: Double, height: Double) {
        val pane = this.root ?: return
        pane.style = this.styleSpec.toCss(width, height, this.resolvedFontSize)
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

            // The title-bar close button asks the stage to close; hand that request to the
            // subscribers before JavaFX hides the window, so they can still read its state.
            stage.setOnCloseRequest { this.performCloseRequest() }

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

    /**
     * Subscribes [handler] to the window's close request, invoked when the user asks to close it.
     *
     * The request is the title-bar close button (or the platform equivalent); a programmatic
     * [close] does not raise it, so code that shuts the window down itself is not reported back to
     * its own handler. Handlers run before JavaFX hides the window, so [title], [width], [height]
     * and the component tree are all still live inside the block.
     *
     * Every call adds one more subscriber; all of them run, in the order they were added:
     *
     * ```kotlin
     * frame.onClose { println("bye from ${this.title}") }
     * ```
     *
     * The handler cannot veto the close: it observes the request, it does not consume it.
     *
     * @return this frame, so the call chains onto the window it registers on.
     */
    fun onClose(handler: Frame.() -> Unit): Frame {
        this.closeHandlers += handler
        return this
    }

    /**
     * Runs every [onClose] handler as if the user had asked to close the window.
     *
     * The backing stage calls this on a close request; tests call it directly.
     */
    fun performCloseRequest() {
        // Copy first: a handler is allowed to subscribe another one without a concurrent change.
        for (handler in this.closeHandlers.toList()) {
            handler(this)
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
