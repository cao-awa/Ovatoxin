package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.runtime.Fx
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.style.StyleVisual
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.util.Duration as FxDuration
import kotlin.time.DurationUnit

/**
 * Base class of everything that can be drawn into a
 * [ComponentDrawable][com.github.kusa233.ovatoxin.component.ComponentDrawable].
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

    /**
     * The font size, in pixels, this component resolved the last time it was laid out, or the one
     * it would resolve outside a layout pass.
     *
     * A component states its size in the [style][com.github.kusa233.ovatoxin.style.Style.fontSize]
     * of the block that draws it; a component that says nothing there inherits the size of its
     * [parent], and a component that is not attached to anything yet — or whose tree never sets a
     * size — falls back to [Length.DEFAULT_FONT_SIZE]. A [TextArea][com.github.kusa233.ovatoxin.widget.text.TextArea]
     * is the exception: it renders text of its own and defaults to that same number instead of
     * inheriting.
     *
     * The size is what the layout resolves every [Length.Em] of the component against, and what a
     * control renders its text with. It is resolved afresh on each pass by [resolveFontFor], because
     * a [percent][Length.Percent] size is taken of the component's *own box height* and therefore
     * cannot be known before that box is:
     *
     * ```kotlin
     * draw(Button()) { button ->
     *     layout { height = 40.px }
     *     style { fontSize = 50.percent }   // half of the button's own 40px: 20px of text
     * }
     * ```
     *
     * Inheriting rather than defaulting keeps a whole subtree on one reference, so `1.em` means the
     * same thing for a container and for the text inside it:
     *
     * ```kotlin
     * draw(Div()) { panel ->
     *     panel.layout { width = 200.px; height = 120.px; hPadding = 1.em }
     *     style { fontSize = 20.px }
     *     // the padding of 1.em is 20px, on a panel whose style set 20; the child drawn into it
     *     // says nothing about a size, so its own ems resolve against that same 20px.
     * }
     * ```
     */
    internal open val resolvedFontSize: Double
        get() = this.layoutFontSize ?: this.fontSizeForHeight(null, this.inheritedFontSize)

    /**
     * The font size this component resolved during the last layout pass, or `null` before the first
     * one. Subclasses that render their own text read it; the base class fills it in when it styles
     * the node.
     */
    protected var layoutFontSize: Double? = null

    /**
     * Resolves the [Style.fontSize] of this component for a box [height] pixels tall, falling back
     * to [inherited] when the style names no size.
     *
     * A [percent][Length.Percent] size is taken of [height], the component's own box height. Because
     * that box may not be known yet — or may itself grow with the font, when its height is `auto` or
     * written in `em` — [height] may be `null`, in which case a percentage falls back to
     * [inherited] rather than resolving against a size that depends on the answer.
     */
    internal open fun fontSizeForHeight(height: Double?, inherited: Double): Double =
        this.styleSpec.fontSize?.resolveFontSize(height) ?: inherited

    /**
     * Computes the font size for a box [height] pixels tall and remembers it as [layoutFontSize], so
     * that [resolvedFontSize] reports it for the rest of this pass.
     */
    internal fun resolveFontFor(height: Double): Double {
        val font = this.fontSizeForHeight(this.percentReferenceHeight(height), this.inheritedFontSize)
        this.layoutFontSize = font
        return font
    }

    /**
     * The height a [percent][Length.Percent] font size resolves against — the box height, but only
     * when the declared height is itself a pixel or percentage value that does not need the font.
     * An `auto` or `em` height would grow with the very font being resolved, so it yields `null`
     * and the percentage falls back to the inherited size.
     */
    private fun percentReferenceHeight(height: Double): Double? =
        when (this.layoutSpec.height) {
            is com.github.kusa233.ovatoxin.layout.Pixel,
            is com.github.kusa233.ovatoxin.layout.Percent,
            -> height
            else -> null
        }

    /**
     * The font size this component inherits from its [parent], in pixels, or the library default
     * when it has none — the reference a child falls back to when its own size is not spelled out,
     * or when a percentage of its box cannot be resolved yet.
     */
    internal val inheritedFontSize: Double
        get() = this.parent?.resolvedFontSize ?: Length.DEFAULT_FONT_SIZE

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
        stopStyleAnimation()
        this.displayedVisual = null
        this.appliedStyle = null
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
     * Restores every value of this component to the default it was created with.
     *
     * This runs right before a draw block is entered, so a block always starts from a known
     * state instead of whatever an earlier draw left behind: a layout, a style, a caption or a
     * handler that a previous draw set does not leak into the next one. The three call sites are
     * [SurfaceScope.draw], the replay of an existing child during a redraw, and [redraw]
     * itself.
     *
     * ```kotlin
     * draw(TextArea()) { text -> text.layout { x = 50.percent } }  // second draw
     * // starts at x = 0 again, not at the 50% of the first draw.
     * ```
     *
     * What counts as a default is the *construction* of the component, not the bare class: a
     * `Button("Ok")` resets to the caption `"Ok"`, and a widget re-applies whatever its `init`
     * block seeded — the transparent background of a [com.github.kusa233.ovatoxin.widget.container.Div],
     * the foreground of a [com.github.kusa233.ovatoxin.widget.text.Link].
     *
     * What is **not** touched is the identity of the component: its [parent], the children of a
     * container and the JavaFX node it already built stay as they are. A redraw reconciles the
     * existing tree, it does not rebuild it.
     */
    open fun reset() {
        this.layoutSpec.reset()
        this.styleSpec.reset()
        this.visible = true
        this.clickHandler = null
    }

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
     * own — see [com.github.kusa233.ovatoxin.widget.text.Link], which opens its `url` in the browser.
     *
     * The receiver is the component that was clicked, not a builder scope, so it deliberately
     * carries no [com.github.kusa233.ovatoxin.dsl.OvatoxinDsl] marker: a handler is where the
     * component is *read*, and marking [Component] would hide `this.text` style access from the
     * very block it exists for. The builder scopes a handler could reach into — [Layout], [Style]
     * and the style's background — are marked themselves, which is what keeps a stray
     * `corner = 10.px` out of an enclosing block.
     */
    fun onClick(handler: T.() -> Unit): T {
        this.clickHandler = handler
        return this.self
    }

    /**
     * Adds [handler] after any handler already registered with [onClick], without replacing it.
     *
     * The widget layer uses this for bookkeeping that must not take the caller's own click handler
     * away: a [com.github.kusa233.ovatoxin.widget.container.TabBar] has to observe a tab click, but
     * the button may also have a handler of its own.
     */
    internal fun appendClickHandler(handler: T.() -> Unit) {
        val previous = this.clickHandler
        this.clickHandler = {
            previous?.invoke(this)
            handler(this)
        }
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
     *   [com.github.kusa233.ovatoxin.component.SurfaceScope.draw].
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
     *
     * The component is [reset] first, so the block starts from the defaults again even when it
     * is run for the second, tenth or hundredth time.
     */
    internal fun redraw() {
        reset()
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

    /**
     * Copies a [Layout] preset into this component, tunes it with [block] and renders the change
     * immediately.
     *
     * The runtime counterpart of [DrawScope.layout]'s preset overload: the whole geometry of this
     * component becomes the preset's, and [block] supplies the per-component difference.
     */
    fun applyLayout(preset: Layout, block: Layout.() -> Unit = {}) {
        this.layoutSpec.copyFrom(preset)
        this.layoutSpec.configure(block)
        this.parent?.relayout()
    }

    /** Replaces the appearance of this component and renders the change immediately. */
    fun applyStyle(block: Style.() -> Unit) {
        val previous = this.styleSpec.fontSize
        this.styleSpec.configure(block)
        // A font size is not just a colour: it moves the pixels of every `em` below this
        // component, so a change of it has to lay the tree out again rather than repaint a node.
        if (this.styleSpec.fontSize != previous) {
            relayoutAfterStyleChange()
        } else {
            this.parent?.relayout()
        }
    }

    /**
     * Copies a [Style] preset into this component, tunes it with [block] and renders the change
     * immediately.
     *
     * The runtime counterpart of [DrawScope.style]'s preset overload: the whole appearance of this
     * component becomes the preset's (its background included), and [block] supplies the
     * per-component difference. A font size that really changed lays the tree out again, exactly as
     * [applyStyle] does.
     */
    fun applyStyle(preset: Style, block: Style.() -> Unit = {}) {
        val previous = this.styleSpec.fontSize
        this.styleSpec.copyFrom(preset)
        this.styleSpec.configure(block)
        if (this.styleSpec.fontSize != previous) {
            relayoutAfterStyleChange()
        } else {
            this.parent?.relayout()
        }
    }

    /**
     * Lays this component out again after a style value that changes what its `em` lengths measure.
     *
     * A plain component only has to be placed again by its [parent], since its own `x`, `y`, width
     * and height are the ones written in `em`. A container also has to place its *children* again —
     * see [Container.relayoutAfterStyleChange] — because their inherited font size changed with it.
     */
    internal open fun relayoutAfterStyleChange() {
        this.parent?.relayout()
    }

    /**
     * Applies [styleSpec] to the node, given the box it currently occupies.
     *
     * Called on every layout pass, because a percentage corner radius — and a percentage font
     * size — depends on the box size. Identical CSS is not re-applied, so resizing stays cheap.
     *
     * A control also renders its text at the resolved size: the font size is appended to the inline
     * style so a `50.percent` font really is half the control's box on screen, not just in the
     * layout maths. A component that paints its own text ([appliesOwnFont]) sets its font itself
     * and is left alone.
     */
    internal open fun applyStyle(width: Double, height: Double) {
        val font = this.resolveFontFor(height)
        val target = this.styleSpec.resolveVisual(width, height, font)
        val image = this.styleSpec.background.image
        val fontSuffix = this.fontCss(font)
        val targetCss = this.styleSpec.renderCss(image, target) + fontSuffix

        // While a transition runs, its destination is `animatedTargetCss`. A layout pass that still
        // describes exactly that destination must not restart it.
        if (this.animatedTargetCss != null) {
            if (this.animatedTargetCss == targetCss) return
            // A new destination arrived mid-flight: fall through and animate towards it from the
            // frame currently on screen.
        } else if (targetCss == this.appliedStyle) {
            return
        }

        val transition = this.styleSpec.transition
        val from = this.displayedVisual
        if (from == null || from.matches(target) || transition == null || !transition.isPositive() ||
            !Platform.isFxApplicationThread()
        ) {
            // Nothing to blend from (first paint), this change does not move anything the
            // transition can interpolate (a font weight, for instance), no positive transition was
            // asked for, or we are not on the FX thread where a Timeline must live: apply the
            // target as it is.
            stopStyleAnimation()
            this.displayedVisual = target
            applyCss(targetCss)
            onStyleFrame(target)
            return
        }

        startStyleAnimation(
            from,
            target,
            image,
            fontSuffix,
            transition.toDouble(DurationUnit.MILLISECONDS),
            targetCss,
        )
    }

    /**
     * Called with the appearance currently painted on the node: once when a style is applied at
     * once, and on every frame while a [transition][kotlin.time.Duration] runs.
     *
     * Subclasses that derive something else from the style — a clip outline from the corner radius,
     * for instance — override this to keep that part in step with the animated values. The default
     * does nothing.
     */
    internal open fun onStyleFrame(visual: StyleVisual) {}

    private var styleAnimation: Timeline? = null

    /** The appearance currently painted on the node, i.e. the source of the next transition. */
    private var displayedVisual: StyleVisual? = null

    /** The destination CSS of the running transition, or `null` when none runs. */
    private var animatedTargetCss: String? = null

    /** Paints [css] on the node and remembers it, so an unchanged style is not re-applied. */
    private fun applyCss(css: String) {
        this.appliedStyle = css
        this.node.style = css
    }

    /**
     * Blends the node from [from] to [to] over [milliseconds], re-rendering every frame.
     *
     * The image (if any) is taken from the style as it stands: an image is not interpolated. On
     * completion the exact final CSS is painted, so rounding in intermediate frames never leaves
     * the node a hair off.
     */
    private fun startStyleAnimation(
        from: StyleVisual,
        to: StyleVisual,
        image: String?,
        fontSuffix: String,
        milliseconds: Double,
        targetCss: String,
    ) {
        stopStyleAnimation()
        val progress = SimpleDoubleProperty(0.0)
        val timeline = Timeline(
            KeyFrame(FxDuration.ZERO, KeyValue(progress, 0.0)),
            KeyFrame(FxDuration.millis(milliseconds), KeyValue(progress, 1.0, Interpolator.EASE_BOTH)),
        )
        progress.addListener { _, _, value ->
            val visual = from.lerp(to, value.toDouble())
            this.displayedVisual = visual
            applyCss(this.styleSpec.renderCss(image, visual) + fontSuffix)
            onStyleFrame(visual)
        }
        timeline.setOnFinished {
            this.displayedVisual = to
            this.animatedTargetCss = null
            this.styleAnimation = null
            applyCss(targetCss)
            onStyleFrame(to)
        }
        this.animatedTargetCss = targetCss
        this.styleAnimation = timeline
        timeline.play()
    }

    /** Stops a running transition, keeping whatever frame is currently painted. */
    private fun stopStyleAnimation() {
        this.styleAnimation?.stop()
        this.styleAnimation = null
        this.animatedTargetCss = null
    }

    /**
     * The `-fx-font-size` and `-fx-font-weight` declarations that make a control render the
     * resolved [font] and the requested [Style.fontWeight], or an empty string when the node is
     * not a control.
     *
     * Leaving the library defaults in place keeps a plain tree looking native; a control only
     * picks up an explicit size — its own, or one inherited from an ancestor that stated a size —
     * and only picks up a weight when a style states one.
     */
    private fun fontCss(font: Double): String {
        if (this.builtNode !is javafx.scene.control.Control || this.appliesOwnFont) return ""
        val weight = this.styleSpec.fontWeight
        val weightCss = weight?.let { " -fx-font-weight: ${it.weight};" } ?: ""
        if (this.styleSpec.fontSize == null && this.inheritedFontSize == Length.DEFAULT_FONT_SIZE) {
            return weightCss
        }
        return "$weightCss -fx-font-size: ${formatFont(font)}px;"
    }

    /** Whether this component paints its own text and must keep [fontCss] off its node. */
    protected open val appliesOwnFont: Boolean get() = false

    /** Formats a font size the way CSS likes it: `20`, not `20.0`. */
    private fun formatFont(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (rounded == kotlin.math.floor(rounded)) rounded.toInt().toString() else rounded.toString()
    }

    private var appliedStyle: String? = null

    override fun toString(): String =
        "${javaClass.simpleName}(${this.layoutSpec}, ${this.styleSpec})"
}
