package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.DrawScope
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.action.Button
import javafx.scene.layout.Region
import javafx.scene.paint.Color

/**
 * The strip of tab buttons belonging to a [TabPane].
 *
 * A tab bar is a [FlexBox]: it flows its buttons either horizontally (side [TabSide.UP] or
 * [TabSide.BOTTOM]) or vertically (side [TabSide.LEFT] or [TabSide.RIGHT]). The `x` and `y` of a
 * tab button are ignored, because the bar decides where the buttons go. The bar stretches every
 * button across its thickness — `width = 100.percent` on the vertical sides and
 * `height = 100.percent` on the horizontal ones — while the other axis keeps the size the button
 * named. A horizontal bar also centres its tabs by default (`justify = Justify.CENTER`).
 *
 * ```kotlin
 * val tabs = TabBar()
 * tabs.side = TabSide.LEFT
 *
 * val first = tabs.add(Button("One")) { text = "One" }
 * val second = tabs.add(Button("Two")) { text = "Two" }
 * tabs.select(first)
 * ```
 *
 * [add] selects the new button. Clicking a button selects it too. [bind] takes the target tab
 * explicitly. [remove] removes the button together with its tab position, selecting the first
 * remaining tab when the removed one was selected.
 *
 * A [selectColor] paints the background of the selected button with that colour, while every other
 * button keeps the style it was given. When it is `null` (the default) the bar paints no
 * selected/unselected difference at all. A [TabPane] also uses the selection to show and hide the
 * bound components.
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
            this.parent?.relayout()
        }

    /**
     * The colour painted on the selected tab, or `null` to leave every tab exactly as it was given.
     *
     * Defaults to `null`. Only the background of the selected button changes. An unselected tab
     * with no background of its own stays transparent, so the bar's fill — usually the pane's
     * background — shows through instead of the button control's default white.
     */
    var selectColor: Color? = null
        set(value) {
            if (field == value) return
            field = value
            applySelectionStyle()
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
     * Paints [selectColor] on the selected button and restores every other button to its base style.
     *
     * The base style is captured when the button is added, so a selected colour is never folded
     * back into it and switching tabs restores the caller's original appearance.
     */
    private fun applySelectionStyle() {
        val color = this.selectColor
        for (button in this.tabs) {
            val base = this.baseStyles[button] ?: continue
            button.styleSpec.copyFrom(base)
            applyInheritedTransition(button, base)
            if (button === this.selectedValue && color != null) {
                button.styleSpec.background.color = color
            }
        }
        relayout()
    }

    /**
     * Gives every tab the bar's transition unless the button named one itself.
     *
     * The bar copies the owning pane's style, so a `transition` written on a [TabPane] reaches its
     * tabs and animates the [selectColor] change. An explicit transition on the button wins.
     */
    internal fun syncInheritedTransition() {
        for (button in this.tabs) {
            val base = this.baseStyles[button] ?: continue
            applyInheritedTransition(button, base)
        }
    }

    private fun applyInheritedTransition(button: Button, base: Style) {
        if (base.transition == null) {
            button.styleSpec.transition = this.styleSpec.transition
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
        syncInheritedTransition()
    }

    /**
     * Restores the tab bar defaults: side [TabSide.UP] and the first tab selected.
     *
     * The buttons themselves are children of the bar and survive a reset, as every container's
     * children do.
     */
    override fun reset() {
        super.reset()
        this.sideValue = TabSide.UP
        this.justify = Justify.CENTER
        applyTabSizing()
        this.selectedValue = this.tabs.firstOrNull()
        this.selectColor = null
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

    /** The natural height of [child] at [width], used for a tab without an explicit height. */
    private fun preferredHeight(child: Component<*>, width: Double): Double {
        val node = child.node
        return if (node is Region) node.prefHeight(width) else node.boundsInLocal.height
    }
}
