package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.ComponentDrawable
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle

/**
 * A plain box that frames a render region.
 *
 * `Div` is an *area*, not a widget: it draws no fill of its own (set `background` in a [style]
 * block if you want one), but it
 *
 * 1. **rescopes percentages** — everything drawn inside resolves its `x`/`width` against the
 *    `Div`, not against the window, so moving the box moves its whole content;
 * 2. **clips its content** — anything a child places outside the box is not rendered, which is
 *    what makes it usable as a viewport or a masked area.
 *
 * ```kotlin
 * val frame = Frame()
 * frame.layout {
 *     val panel = draw(Div()) { outer ->
 *         layout {
 *             x = 10.percent
 *             y = 20.percent
 *             width = 80.percent
 *             height = 50.percent
 *         }
 *         outer.layout {
 *             // 50% here is half of the panel, not half of the window.
 *             draw(TextArea()) { text ->
 *                 layout { x = 50.percent; width = 50.percent }
 *                 text.text("inside the panel")
 *             }
 *         }
 *     }
 * }
 * frame.show()
 * ```
 *
 * The inner block is reached through the [layout][com.github.kusa233.ovatoxin.component.ComponentDrawable.layout]
 * of the panel (`outer.layout { ... }`); the outer `layout { ... }` passed to `draw` always
 * configures the panel's own geometry, as in the rest of the DSL.
 *
 * A non-zero [corner][com.github.kusa233.ovatoxin.style.Style.corner] rounds the clipped region
 * as well, so the content is cut along the same outline as the fill.
 *
 * A panel is a drawing surface like the window and a [FlexBox], so a `Div` can be nested in a
 * `Div` — see [ComponentDrawable]. It places its children at absolute positions; when they should
 * flow instead, draw a [FlexBox] into it and let that one arrange them.
 */
class Div : ComponentDrawable<Div>() {

    private var clipValue: Boolean = true

    /** Whether content outside the box is clipped. Defaults to `true`. */
    var clip: Boolean
        get() = this.clipValue
        set(value) {
            this.clipValue = value
            applyClip()
        }

    private var clipBounds: Rectangle? = null

    init {
        // An area frames a region instead of being painted, so it starts out fully transparent.
        this.styleSpec.background { color = null }
    }

    override fun buildNode(): Node {
        val pane = Pane()
        // A Div has no background of its own; its bounds come from the layout, not its content.
        pane.isPickOnBounds = true
        return pane
    }

    override fun onNodeCreated(node: Node) {
        super.onNodeCreated(node)
        // A percentage corner depends on the box size, so the clip follows every resize.
        (node as Pane).widthProperty().addListener { _, _, _ -> applyClip() }
        node.heightProperty().addListener { _, _, _ -> applyClip() }
        applyClip()
    }

    override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        applyClip()
    }

    /**
     * Restores the default of a bare area: clipping on, and a fully transparent fill.
     *
     * The transparent fill is the seed set in `init`, so it survives a reset — a `Div` still
     * frames its region instead of suddenly painting the default white.
     */
    override fun reset() {
        super.reset()
        this.clipValue = true
        this.styleSpec.background { color = null }
        applyClip()
    }

    /** Keeps the clip rectangle exactly as large as the box itself. */
    private fun applyClip() {
        val pane = this.builtNode as? Pane ?: return
        if (!this.clipValue) {
            pane.clip = null
            return
        }

        // Reuse one rectangle across toggles instead of rebinding a new one every time.
        val bounds = this.clipBounds ?: Rectangle().also { rectangle ->
            rectangle.widthProperty().bind(pane.widthProperty())
            rectangle.heightProperty().bind(pane.heightProperty())
            this.clipBounds = rectangle
        }

        // `arcWidth` is a diameter, hence the doubling of the pixel radius.
        val radius = this.styleSpec.cornerPixels(pane.width, pane.height) * 2.0
        bounds.arcWidth = radius
        bounds.arcHeight = radius
        pane.clip = bounds
    }
}
