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
     */
    fun <C : Component<*>> draw(component: C, config: DrawScope<C>.(C) -> Unit = {}): C {
        require(component.parent == null) {
            "${component::class.simpleName} is already attached to a container"
        }
        component.parent = this
        this.mutableChildren += component
        DrawScope(component).config(component)
        relayout()
        return component
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

    /** Places every child onto [host], adding missing nodes to the pane. */
    private fun applyGeometry(host: Pane) {
        val width = host.width
        val height = host.height

        // The container styles itself against its own box; a percentage corner needs that size.
        applyStyle(width, height)

        for (child in this.mutableChildren.toList()) {
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
