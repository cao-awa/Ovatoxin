package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Layout
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
 * [com.github.kusa233.ovatoxin.window.Frame] and [com.github.kusa233.ovatoxin.widget.Div] — with
 * [com.github.kusa233.ovatoxin.component.SurfaceScope.draw]. This class therefore holds everything
 * a container needs *after* a child exists — the child list, the layout pass and the repaint of a
 * single child — while the act of drawing is confined to [ComponentDrawable].
 *
 * Geometry is expressed by a [Layout] whose slots each take a percentage of the container size or
 * an absolute pixel count. Layouts are re-resolved whenever the container is resized.
 *
 * Percentages always resolve against the container itself, not against the window, so nesting
 * a [com.github.kusa233.ovatoxin.widget.Div] rescopes them for everything inside.
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
     * Re-applies the geometry of [component] alone, leaving every sibling untouched.
     *
     * This is what a [com.github.kusa233.ovatoxin.state.State] uses when its value changes: a
     * single child is re-placed and re-styled, instead of the whole subtree.
     */
    internal fun repaint(component: Component<*>) {
        val host = this.pane ?: return
        Fx.runOnFxThread { applyGeometry(host, component) }
    }

    /** Resolves [spec] against the size of this container and moves [node] accordingly. */
    internal fun place(node: Node, spec: Layout, width: Double, height: Double) {
        val resolvedX = spec.x.resolve(width)
        val resolvedY = spec.y.resolve(height)
        val resolvedWidth = spec.width?.resolve(width)
        val resolvedHeight = spec.height?.resolve(height)
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
     */
    private fun applyGeometry(host: Pane, only: Component<*>? = null) {
        val width = host.width
        val height = host.height

        // The container styles itself against its own box; a percentage corner needs that size.
        applyStyle(width, height)

        val targets = if (only == null) this.mutableChildren.toList() else listOf(only)
        for (child in targets) {
            val node = child.node
            if (node.parent == null) host.children += node
            node.isVisible = child.visible
            node.isManaged = false
            place(node, child.layoutSpec, width, height)
            child.applyStyle(childBoxWidth(node), childBoxHeight(node))
        }
    }

    /** The width a child actually occupies after [place], used to resolve its corner radius. */
    private fun childBoxWidth(node: Node): Double =
        if (node is Region) node.width else node.layoutBounds.width

    /** The height a child actually occupies after [place], used to resolve its corner radius. */
    private fun childBoxHeight(node: Node): Double =
        if (node is Region) node.height else node.layoutBounds.height

    override fun toString(): String =
        "${javaClass.simpleName}(children=${this.mutableChildren.size}, ${this.layoutSpec})"
}
