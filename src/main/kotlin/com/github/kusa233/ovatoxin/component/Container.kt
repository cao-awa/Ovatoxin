package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.Pixel
import com.github.kusa233.ovatoxin.runtime.Fx
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

/**
 * Base class of every component that can hold children.
 *
 * A container owns the children of a region and the [Layout] that places them; it does not by
 * itself offer a way to declare them. Children are declared inside the `layout` block of a
 * drawing surface — [com.github.kusa233.ovatoxin.component.ComponentDrawable], inherited by
 * [com.github.kusa233.ovatoxin.window.Frame], [com.github.kusa233.ovatoxin.widget.container.Div] and
 * [com.github.kusa233.ovatoxin.widget.container.FlexBox] — with
 * [com.github.kusa233.ovatoxin.component.SurfaceScope.draw]. This class therefore holds everything
 * a container needs *after* a child exists — the child list, the layout pass and the repaint of a
 * single child — while the act of drawing is confined to [ComponentDrawable].
 *
 * Geometry is expressed by a [Layout] whose slots each take a percentage of the container size or
 * an absolute pixel count. Layouts are re-resolved whenever the container is resized.
 *
 * Percentages resolve against [contentBox] — the container itself, cut down by the `hPadding` and
 * `vPadding` its [Layout] declares. For a container without padding that is simply its own size;
 * for one padded by `10.px` it is twenty pixels narrower, and the content starts ten pixels in.
 * Nesting a [com.github.kusa233.ovatoxin.widget.container.Div] therefore rescopes percentages for everything
 * inside, and so does padding.
 *
 * A plain container gives every child the origin of its content box, so a child lands where its own
 * `x` and `y` say. A flowing container is free to hand out a slot instead — see [slotFor], which
 * [com.github.kusa233.ovatoxin.widget.container.FlexBox] overrides to lay its children out in a line.
 *
 * The class is marked with [OvatoxinDsl], so a nested surface's `layout` block sees only the scope
 * it declares children for, never the one around it: inside `panel.layout { }` a member that
 * belongs to the enclosing `Frame` — its `title`, for instance — does not resolve, and the
 * children are declared for `panel` and not by accident for the window.
 *
 * @param T the concrete subclass, so the block entry point knows the exact type it was opened for.
 */
@OvatoxinDsl
abstract class Container<T : Container<T>> : Component<T>() {

    private val mutableChildren = mutableListOf<Component<*>>()

    /** All children in drawing order (bottom to top). */
    val children: List<Component<*>> get() = this.mutableChildren

    private var pane: Pane? = null

    /** The pane hosting the children, or `null` while the container is not rendered. */
    internal val contentPane: Pane? get() = this.pane

    /**
     * The area children may be placed into, for a container of [width] by [height] pixels.
     *
     * It is the box of the container itself, cut down by the `hPadding` and `vPadding` of its
     * [layoutSpec] — the inset a container keeps around its children. An inset is symmetric, so
     * the room left over shrinks by the padding twice, and percentages of a child resolve against
     * what is left: a `100.percent` child of a box padded by `10.px` is twenty pixels narrower than
     * the box instead of running over its edge.
     *
     * Padding larger than half the box would leave no room at all — and, unresolved, would put the
     * content outside the container — so an inset is capped at half the box. A container padded that
     * far therefore has an empty content box in its middle rather than a negative one.
     *
     * A padding written in `em` resolves against [fontSize], the size of *this* container by
     * default: the inset belongs to the container, so it follows the container's font and not the
     * font of whichever child happens to sit next to it.
     */
    internal open fun contentBox(
        width: Double,
        height: Double,
        fontSize: Double = this.resolvedFontSize,
    ): ContentBox {
        val insetX = this.layoutSpec.hPadding.resolve(width, fontSize).coerceIn(0.0, width / 2.0)
        val insetY = this.layoutSpec.vPadding.resolve(height, fontSize).coerceIn(0.0, height / 2.0)
        return ContentBox(
            offsetX = insetX,
            offsetY = insetY,
            width = width - insetX * 2.0,
            height = height - insetY * 2.0,
        )
    }

    /**
     * The slot [child] is placed into, given the [box] its container offers.
     *
     * The default is the origin of the box, so a child of a plain container is positioned exactly
     * as its own `x` and `y` describe. A flowing container overrides this to hand out a computed
     * slot and thereby joins the flow; the child's own offsets still add onto it.
     */
    internal open fun slotFor(child: Component<*>, box: ContentBox): Slot =
        Slot(box.offsetX, box.offsetY)

    /**
     * Runs [block] as one child-declaring pass and lays the container out afterwards.
     *
     * The caller is [ComponentDrawable.layout], which is the only place a block reaches this:
     * declaring children is a property of a drawing surface, not of every container.
     *
     * A replayed block draws its children again, so the containment is registered here as well —
     * a container whose new block draws fewer children than before still has to be trimmed.
     */
    internal fun declareChildren(block: () -> Unit) {
        ReplayScope.current?.begin(this)
        block()
        relayout()
    }

    /** Appends [component] to the child list; [SurfaceScope.draw] sets its [parent] first. */
    internal fun attach(component: Component<*>) {
        this.mutableChildren += component
    }

    /** The child in slot [index], or `null` when the slot is empty. */
    internal fun childAt(index: Int): Component<*>? = this.mutableChildren.getOrNull(index)

    /** Drops every child from [count] on, used when a replayed block draws fewer children. */
    internal fun trimTo(count: Int) {
        while (this.mutableChildren.size > count) {
            val dropped = this.mutableChildren.removeAt(count)
            dropped.parent = null
            dropped.detachNode()
        }
    }

    /** Removes [component] from this container. */
    fun remove(component: Component<*>) {
        if (this.mutableChildren.remove(component)) {
            component.parent = null
            component.detachNode()
            relayout()
        }
    }

    /** Re-applies the geometry of every child. */
    internal fun relayout() {
        val host = this.pane ?: return
        if (Platform.isFxApplicationThread()) {
            applyGeometry(host)
        } else {
            Fx.runOnFxThread { applyGeometry(host) }
        }
    }

    /**
     * Lays this container out again after its font size changed, and then its own box.
     *
     * The order matters: the children inherit the font size this container now has, so they have to
     * be placed with their new `em`s — which is a pass over *this* container's pane. Only then does
     * the container itself have to move, since the `em`s it is placed with may have moved too.
     */
    internal override fun relayoutAfterStyleChange() {
        relayout()
        this.parent?.relayout()
    }

    /**
     * Re-applies the geometry of [component] alone, leaving every sibling untouched.
     *
     * This is what a [com.github.kusa233.ovatoxin.state.State] uses when its value changes: a
     * single child is re-placed and re-styled, instead of the whole subtree.
     */
    internal fun repaint(component: Component<*>) {
        val host = this.pane ?: return
        Fx.runOnFxThread { applyGeometry(host, component) }
    }

    /**
     * Resolves [spec] against the width and height of the content box and moves [node] accordingly.
     *
     * [offsetX] and [offsetY] are added to the resolved position, which is how a padded surface
     * keeps its children inside the inset box and how a flow hands over the slot it computed:
     * the `x` of a child is a position *relative to* that offset, never an absolute one.
     *
     * The relative units of [spec] resolve against the box being placed into; an `em` resolves
     * against [fontSize], since the offsets and the size belong to the child being placed and not to
     * the box around it.
     */
    internal fun place(
        node: Node,
        spec: Layout,
        width: Double,
        height: Double,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        fontSize: Double = this.resolvedFontSize,
    ) {
        val resolvedX = offsetX + spec.x.resolve(width, fontSize)
        val resolvedY = offsetY + spec.y.resolve(height, fontSize)
        val resolvedWidth = spec.width?.resolve(width, fontSize)
        val resolvedHeight = spec.height?.resolve(height, fontSize)
        node.autosize()

        if (node is Region) {
            val fallbackWidth = node.prefWidth(-1.0)
            node.resizeRelocate(
                resolvedX,
                resolvedY,
                resolvedWidth ?: fallbackWidth,
                resolvedHeight ?: node.prefHeight(resolvedWidth ?: fallbackWidth),
            )
        } else {
            node.layoutX = resolvedX
            node.layoutY = resolvedY
        }
    }

    /** Binds the JavaFX pane of this container and starts tracking its size. */
    internal fun attachContent(target: Pane) {
        this.pane = target
        target.widthProperty().addListener { _, _, _ -> applyGeometry(target) }
        target.heightProperty().addListener { _, _, _ -> applyGeometry(target) }
        applyGeometry(target)
    }

    /** A container binds the pane it created, so subclasses get nesting for free. */
    override fun onNodeCreated(node: Node) {
        if (node is Pane && this.pane == null) attachContent(node)
    }

    /**
     * Places every child onto [host], adding missing nodes to the pane.
     *
     * With [only] set, exactly that one child is placed and styled and its siblings are left as
     * they are — a partial pass used to redraw a single component.
     *
     * A flow is asked for its layout once per child, because its lines depend on the widths the
     * children resolve to. [beginPass] lets it compute that layout before the first child is
     * placed, and [slotFor] then answers with a slot it already knows.
     */
    private fun applyGeometry(host: Pane, only: Component<*>? = null) {
        // The container resolves its own font first: a percentage font size needs the box height,
        // and every child inherits whatever this resolves to.
        val selfFont = resolveFontFor(host.height)
        val box = contentBox(host.width, host.height, selfFont)

        // The container styles itself against its own box; a percentage corner needs that size.
        applyStyle(host.width, host.height)

        val targets = if (only == null) this.mutableChildren.toList() else listOf(only)
        beginPass(box, targets)
        for (child in targets) {
            val node = child.node
            if (node.parent == null) host.children += node
            node.isVisible = child.visible
            node.isManaged = false
            val slot = slotFor(child, box)
            place(
                node,
                child.layoutSpec,
                box.width,
                box.height,
                slot.x,
                slot.y,
                childFont(child, box, selfFont),
            )
            child.applyStyle(childBoxWidth(node), childBoxHeight(node))
        }
    }

    /**
     * The font size [child] resolves inside [box], inheriting [inherited].
     *
     * A percentage font size is taken of the child's own box height, so the child's declared height
     * is resolved here — without the font, since only a pixel or percentage height is font
     * independent. An `auto` or `em` height stays unknown and the percentage falls back to
     * [inherited]; the child's real box, once placed, carries the right size into [Component.applyStyle]
     * anyway.
     */
    private fun childFont(child: Component<*>, box: ContentBox, inherited: Double): Double {
        val height = when (val declared = child.layoutSpec.height) {
            is Pixel -> declared.value
            is Percent -> box.height * declared.value / 100.0
            else -> null
        }
        return child.fontSizeForHeight(height, inherited)
    }

    /**
     * Gives a flow the chance to lay itself out before its children are placed, one child at a
     * time. A plain container has nothing to prepare, since every slot is the origin of the box.
     */
    internal open fun beginPass(box: ContentBox, targets: List<Component<*>>) = Unit

    /** The width a child actually occupies after [place], used to resolve its corner radius. */
    private fun childBoxWidth(node: Node): Double =
        if (node is Region) node.width else node.layoutBounds.width

    /** The height a child actually occupies after [place], used to resolve its corner radius. */
    private fun childBoxHeight(node: Node): Double =
        if (node is Region) node.height else node.layoutBounds.height

    override fun toString(): String =
        "${javaClass.simpleName}(children=${this.mutableChildren.size}, ${this.layoutSpec})"
}
