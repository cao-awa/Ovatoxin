package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.runtime.Fx
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import javafx.scene.Node
import javafx.scene.input.MouseButton

/**
 * Base class of everything that can be drawn into a [Container].
 *
 * A component owns a [layoutSpec], a [styleSpec] and knows its [parent]. The backing JavaFX
 * [Node] is created lazily on the JavaFX thread, so component trees can be built and inspected
 * without a running toolkit — which keeps them unit testable.
 *
 * Every component can be clicked: [onClick] hands the handler a receiver of the concrete type,
 * so inside the block `this` is the component that was drawn.
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     button.text = "Button"
 *     button.onClick { println(this.text) }
 * }
 * ```
 *
 * @param T the concrete subclass, so [onClick] and [performClick] can hand out the exact type.
 */
abstract class Component<T : Component<T>> {

    /** Position and size of this component inside its parent container. */
    val layoutSpec: Layout = Layout()

    /** Background, text colour and corner radius of this component. */
    val styleSpec: Style = Style()

    /** The container this component was drawn into, or `null` while it is not attached. */
    var parent: Container<*>? = null
        internal set

    /** This component typed as its concrete subclass. */
    @Suppress("UNCHECKED_CAST")
    protected val self: T get() = this as T

    private var nodeRef: Node? = null

    /** The backing node, creating it on first access. Must run on the JavaFX thread. */
    internal val node: Node get() = this.nodeRef ?: createNode()

    /** The backing node if it already exists, or `null`. Safe to call from any thread. */
    internal val builtNode: Node? get() = this.nodeRef

    /** Creates the JavaFX node of this component. Called at most once. */
    protected abstract fun buildNode(): Node

    /**
     * Whether the backing node reports a mouse click as an *action* of its own.
     *
     * A JavaFX `Button` fires an action while it is pressed, and that action is already wired to
     * [performClick]. The generic mouse wiring has to stay out of the way there, otherwise a
     * single click would run the handler twice.
     */
    protected open val reportsClicksAsAction: Boolean get() = false

    /** Builds the node and adds the click handling every component shares. */
    private fun createNode(): Node {
        val created = buildNode()
        this.nodeRef = created
        if (!this.reportsClicksAsAction) {
            created.setOnMouseClicked { event ->
                if (event.button == MouseButton.PRIMARY) performClick()
            }
        }
        onNodeCreated(created)
        return created
    }

    /**
     * Called right after [buildNode] returned and the node was stored.
     *
     * By the time it runs [builtNode] is already set, so implementations may safely touch the
     * component itself.
     */
    protected open fun onNodeCreated(node: Node) {}

    /** Whether the JavaFX node backing this component already exists. */
    val isNodeCreated: Boolean get() = this.nodeRef != null

    /** Drops the backing node, e.g. when the component is removed from its container. */
    internal fun detachNode() {
        this.nodeRef = null
    }

    private var visibleValue: Boolean = true

    /** Whether the component is rendered. Defaults to `true`. */
    var visible: Boolean
        get() = this.nodeRef?.isVisible ?: this.visibleValue
        set(value) {
            this.visibleValue = value
            this.nodeRef?.isVisible = value
        }

    private var clickHandler: (T.() -> Unit)? = null

    /** Whether [onClick] was given a handler. */
    val hasOnClick: Boolean get() = this.clickHandler != null

    /**
     * Registers the click handler, replacing any handler registered before.
     *
     * Inside the handler `this` is this component, so its own properties can be read directly:
     *
     * ```kotlin
     * button.onClick { println(this.text) }
     * ```
     *
     * A component without a handler ignores clicks, unless it defines a default behaviour of its
     * own — see [com.github.kusa233.ovatoxin.widget.Link], which opens its `url` in the browser.
     */
    fun onClick(handler: T.() -> Unit): T {
        this.clickHandler = handler
        return this.self
    }

    /**
     * Runs the click handler as if the user had clicked this component.
     *
     * The backing node calls this on a primary mouse click, tests call it directly.
     */
    open fun performClick() {
        this.clickHandler?.invoke(this.self)
    }

    /**
     * Binds this component to [state], so the state keeps it up to date.
     *
     * ```kotlin
     * val state = State("zh-cn")
     *
     * val text = draw(TextArea()) { it.text(languages[state.value]) }.withState(state)
     * ```
     *
     * Binding does two things. The component is redrawn once right away, and from then on every
     * value the state receives saves itself there and redraws **this component only** — the rest
     * of the tree is left untouched, which is what makes a state cheap: one component redraws,
     * not the whole window.
     *
     * A redraw re-runs the block this component was drawn with, so a block that derives its
     * content from [State.value] — a translation, a formatted number — recomputes it.
     *
     * A component can be bound to more than one state; each of them redraws it.
     *
     * @return this component, so the call chains onto the result of
     *   [com.github.kusa233.ovatoxin.component.Container.draw].
     */
    fun withState(state: State<*>): T {
        state.bind(this)
        redraw()
        return this.self
    }

    /** The block this component was drawn with, re-run on every redraw. */
    internal var drawBlock: (() -> Unit)? = null
        private set

    /** Remembers the block this component was drawn with, so a redraw can re-run it. */
    internal fun recordDrawBlock(block: () -> Unit) {
        this.drawBlock = block
    }

    /**
     * Re-runs this component's draw block and re-renders the component on its own.
     *
     * Called by a bound [State] when its value changes. A component that is not rendered yet
     * has no node to paint, so the stored value reaches the screen when it is first shown.
     */
    internal fun redraw() {
        replayDrawBlock()
        renderSelf()
    }

    /** Re-runs [drawBlock], if this component was drawn with one. */
    internal open fun replayDrawBlock() {
        this.drawBlock?.invoke()
    }

    /**
     * Re-renders this component's node without touching any sibling.
     *
     * With a parent, the container re-places and re-styles this one child. Without a parent
     * there is no box to place it against, so it is styled against the box it already occupies.
     */
    private fun renderSelf() {
        val node = this.builtNode ?: return
        val host = this.parent
        if (host != null) {
            host.repaint(this)
        } else {
            Fx.runOnFxThread { applyStyle(node.layoutBounds.width, node.layoutBounds.height) }
        }
    }

    /** Replaces the geometry of this component and renders the change immediately. */
    fun applyLayout(block: Layout.() -> Unit) {
        this.layoutSpec.configure(block)
        this.parent?.relayout()
    }

    /** Replaces the appearance of this component and renders the change immediately. */
    fun applyStyle(block: Style.() -> Unit) {
        this.styleSpec.configure(block)
        this.parent?.relayout()
    }

    /**
     * Applies [styleSpec] to the node, given the box it currently occupies.
     *
     * Called on every layout pass, because a percentage corner radius depends on the box size.
     * Identical CSS is not re-applied, so resizing stays cheap.
     */
    internal open fun applyStyle(width: Double, height: Double) {
        val css = this.styleSpec.toCss(width, height)
        if (css == this.appliedStyle) return
        this.appliedStyle = css
        this.node.style = css
    }

    private var appliedStyle: String? = null

    override fun toString(): String =
        "${javaClass.simpleName}(${this.layoutSpec}, ${this.styleSpec})"
}
