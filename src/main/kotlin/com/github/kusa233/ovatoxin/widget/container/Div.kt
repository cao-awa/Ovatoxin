package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.style.StyleVisual
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
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

    private var scrollYValue: Boolean = false

    private var scrollSurface: ScrollSurface? = null

    /**
     * Whether the content scrolls up and down inside the box.
     *
     * It is off by default, so a [Div] keeps its absolute placement and its children simply clip at
     * the edge. Turn it on before the component is rendered: a scrolling [Div] needs a different
     * JavaFX node (a scroll pane around its content), and swapping that node after it exists is not
     * possible.
     *
     * When enabled, the box keeps its declared height as the visible viewport. Percentages inside it
     * resolve against that viewport, the content scrolls vertically only, and the rounded clip still
     * follows the outer edge.
     */
    var scrollY: Boolean
        get() = this.scrollYValue
        set(value) {
            check(this.builtNode == null) {
                "scrollY must be set before the Div is rendered"
            }
            this.scrollYValue = value
        }

    init {
        // An area frames a region instead of being painted, so it starts out fully transparent.
        this.styleSpec.background { color = null }
    }

    override fun buildNode(): Node {
        if (this.scrollYValue) {
            val content = ScrollContentPane()
            val surface = ScrollSurface(this, content)
            this.scrollSurface = surface
            return surface.build()
        }
        val pane = Pane()
        // A Div has no background of its own; its bounds come from the layout, not its content.
        pane.isPickOnBounds = true
        return pane
    }

    override fun onNodeCreated(node: Node) {
        val surface = this.scrollSurface
        if (surface != null && node is ScrollPane) {
            // The scroll pane is only the frame; the children still belong to the content pane.
            attachContent(surface.content)
        } else {
            super.onNodeCreated(node)
        }
        // A percentage corner depends on the box size, so the clip follows every resize.
        (node as? Region)?.let { region ->
            region.widthProperty().addListener { _, _, _ -> applyClip() }
            region.heightProperty().addListener { _, _, _ -> applyClip() }
        }
        applyClip()
    }

    /**
     * The height a percentage or an `em` resolves against: the visible viewport when scrolling, the
     * whole box otherwise. Without this a `50.percent` child would size against the full scroll
     * extent rather than against the area the user can actually see.
     */
    internal override fun contentBox(width: Double, height: Double, fontSize: Double): ContentBox =
        super.contentBox(width, visibleHeight(height), fontSize)

    /** Font percentages are also taken of the visible viewport while scrolling. */
    internal override fun fontSizeForHeight(height: Double?, inherited: Double): Double =
        super.fontSizeForHeight(height?.let { visibleHeight(it) }, inherited)

    override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, visibleHeight(height))
        applyClip()
    }

    /** Keeps the clip outline in step with an animated corner radius. */
    internal override fun onStyleFrame(visual: StyleVisual) {
        super.onStyleFrame(visual)
        applyClip(visual.corner)
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

    /** Measures how far the placed children reach, so a scrolling Div can grow its content pane. */
    internal override fun afterGeometry(host: Pane, box: ContentBox) {
        val surface = this.scrollSurface ?: return
        if (!this.scrollYValue) return
        var bottom = box.offsetY
        for (child in this.children) {
            if (!child.visible) continue
            val node = child.node
            val nodeBottom = if (node is Region) {
                node.layoutY + node.height
            } else {
                node.layoutY + node.layoutBounds.height
            }
            if (nodeBottom > bottom) bottom = nodeBottom
        }
        surface.setExtent(bottom + box.offsetY)
    }

    /** The visible height of a scrolling box, or [height] when it does not scroll. */
    private fun visibleHeight(height: Double): Double {
        val surface = this.scrollSurface ?: return height
        return if (this.scrollYValue && surface.viewportHeight > 0.0) surface.viewportHeight else height
    }

    /** Keeps the clip rectangle exactly as large as the box itself. */
    private fun applyClip() {
        val region = this.builtNode as? Region ?: return
        applyClip(this.styleSpec.cornerPixels(region.width, region.height, this.resolvedFontSize))
    }

    /** Applies a clip with the given (already resolved) corner radius. */
    private fun applyClip(corner: Double) {
        val region = this.builtNode as? Region ?: return
        if (!this.clipValue) {
            region.clip = null
            return
        }

        // Reuse one rectangle across toggles instead of rebinding a new one every time.
        val bounds = this.clipBounds ?: Rectangle().also { rectangle ->
            rectangle.widthProperty().bind(region.widthProperty())
            rectangle.heightProperty().bind(region.heightProperty())
            this.clipBounds = rectangle
        }

        // `arcWidth` is a diameter, hence the doubling of the pixel radius.
        val radius = corner * 2.0
        bounds.arcWidth = radius
        bounds.arcHeight = radius
        region.clip = bounds
    }
}
