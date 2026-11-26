package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.DrawScope
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

/**
 * A container with a [TabBar] on one of its four sides and one visible component per tab.
 *
 * ```kotlin
 * val pane = TabPane()
 * pane.side = TabSide.LEFT
 *
 * val first = pane.add(Button("One"))
 * pane.bind(first, TextArea("first page"))
 *
 * val second = pane.add(Button("Two"))
 * pane.bind(second, TextArea("second page"))
 *
 * draw(pane) {
 *     layout {
 *         width = 100.percent
 *         height = 100.percent
 *     }
 * }
 * ```
 *
 * [add] puts a button into the [tab] bar. [bind] attaches content to an explicit tab button: every
 * component bound to the selected tab is visible, every component bound to another tab is hidden.
 * Selecting a tab therefore switches the shown content without rebuilding anything.
 *
 * The bar itself is a [TabBar], so its buttons are added and removed through [TabBar.add] and
 * [TabBar.remove]; [add] and [remove] here are the same operations spelled on the pane. The side is
 * available both as [TabPane.side] and as [TabBar.side].
 *
 * [windowDragEnabled] forwards to the bar and lets the empty part of the bar drag the window, which
 * is useful when the pane fills a frameless window.
 *
 * `x` and `y` of bound components are ignored. Binding draws the component immediately and fills
 * the content area that remains after the tab bar: a component without a width or height gets
 * `100.percent` of that remaining area, while a width or height given by the caller is kept. The
 * `x` and `y` of the tab buttons are ignored as well, since [TabBar] decides where they sit.
 */
class TabPane(
    /** The initial size of the tab strip across the side it occupies. */
    barSize: Length = DEFAULT_TAB_BAR_THICKNESS,
) : Container<TabPane>() {

    /** The tab strip controlled by this pane. */
    val tab: TabBar = TabBar()

    /** The same bar as [tab], under a name that reads better in prose. */
    val tabBar: TabBar get() = this.tab

    /** The content area holding the components bound to tabs. */
    private val contentHost: TabContentHost = TabContentHost()

    /** Components bound to each tab, in binding order. */
    private val boundComponents: MutableMap<Button, MutableList<Component<*>>> = LinkedHashMap()

    /**
     * Size of the tab strip, measured across the side it is on.
     *
     * For [TabSide.UP] and [TabSide.BOTTOM] this is the bar height; for [TabSide.LEFT] and
     * [TabSide.RIGHT] it is the bar width. Defaults to `36.px`.
     */
    var barSize: Length = barSize
        set(value) {
            if (field == value) return
            field = value
            relayout()
        }

    /** The same size as [barSize], kept for callers written before the rename. */
    var tabBarThickness: Length
        get() = this.barSize
        set(value) {
            this.barSize = value
        }

    init {
        this.tab.parent = this
        this.tab.pane = this
        attach(this.tab)
        this.contentHost.parent = this
        attach(this.contentHost)
        this.tab.selectionListener = { refreshVisibility() }
        this.contentHost.styleSpec.background { color = null }
    }

    /** Which side of the pane the tab bar is placed on. Defaults to [TabSide.UP]. */
    var side: TabSide
        get() = this.tab.side
        set(value) {
            this.tab.side = value
        }

    /**
     * Whether dragging the empty part of the tab bar moves its window.
     *
     * This forwards to [TabBar.windowDragEnabled]. Enable it when the pane fills a frameless window
     * and the bar should also serve as its title bar; tab buttons and scroll bars keep working as
     * usual.
     */
    var windowDragEnabled: Boolean
        get() = this.tab.windowDragEnabled
        set(value) {
            this.tab.windowDragEnabled = value
        }

    /**
     * The background and text colour painted on the selected tab.
     *
     * This is the same style as [TabBar.selectionStyle]; the pane simply forwards it to [tab].
     *
     * ```kotlin
     * pane.selectColor {
     *     background = Color.WHITE
     *     foreground = Color.BLACK
     * }
     * ```
     */
    val selectionStyle: TabSelectionStyle
        get() = this.tab.selectionStyle

    /** Configures [selectionStyle], painting the result on the selected tab. */
    fun selectColor(block: TabSelectionStyle.() -> Unit) {
        this.tab.selectColor(block)
    }

    /**
     * The selected tab's background colour, kept for source compatibility.
     *
     * Use [selectColor] with a [TabSelectionStyle] block instead, which can also set the selected
     * tab's text colour.
     */
    @Deprecated("Use selectColor { background = ... } so the selected text colour can be set too")
    var selectColor: Color?
        get() = this.tab.selectionStyle.background
        set(value) {
            this.tab.selectionStyle.background = value
        }

    /** The tab buttons in the order they were added. */
    val tabs: List<Button> get() = this.tab.tabs

    /** The selected tab button, or `null` when no tab has been added. */
    val selectedTab: Button? get() = this.tab.selected

    /** The selected tab button, or `null` when no tab has been added. */
    val selected: Button? get() = this.tab.selected

    /**
     * Adds [button] as a tab and configures it with [block].
     *
     * The new tab is selected. See [TabBar.add].
     *
     * @return the added button.
     */
    fun add(button: Button, block: Button.() -> Unit = {}): Button = this.tab.add(button, block)

    /**
     * Removes [button] and every component bound to it.
     *
     * If it was the selected tab, the first remaining tab becomes selected.
     *
     * @return `true` when the button was a tab of this pane.
     */
    fun remove(button: Button): Boolean {
        this.boundComponents.remove(button)?.forEach { this.contentHost.remove(it) }
        val removed = this.tab.remove(button)
        if (removed) refreshVisibility()
        return removed
    }

    /** Selects [button], which must already be a tab of this pane. */
    fun select(button: Button) {
        this.tab.select(button)
    }

    /** Selects the tab at [index], or throws when there is no such tab. */
    fun select(index: Int) {
        this.tab.select(index)
    }

    /** Clears the selection; every bound component is then hidden. */
    fun clearSelection() {
        this.tab.clearSelection()
    }

    /**
     * Binds [component] to the tab [button] and configures it with [block].
     *
     * The component is visible exactly while [button] is the selected tab. It fills the content
     * area by default, and its `x` and `y` are ignored. The block runs in a [DrawScope], so
     * `layout { }` and `style { }` configure [component], which is also passed as the block
     * parameter.
     *
     * @return [component], so the call chains onto the bound component.
     * @throws IllegalArgumentException when [button] is not a tab of this pane.
     */
    fun <T : Component<T>> bind(
        button: Button,
        component: T,
        block: DrawScope<T>.(T) -> Unit = {},
    ): T {
        require(this.tabs.any { it === button }) {
            "bind needs a tab that belongs to this TabPane"
        }
        return bindTo(button, component, block)
    }

    /** The components bound to [button], in the order they were bound. */
    fun componentsFor(button: Button): List<Component<*>> =
        this.boundComponents[button]?.toList().orEmpty()

    /** Binds [component] to the explicitly selected [target] tab. */
    private fun <T : Component<T>> bindTo(
        target: Button,
        component: T,
        block: DrawScope<T>.(T) -> Unit,
    ): T {
        require(component.parent == null) { "a bound component can only belong to one tab" }
        val drawn = this.contentHost.drawBound(component, block) { target === this.tab.selected }
        this.boundComponents.getOrPut(target) { mutableListOf() }.add(drawn)
        return drawn
    }

    /** Shows the components of the selected tab and hides every other tab's components. */
    private fun refreshVisibility() {
        val selected = this.tab.selected
        for ((button, components) in this.boundComponents) {
            val visible = button === selected
            for (component in components) {
                component.visible = visible
            }
        }
        this.contentHost.relayout()
    }

    /**
     * Applies the pane's style and hands the same appearance to its bar.
     *
     * The bar is the pane's own chrome, so it shares the pane's background, corner and text colour
     * instead of carrying a second, independent style. The copy happens on every layout pass, so a
     * style changed after the tree was built reaches the bar as well.
     */
    internal override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        this.tab.styleSpec.copyFrom(this.styleSpec)
        this.tab.syncInheritedStyle()
    }

    /**
     * Places the bar on the configured side and gives it the rest of the pane.
     *
     * The content host fills whatever the bar leaves over. The calculations are in pixels because
     * the content box is already resolved against the pane's current size, and a percentage tab bar
     * thickness has been resolved against the side it occupies.
     */
    override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        // A caller can drive a geometry pass directly; keep the bar in step even then.
        this.tab.styleSpec.copyFrom(this.styleSpec)
        this.tab.syncInheritedStyle()
        val vertical = this.side.isVertical
        val available = if (vertical) box.width else box.height
        val thickness = this.barSize.resolve(available, this.resolvedFontSize)
            .coerceIn(0.0, available)
        val remaining = (available - thickness).coerceAtLeast(0.0)

        when (this.side) {
            TabSide.UP -> {
                setBox(this.tab, 0.0, 0.0, box.width, thickness)
                setBox(this.contentHost, 0.0, thickness, box.width, remaining)
            }

            TabSide.BOTTOM -> {
                setBox(this.tab, 0.0, remaining, box.width, thickness)
                setBox(this.contentHost, 0.0, 0.0, box.width, remaining)
            }

            TabSide.LEFT -> {
                setBox(this.tab, 0.0, 0.0, thickness, box.height)
                setBox(this.contentHost, thickness, 0.0, remaining, box.height)
            }

            TabSide.RIGHT -> {
                setBox(this.tab, remaining, 0.0, thickness, box.height)
                setBox(this.contentHost, 0.0, 0.0, remaining, box.height)
            }
        }
    }

    /** Gives [component] an exact pixel box relative to the pane's content origin. */
    private fun setBox(
        component: Component<*>,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        component.layoutSpec.x = x.px
        component.layoutSpec.y = y.px
        component.layoutSpec.width = width.px
        component.layoutSpec.height = height.px
    }

    override fun buildNode(): Node = Pane()

    companion object {
        /** The default thickness of the tab bar, in pixels. */
        val DEFAULT_TAB_BAR_THICKNESS: Length = 36.px
    }
}

/**
 * The internal area that holds tab content.
 *
 * It is a plain [Container] rather than a public drawing surface: callers bind components instead
 * of drawing into the page. The only special rule is that child `x` and `y` never move a page
 * component — the page owns that space.
 */
private class TabContentHost : Container<TabContentHost>() {

    init {
        this.styleSpec.background { color = null }
    }

    /**
     * Draws [component] into this host and remembers [block] so a redraw can replay it.
     *
     * Binding is deliberately the same operation as `draw` on a visible surface: the component is
     * attached to this host right away, the block configures it immediately, and the component
     * records the block for a later state-driven redraw. The only page-specific rule applied after
     * every run is the geometry of a page: `x` and `y` are ignored, and a missing width or height
     * fills the host — which is already the area left over after the tab bar.
     *
     * [isVisible] is evaluated on every run, so a redraw cannot make a component of an unselected
     * tab visible again.
     */
    internal fun <T : Component<T>> drawBound(
        component: T,
        block: DrawScope<T>.(T) -> Unit,
        isVisible: () -> Boolean,
    ): T {
        component.reset()
        component.parent = this
        attach(component)

        val render: () -> Unit = {
            DrawScope(component).block(component)
            component.layoutSpec.x = Length.ZERO
            component.layoutSpec.y = Length.ZERO
            if (component.layoutSpec.width == null) component.layoutSpec.width = 100.percent
            if (component.layoutSpec.height == null) component.layoutSpec.height = 100.percent
            component.visible = isVisible()
        }
        component.recordDrawBlock(render)
        render()
        relayout()
        return component
    }

    override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        for (child in this.children) {
            child.layoutSpec.x = Length.ZERO
            child.layoutSpec.y = Length.ZERO
        }
    }

    override fun buildNode(): Node = Pane()
}
