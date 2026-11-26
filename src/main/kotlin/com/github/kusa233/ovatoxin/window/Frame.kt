package com.github.kusa233.ovatoxin.window

import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.runtime.Fx
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle

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
 * Percentages are resolved against the current window size and re-resolved on resize. The user's
 * resize range can be bounded with [minWidth], [minHeight], [maxWidth] and [maxHeight].
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
    /** Whether the window shows a title bar; pass `false` for a borderless window. */
    titleBarEnabled: Boolean = true,
    /** The minimum window width in pixels while the user resizes it. */
    minWidth: Double = 0.0,
    /** The minimum window height in pixels while the user resizes it. */
    minHeight: Double = 0.0,
    /** The maximum window width in pixels while the user resizes it. */
    maxWidth: Double = Double.MAX_VALUE,
    /** The maximum window height in pixels while the user resizes it. */
    maxHeight: Double = Double.MAX_VALUE,
) : ComponentDrawable<Frame>() {

    private val defaultTitle: String = title
    private val defaultWidth: Double = width
    private val defaultHeight: Double = height
    private val defaultMinWidth: Double = minWidth
    private val defaultMinHeight: Double = minHeight
    private val defaultMaxWidth: Double = maxWidth
    private val defaultMaxHeight: Double = maxHeight

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

    /**
     * The minimum width the window may be resized to, in pixels.
     *
     * Defaults to `0`. Borderless windows still keep a small internal resize floor of 16 px, so
     * this value cannot make a window collapse to nothing.
     */
    var minWidth: Double = minWidth
        set(value) {
            field = value
            this.stage?.minWidth = value
        }

    /**
     * The minimum height the window may be resized to, in pixels.
     *
     * Defaults to `0`. Borderless windows still keep a small internal resize floor of 16 px, so
     * this value cannot make a window collapse to nothing.
     */
    var minHeight: Double = minHeight
        set(value) {
            field = value
            this.stage?.minHeight = value
        }

    /** The maximum width the window may be resized to, in pixels. `Double.MAX_VALUE` means unlimited. */
    var maxWidth: Double = maxWidth
        set(value) {
            field = value
            this.stage?.maxWidth = value
        }

    /** The maximum height the window may be resized to, in pixels. `Double.MAX_VALUE` means unlimited. */
    var maxHeight: Double = maxHeight
        set(value) {
            field = value
            this.stage?.maxHeight = value
        }

    /** The underlying stage, or `null` before the first [show]. */
    var stage: Stage? = null
        private set

    /**
     * Whether the window shows its title bar, as set by the constructor argument.
     *
     * Defaults to `true`. JavaFX fixes a window's decorations when it is first shown, so it can only
     * be chosen at construction: pass `titleBarEnabled = false` for a borderless window.
     */
    var titleBarEnabled: Boolean = titleBarEnabled
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
     * Restores the title and the size this frame was created with, plus the window surface colour,
     * and clears the [onClose] subscriptions.
     *
     * The size and the resize limits are part of the defaults, so a live window is resized back to
     * them; the listeners installed by [show] then keep the logical size in sync with the real one
     * as usual. The title bar setting is window configuration, not part of the drawn defaults, so
     * [titleBarEnabled] is left as it is.
     */
    override fun reset() {
        super.reset()
        this.title = this.defaultTitle
        this.width = this.defaultWidth
        this.height = this.defaultHeight
        this.minWidth = this.defaultMinWidth
        this.minHeight = this.defaultMinHeight
        this.maxWidth = this.defaultMaxWidth
        this.maxHeight = this.defaultMaxHeight
        this.styleSpec.background { color = WINDOW_BACKGROUND }
        this.styleSpec.corner = 0.px
        this.closeHandlers.clear()
    }

    override fun buildNode(): Node {
        val pane = Pane()
        this.root = pane
        return pane
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

            val stage = this.createStage()
            this.stage = stage
            // The pane is bound by Container.onNodeCreated, so children are already tracked.
            stage.show()
        }
    }

    /**
     * Builds a stage for this frame, matching the current [titleBarEnabled] and [title].
     *
     * A borderless stage loses the native resize handles, so [FramelessResizer] puts edge dragging
     * back. The caller owns the stage: it is stored in [stage] and shown.
     */
    private fun createStage(): Stage {
        val pane = this.root ?: (this.node as Pane)
        val stage = Stage()
        val scene = Scene(pane, this.width, this.height)
        // Native decorated windows honour these directly; FramelessResizer also reads them so an
        // undecorated window cannot be dragged past the same limits.
        stage.minWidth = this.minWidth
        stage.minHeight = this.minHeight
        stage.maxWidth = this.maxWidth
        stage.maxHeight = this.maxHeight
        if (!this.titleBarEnabled) {
            // A window's decorations are fixed once it is shown, so this must be decided here.
            stage.initStyle(StageStyle.UNDECORATED)
            // Undecorated windows have no native frame, hence no resize handles: add them back.
            stage.isResizable = true
            FramelessResizer.install(stage, scene)
        }
        stage.title = this.title
        stage.scene = scene

        // Keep the logical size in sync with the real one so percentages stay correct.
        stage.widthProperty().addListener { _, _, value -> this.width = value.toDouble() }
        stage.heightProperty().addListener { _, _, value -> this.height = value.toDouble() }

        // The title-bar close button asks the stage to close; hand that request to the
        // subscribers before JavaFX hides the window, so they can still read its state.
        stage.setOnCloseRequest { this.performCloseRequest() }

        return stage
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
