package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.runtime.Fx
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

/**
 * Base class of every component that can hold children.
 *
 * Children are declared with [draw]; their geometry is expressed by a [Layout] that mixes
 * percentages of the container size with absolute pixels. Layouts are re-resolved whenever
 * the container is resized.
 *
 * Percentages always resolve against the container itself, not against the window, so nesting
 * a [com.github.kusa233.ovatoxin.widget.Div] rescopes them for everything inside.
 *
 * @param T the concrete subclass, so `layout { }` returns the exact type and `this` inside the
 *   block is the concrete container rather than a bare [Container].
 */
abstract class Container<T : Container<T>> : Component<T>() {

    private val mutableChildren = mutableListOf<Component<*>>()

    /** All children in drawing order (bottom to top). */
    val children: List<Component<*>> get() = this.mutableChildren

    private var pane: Pane? = null

    /** The pane hosting the children, or `null` while the container is not rendered. */
    internal val contentPane: Pane? get() = this.pane

    /**
     * Declares the children of this container, mirroring the top level block of the DSL:
     *
     * ```kotlin
     * frame.layout {
     *     draw(Button()) { ... }
     * }
     * ```
     */
    fun layout(block: T.() -> Unit): T {
        // A replayed block draws its children again, so register here too: a container whose
        // new block draws fewer children than before still has to be trimmed.
        ReplayScope.current?.begin(this)
        this.self.block()
        relayout()
        return this.self
    }

    /**
     * Draws [component] into this container and configures it with [config].
     *
     * The component is the lambda parameter; the receiver is a [DrawScope] that accepts a
     * `layout { ... }` block. Drawing a container nests it, so its own `layout { ... }` blocks
     * are scoped to that container.
     *
     * The block is remembered as the render function of the component, so a redraw — see
     * [Component.withState] — can run it again and pick up the new value of a state.
     */
    fun <C : Component<*>> draw(component: C, config: DrawScope<C>.(C) -> Unit = {}): C {
        val replayed = replay(component, config)
        if (replayed != null) return replayed

        require(component.parent == null) {
            "${component::class.simpleName} is already attached to a container"
        }
        component.parent = this
        this.mutableChildren += component
        val scope = DrawScope(component)
        component.recordDrawBlock { scope.config(component) }
        scope.config(component)
        relayout()
        return component
    }

    /**
     * Fills a slot of a replay with an existing child instead of a new one.
     *
     * Returns `null` when no replay is running or the slot does not hold a component of the
     * same kind, in which case [draw] attaches [component] as usual.
     */
    private fun <C : Component<*>> replay(component: C, config: DrawScope<C>.(C) -> Unit): C? {
        val scope = ReplayScope.current ?: return null
        val existing = this.mutableChildren.getOrNull(scope.index(this))
        if (existing == null || existing::class != component::class) return null
        scope.advance(this)

        @Suppress("UNCHECKED_CAST")
        val target = existing as C
        DrawScope(target).config(target)
        return target
    }

    /** Re-runs the render function of this container, reconciling the children it draws. */
    internal override fun replayDrawBlock() {
        val previous = ReplayScope.current
        val scope = ReplayScope()
        ReplayScope.current = scope
        scope.begin(this)
        try {
            super.replayDrawBlock()
        } finally {
            ReplayScope.current = previous
            scope.finish()
        }
        relayout()
    }

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
        Fx.runOnFxThread { applyGeometry(host) }
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
