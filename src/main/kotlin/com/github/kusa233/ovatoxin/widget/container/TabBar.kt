package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.DrawScope
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.runtime.Fx
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.window.WindowDragger
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.paint.Color

/**
 * The strip of tab buttons belonging to a [TabPane].
 *
 * A tab bar is a [FlexBox]: it lays its buttons in one row (side [TabSide.UP] or [TabSide.BOTTOM])
 * or one column (side [TabSide.LEFT] or [TabSide.RIGHT]). When the tabs outgrow the space the bar
 * is given, it scrolls instead of wrapping, showing a horizontal or vertical scroll bar as needed.
 * The `x` and `y` of a tab button are ignored, because the bar decides where the buttons go. The bar
 * stretches every button across its thickness — `width = 100.percent` on the vertical sides and
 * `height = 100.percent` on the horizontal ones — while the other axis keeps the size the button
 * named. A horizontal bar also centres its tabs by default (`justify = Justify.CENTER`).
 *
 * ```kotlin
 * val tabs = TabBar()
 * tabs.side = TabSide.LEFT
 *
 * val first = tabs.add(Button("One")) { text("One") }
 * val second = tabs.add(Button("Two")) { text("Two") }
 * tabs.select(first)
 * ```
 *
 * [add] selects the new button. Clicking a button selects it too. [bind] takes the target tab
 * explicitly. [remove] removes the button together with its tab position, selecting the first
 * remaining tab when the removed one was selected.
 *
 * A `selectColor { ... }` block paints the background and text colour of the selected button,
 * while every other button keeps the style it was given. With no selection style set (the default)
 * the bar paints no selected/unselected difference at all. A [TabPane] also uses the selection to
 * show and hide the bound components.
 *
 * Setting [windowDragEnabled] turns the empty background of the bar into a window drag handle, which
 * is useful when the bar doubles as the title bar of a frameless window. Tabs and scroll bars keep
 * their own mouse behaviour.
 */
class TabBar : FlexBox() {

    private var sideValue: TabSide = TabSide.UP
    private var selectedValue: Button? = null
    private var verticalSlots: Map<Component<*>, Slot> = emptyMap()
    private val detachedBindings: MutableMap<Button, MutableList<Component<*>>> = LinkedHashMap()

    /** The style each button had before the selected colour was painted over it. */
    private val baseStyles: MutableMap<Button, Style> = LinkedHashMap()

    /** The width and height each button named before the bar took over its cross-axis size. */
    private val baseLayouts: MutableMap<Button, BaseLayout> = LinkedHashMap()

    /** The scroll pane that wraps the tab row or column, or `null` before the bar is rendered. */
    private var scrollPane: ScrollPane? = null

    /** The pane the buttons are actually placed into, inside [scrollPane]. */
    private var scrollContent: Pane? = null

    init {
        // A horizontal strip leaves its leftover room on both sides rather than after the last tab.
        this.justify = Justify.CENTER
    }

    /**
     * Which edge of the owning [TabPane] this bar is placed on. Defaults to [TabSide.UP].
     *
     * Setting it lays the owning pane out again, because the bar and the content swap their
     * horizontal or vertical relationship. It also swaps which axis the tabs fill: a vertical bar
     * stretches every button to the bar's width, a horizontal one to its height.
     */
    var side: TabSide
        get() = this.sideValue
        set(value) {
            if (this.sideValue == value) return
            this.sideValue = value
            applyTabSizing()
            this.scrollContent?.requestLayout()
            this.parent?.relayout()
        }

    /**
     * Whether pressing and dragging the empty background of this bar moves its window.
     *
     * Defaults to `false`. When enabled, a primary-button drag on the part of the bar not covered by
     * a tab or a scroll bar moves the [javafx.stage.Stage] that contains the bar; tab clicks and
     * scroll-bar drags are untouched. This makes a [TabBar] usable as the title bar of a frameless
     * window created with `Frame(titleBarEnabled = false)`.
     *
     * The property can be changed after the bar is shown: every press checks its current value.
     */
    var windowDragEnabled: Boolean = false

    /**
     * The background and text colour painted on the selected tab.
     *
     * Neither value is set by default, so every tab keeps the appearance it was given. Setting
     * [TabSelectionStyle.background] fills only the selected button, while an unselected tab with
     * no background of its own stays transparent and lets the bar's fill — usually the pane's
     * background — show through. [TabSelectionStyle.foreground] overrides the bar's inherited text
     * colour on the selected button only.
     *
     * ```kotlin
     * bar.selectColor {
     *     background = Color.WHITE
     *     foreground = Color.BLACK
     * }
     * ```
     */
    val selectionStyle: TabSelectionStyle = TabSelectionStyle().also {
        it.onChange = { applySelectionStyle() }
    }

    /** Configures [selectionStyle], painting the result on the currently selected tab. */
    fun selectColor(block: TabSelectionStyle.() -> Unit) {
        this.selectionStyle.configure(block)
    }

    /**
     * The selected tab's background colour, kept for source compatibility.
     *
     * Use [selectColor] with a [TabSelectionStyle] block instead, which can also set the selected
     * tab's text colour.
     */
    @Deprecated("Use selectColor { background = ... } so the selected text colour can be set too")
    var selectColor: Color?
        get() = this.selectionStyle.background
        set(value) {
            this.selectionStyle.background = value
        }

    /** The currently selected tab button, or `null` when the bar has no tabs. */
    var selected: Button?
        get() = this.selectedValue
        set(value) {
            require(value == null || value.parent === this) {
                "a tab must belong to this TabBar before it can be selected"
            }
            if (this.selectedValue === value) return
            this.selectedValue = value
            applySelectionStyle()
            updateDetachedVisibility()
            scheduleSelectedVisible()
            this.selectionListener?.invoke(value)
        }

    /** The currently selected tab button, or `null` when the bar has no tabs. */
    val selectedButton: Button? get() = this.selectedValue

    /** The tab buttons in the order they were added. */
    val tabs: List<Button> get() = this.children.filterIsInstance<Button>()

    /** Receives every selection change; [TabPane] uses it to show and hide bound content. */
    internal var selectionListener: ((Button?) -> Unit)? = null

    /** The pane this bar was installed in, if any; [bind] delegates to it. */
    internal var pane: TabPane? = null

    /**
     * Adds [button] as the last tab and configures it with [block].
     *
     * The new tab becomes the selected one, so the content bound immediately afterwards belongs to
     * it. The callback is appended to any `onClick` the block registers instead of replacing it.
     *
     * `x` and `y` are ignored: the bar owns the position of its buttons.
     *
     * @return the added button.
     */
    fun add(button: Button, block: Button.() -> Unit = {}): Button {
        require(button.parent == null) { "a tab button can only belong to one TabBar" }
        button.reset()
        button.parent = this
        attach(button)
        button.block()
        button.layoutSpec.x = Length.ZERO
        button.layoutSpec.y = Length.ZERO
        this.baseStyles[button] = Style().also {
            it.copyFrom(button.styleSpec)
            // A tab with no background of its own should let the bar's fill show through, not
            // cover it with the button control's default white.
            if (it.background.color == Style.DEFAULT_COLOR) it.background.color = null
        }
        this.baseLayouts[button] = BaseLayout(button.layoutSpec.width, button.layoutSpec.height)
        applyTabSizing(button)
        button.appendClickHandler { select(this) }
        select(button)
        this.scrollContent?.requestLayout()
        relayout()
        return button
    }

    /**
     * Binds [component] to the tab [button].
     *
     * Inside a [TabPane] this is the same call as [TabPane.bind], reached through the bar for code
     * that already holds the [TabBar]. On a standalone bar no content area exists to lay the
     * component out in, but the bar still tracks it and shows it only while [button] is selected —
     * useful when the content is rendered elsewhere. The block runs in a [DrawScope], so
     * `layout { }` and `style { }` configure [component], which is also passed as the block
     * parameter.
     *
     * @return [component], so the call chains onto the bound component.
     * @throws IllegalArgumentException when [button] is not a tab of this bar.
     */
    fun <T : Component<T>> bind(
        button: Button,
        component: T,
        block: DrawScope<T>.(T) -> Unit = {},
    ): T {
        require(this.tabs.any { it === button }) {
            "bind needs a tab that belongs to this TabBar"
        }
        val owner = this.pane
        if (owner != null) return owner.bind(button, component, block)

        require(component.parent == null) { "a bound component can only belong to one tab" }
        component.reset()
        DrawScope(component).block(component)
        component.layoutSpec.x = Length.ZERO
        component.layoutSpec.y = Length.ZERO
        if (component.layoutSpec.width == null) component.layoutSpec.width = 100.percent
        if (component.layoutSpec.height == null) component.layoutSpec.height = 100.percent
        this.detachedBindings.getOrPut(button) { mutableListOf() }.add(component)
        component.visible = button === this.selected
        return component
    }

    /** The components bound to [button], whether through a pane or a standalone bar. */
    fun componentsFor(button: Button): List<Component<*>> =
        this.pane?.componentsFor(button) ?: this.detachedBindings[button]?.toList().orEmpty()

    /**
     * Removes [button] from the bar.
     *
     * When it was the selected tab, the first remaining tab becomes selected; an empty bar has no
     * selection. Any content bound to the tab is removed by [TabPane.remove], not here.
     *
     * @return `true` when the button was a tab of this bar.
     */
    fun remove(button: Button): Boolean {
        if (button.parent !== this) return false
        val wasSelected = this.selectedValue === button
        this.detachedBindings.remove(button)
        this.baseStyles.remove(button)
        this.baseLayouts.remove(button)
        super.remove(button)
        if (wasSelected) this.selected = this.tabs.firstOrNull()
        this.scrollContent?.requestLayout()
        relayout()
        return true
    }

    /** Selects [button], which must already be a tab of this bar. */
    fun select(button: Button) {
        this.selected = button
    }

    /** Selects the tab at [index], or throws when there is no such tab. */
    fun select(index: Int) {
        this.selected = this.tabs.getOrNull(index)
            ?: throw IndexOutOfBoundsException("tab index $index is outside 0 until ${this.tabs.size}")
    }

    /** Clears the selection; every bound component of a [TabPane] is then hidden. */
    fun clearSelection() {
        this.selected = null
    }

    /**
     * Paints the selection style on the selected button and restores every other button to its
     * base style.
     *
     * The base style is captured when the button is added, so a selected colour is never folded
     * back into it and switching tabs restores the caller's original appearance.
     */
    private fun applySelectionStyle() {
        for (button in this.tabs) {
            val base = this.baseStyles[button] ?: continue
            button.styleSpec.copyFrom(base)
            applyInheritedTransition(button, base)
            applyInheritedForeground(button, base)
            applySelectionOverride(button)
        }
        relayout()
    }

    /**
     * Applies the selected-tab colours to [button] when it is the selected one.
     *
     * The override runs after the inherited style, so a selected foreground can deliberately differ
     * from the pane's text colour — for instance black text on a white selected tab.
     */
    private fun applySelectionOverride(button: Button) {
        if (button !== this.selectedValue) return
        this.selectionStyle.background?.let { button.styleSpec.background.color = it }
        this.selectionStyle.foreground?.let { button.styleSpec.foreground = it }
    }

    /**
     * Keeps the inherited style values in step for every tab that did not name them itself.
     *
     * The bar copies the owning pane's style, so a `transition` and a `foreground` written on a
     * [TabPane] have to reach the buttons that paint the text. Without the colour copy it would
     * stay on the bar's own `ScrollPane`, where no text is painted, and the buttons would keep
     * JavaFX's default dark caption. Explicit values on a button are its own choice and win.
     */
    internal fun syncInheritedStyle() {
        for (button in this.tabs) {
            val base = this.baseStyles[button] ?: continue
            applyInheritedTransition(button, base)
            applyInheritedForeground(button, base)
            applySelectionOverride(button)
        }
    }

    private fun applyInheritedTransition(button: Button, base: Style) {
        if (base.transition == null) {
            button.styleSpec.transition = this.styleSpec.transition
        }
    }

    private fun applyInheritedForeground(button: Button, base: Style) {
        if (base.foreground == null) {
            button.styleSpec.foreground = this.styleSpec.foreground
        }
    }

    /**
     * Gives the tabs their cross-axis size: a vertical bar stretches them across its width, a
     * horizontal one across its height. The other axis keeps whatever the button named itself.
     */
    private fun applyTabSizing() {
        for (button in this.tabs) applyTabSizing(button)
    }

    /** Applies [applyTabSizing] to one button, restoring the size it named on the other axis. */
    private fun applyTabSizing(button: Button) {
        val base = this.baseLayouts[button] ?: return
        if (this.sideValue.isVertical) {
            button.layoutSpec.width = 100.percent
            button.layoutSpec.height = base.height
        } else {
            button.layoutSpec.width = base.width
            button.layoutSpec.height = 100.percent
        }
    }

    /** The size a button named before the bar started stretching it. */
    private data class BaseLayout(val width: Length?, val height: Length?)

    /** Mirrors the selection onto components bound through a standalone bar. */
    private fun updateDetachedVisibility() {
        for ((button, components) in this.detachedBindings) {
            val visible = button === this.selectedValue
            for (component in components) {
                component.visible = visible
            }
        }
    }

    /**
     * Paints the bar and keeps the tabs' transition in step with the bar's own style.
     *
     * The bar receives the pane's style just before it is placed, so this is also where a pane
     * transition reaches the buttons it should animate.
     */
    internal override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        syncInheritedStyle()
    }

    /**
     * Wraps the tabs in a scroll pane.
     *
     * The bar itself is still the styled surface; the scroll pane only clips the tab row or column
     * and supplies the bars when the tabs outgrow the space the pane gives the bar. The inner pane
     * is at least as large as the viewport, so a short row still fills the bar rather than leaving a
     * bare strip beside it.
     */
    override fun buildNode(): Node {
        val content = ScrollContentPane()
        val pane = ScrollPane(content)
        pane.hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        pane.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        // The content controls its own extent; forcing it to fit would remove the overflow the
        // scroll bars exist for.
        pane.isFitToWidth = false
        pane.isFitToHeight = false
        pane.isPannable = true
        pane.styleClass += STYLE_CLASS
        TabBar::class.java.getResource(STYLESHEET)?.let { pane.stylesheets += it.toExternalForm() }
        pane.viewportBoundsProperty().addListener { _, _, bounds ->
            // Keep the content at least as large as the viewport in both directions. Overflow on
            // either axis then becomes a scroll bar, while a small row still paints the whole bar.
            content.minWidth = bounds.width
            content.minHeight = bounds.height
            content.requestLayout()
            // The first viewport arrives as part of the layout that creates the tabs, so the
            // selected one can only be brought into view once that pass has finished.
            Platform.runLater { ensureSelectedVisible() }
        }
        this.scrollPane = pane
        this.scrollContent = content
        WindowDragger.install(content) { this.windowDragEnabled }
        return pane
    }

    /** The scroll pane owns the JavaFX node, so attach the inner content pane as the host. */
    override fun onNodeCreated(node: Node) {
        if (node is ScrollPane) {
            this.scrollContent?.let { attachContent(it) }
            scheduleSelectedVisible()
        }
    }

    /**
     * The width the whole tab row needs, before clipping.
     *
     * A vertical bar stacks one tab per line and stretches the tabs to its own width, so only an
     * explicitly wider tab can make it scroll sideways. A horizontal bar needs the sum of every tab
     * width plus the gaps; that is what the horizontal scroll bar measures.
     */
    internal fun scrollContentWidth(): Double {
        val inset = this.layoutSpec.hPadding.resolve(0.0, this.resolvedFontSize)
        if (this.children.isEmpty()) return inset * 2.0
        if (this.sideValue.isVertical) {
            val widest = this.children.maxOfOrNull { child ->
                val width = child.layoutSpec.width
                if (width == null || width is Percent) 0.0
                else width.resolve(0.0, child.resolvedFontSize)
            } ?: 0.0
            return inset * 2.0 + widest
        }
        val gap = this.gap.resolve(0.0, this.resolvedFontSize)
        var total = inset * 2.0 + gap * (this.children.size - 1)
        for (child in this.children) {
            val width = child.layoutSpec.width
            total += if (width == null || width is Percent) preferredWidth(child)
            else width.resolve(0.0, child.resolvedFontSize)
        }
        return total
    }

    /**
     * The height the whole tab column needs, before clipping.
     *
     * The mirror image of [scrollContentWidth]: a horizontal bar is one row high, while a vertical
     * bar adds the heights of its tabs and the gaps between them.
     */
    internal fun scrollContentHeight(width: Double): Double {
        val inset = this.layoutSpec.vPadding.resolve(0.0, this.resolvedFontSize)
        if (this.children.isEmpty()) return inset * 2.0
        if (this.sideValue.isVertical) {
            val gap = this.gap.resolve(0.0, this.resolvedFontSize)
            var total = inset * 2.0 + gap * (this.children.size - 1)
            for (child in this.children) {
                val height = child.layoutSpec.height
                total += if (height == null || height is Percent) {
                    preferredHeight(
                        child,
                        child.layoutSpec.width?.resolve(width, child.resolvedFontSize) ?: width,
                    )
                } else {
                    height.resolve(0.0, child.resolvedFontSize)
                }
            }
            return total
        }
        val tallest = this.children.maxOfOrNull { child ->
            val height = child.layoutSpec.height
            if (height == null || height is Percent) 0.0
            else height.resolve(0.0, child.resolvedFontSize)
        } ?: 0.0
        return inset * 2.0 + tallest
    }

    /**
     * Scrolls a newly selected tab into view after the next layout pulse.
     *
     * Selection can move without a click — [add] selects the tab it adds — so without this a
     * programmatically selected tab could sit past the clipped edge and look as if nothing had been
     * selected. The delay lets the scroll pane learn its new content extent first.
     */
    private fun scheduleSelectedVisible() {
        if (this.scrollPane == null) return
        Fx.runOnFxThread {
            Platform.runLater { ensureSelectedVisible() }
        }
    }

    /** Moves [selected] just inside the viewport when it currently lies outside it. */
    private fun ensureSelectedVisible() {
        val pane = this.scrollPane ?: return
        val button = this.selectedValue ?: return
        val node = button.builtNode ?: return
        val content = this.scrollContent ?: return
        val viewport = pane.viewportBounds
        if (viewport.width <= 0.0 || viewport.height <= 0.0) return
        val bounds = node.boundsInParent

        val overflowX = (content.width - viewport.width).coerceAtLeast(0.0)
        if (overflowX > 0.0) {
            val current = pane.hvalue * overflowX
            val target = when {
                bounds.minX < current -> bounds.minX
                bounds.maxX > current + viewport.width -> bounds.maxX - viewport.width
                else -> current
            }
            pane.hvalue = (target / overflowX).coerceIn(0.0, 1.0)
        }

        val overflowY = (content.height - viewport.height).coerceAtLeast(0.0)
        if (overflowY > 0.0) {
            val current = pane.vvalue * overflowY
            val target = when {
                bounds.minY < current -> bounds.minY
                bounds.maxY > current + viewport.height -> bounds.maxY - viewport.height
                else -> current
            }
            pane.vvalue = (target / overflowY).coerceIn(0.0, 1.0)
        }
    }

    /** The content pane of the scroll pane, sized from the tabs' own dimensions. */
    private inner class ScrollContentPane : Pane() {
        init {
            this.isPickOnBounds = true
        }

        override fun computePrefWidth(height: Double): Double = this@TabBar.scrollContentWidth()

        override fun computePrefHeight(width: Double): Double = this@TabBar.scrollContentHeight(width)

        override fun computeMinWidth(height: Double): Double = computePrefWidth(height)

        override fun computeMinHeight(width: Double): Double = computePrefHeight(width)
    }

    /**
     * Restores the tab bar defaults: side [TabSide.UP], the first tab selected and window dragging
     * off.
     *
     * The buttons themselves are children of the bar and survive a reset, as every container's
     * children do.
     */
    override fun reset() {
        super.reset()
        this.sideValue = TabSide.UP
        this.justify = Justify.CENTER
        this.windowDragEnabled = false
        applyTabSizing()
        this.selectedValue = this.tabs.firstOrNull()
        this.selectionStyle.reset()
        applySelectionStyle()
        updateDetachedVisibility()
        this.selectionListener?.invoke(this.selectedValue)
    }

    /**
     * Computes the vertical slots for [TabSide.LEFT] and [TabSide.RIGHT].
     *
     * A horizontal bar keeps the normal [FlexBox] flow. A vertical one stacks its buttons from the
     * top, because a flow that wrapped into columns would read as several columns of tabs rather
     * than one column.
     */
    override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        for (child in this.children) {
            // The bar owns the tab positions, so a caller-written x or y cannot move a tab.
            child.layoutSpec.x = Length.ZERO
            child.layoutSpec.y = Length.ZERO
        }

        if (!this.sideValue.isVertical) {
            this.verticalSlots = emptyMap()
            super.beginPass(box, targets)
            return
        }

        val gap = this.gap.resolve(box.height, this.resolvedFontSize)
        val result = LinkedHashMap<Component<*>, Slot>(this.children.size)
        var y = box.offsetY
        for (child in this.children) {
            val width = child.layoutSpec.width?.resolve(box.width, child.resolvedFontSize) ?: box.width
            val height = child.layoutSpec.height?.resolve(box.height, child.resolvedFontSize)
                ?: preferredHeight(child, width)
            result[child] = Slot(box.offsetX, y, height)
            y += height + gap
        }
        this.verticalSlots = result
    }

    /** Answers with the vertical slot computed by [beginPass]. */
    override fun slotFor(child: Component<*>, box: ContentBox): Slot =
        if (this.sideValue.isVertical) {
            this.verticalSlots[child] ?: Slot(box.offsetX, box.offsetY)
        } else {
            super.slotFor(child, box)
        }

    /** The natural width of [child], used for a tab whose width is not a pixel count. */
    private fun preferredWidth(child: Component<*>): Double {
        val node = child.node
        if (node is Region) {
            if (node.scene != null) node.applyCss()
            return node.prefWidth(-1.0)
        }
        return node.boundsInLocal.width
    }

    /** The natural height of [child] at [width], used for a tab without an explicit height. */
    private fun preferredHeight(child: Component<*>, width: Double): Double {
        val node = child.node
        if (node is Region) {
            if (node.scene != null) node.applyCss()
            return node.prefHeight(width)
        }
        return node.boundsInLocal.height
    }

    private companion object {
        /** Style class every tab bar carries; the scroll-bar styling hangs off it. */
        const val STYLE_CLASS: String = "ovatoxin-tabbar"

        /** The stylesheet of the scroll bars, next to this class in `resources`. */
        const val STYLESHEET: String = "tabbar.css"
    }
}
