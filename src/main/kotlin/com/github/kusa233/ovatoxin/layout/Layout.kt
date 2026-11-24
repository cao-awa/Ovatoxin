package com.github.kusa233.ovatoxin.layout

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl

/**
 * The position and the size of a component inside its parent container.
 *
 * Every value is a [Length], so each of the four slots takes either unit — a percentage for a size
 * that follows the window, a pixel for one that must not:
 *
 * ```kotlin
 * layout {
 *     x = 10.percent
 *     y = 10.px
 *     width = 80.percent
 *     height = 10.percent
 * }
 * ```
 *
 * Choosing a unit per slot is not the same as mixing units inside one value: a [Percent] only does
 * arithmetic with a [Percent] and a [Pixel] only with a [Pixel], so no slot can end up holding a
 * length whose unit is half of one and half of the other.
 *
 * When [width] or [height] is `null` the component keeps its natural (content based) size.
 *
 * A component that holds children also keeps its content away from its own edges with [hPadding]
 * and [vPadding] — the same block, because the inset is part of how the component is laid out:
 *
 * ```kotlin
 * draw(Div()) { panel ->
 *     layout {
 *         x = 10.percent
 *         width = 80.percent
 *         hPadding = 12.px
 *         vPadding = 12.px
 *     }
 *     panel.layout { draw(TextArea()) { it.layout { width = 100.percent } } }
 * }
 * ```
 *
 * The inset belongs to the *container*, so it only affects the children inside it; for a component
 * that has no children the two values change nothing. See
 * [com.github.kusa233.ovatoxin.component.Container.contentBox] for how the room they take is cut
 * off.
 *
 * A `layout { }` block is a marked DSL scope: it hides the blocks around it, so a property that
 * does not belong to it — a `corner`, a `color`, a component's `text` — is a compile error here
 * instead of quietly configuring the enclosing block.
 */
@OvatoxinDsl
class Layout {

    /**
     * Horizontal offset from the left edge of the parent: `10.percent` of its width, or `10.px`.
     * Defaults to [Length.ZERO], i.e. flush left.
     */
    var x: Length = Length.ZERO

    /**
     * Vertical offset from the top edge of the parent: `10.percent` of its height, or `10.px`.
     * Defaults to [Length.ZERO], i.e. flush top.
     */
    var y: Length = Length.ZERO

    /** Width of the component. `null` means "as wide as the content needs". */
    var width: Length? = null

    /** Height of the component. `null` means "as tall as the content needs". */
    var height: Length? = null

    /**
     * How far the children of this component are kept from its left and right edge.
     *
     * The inset is symmetric — the same amount is cut off on both sides — and it is a *content
     * inset*, not a shift: the room left over shrinks by the padding twice, so a child asking for
     * `100.percent` still ends one padding short of the edge instead of running over it. A
     * percentage resolves against the width of the component itself.
     *
     * Defaults to [Length.ZERO], i.e. flush. Only a component that holds children is affected by
     * it — see [com.github.kusa233.ovatoxin.component.Container.contentBox]. It is an inset and
     * nothing else: the space *between* the children of a
     * [com.github.kusa233.ovatoxin.widget.container.FlexBox] is its
     * [gap][com.github.kusa233.ovatoxin.widget.container.FlexBox.gap], which is a value of the box itself.
     */
    var hPadding: Length = Length.ZERO

    /**
     * How far the children of this component are kept from its top and bottom edge.
     *
     * The vertical counterpart of [hPadding]: an inset, never the space between the children.
     * Defaults to [Length.ZERO].
     */
    var vPadding: Length = Length.ZERO

    /** Applies [block] to this layout and returns it. */
    fun configure(block: Layout.() -> Unit): Layout {
        block(this)
        return this
    }

    /** Restores every value to the documented default: top left corner, unsized, unpadded. */
    internal fun reset() {
        this.x = Length.ZERO
        this.y = Length.ZERO
        this.width = null
        this.height = null
        this.hPadding = Length.ZERO
        this.vPadding = Length.ZERO
    }

    /** Copies every value of [other] into this layout. */
    fun copyFrom(other: Layout) {
        this.x = other.x
        this.y = other.y
        this.width = other.width
        this.height = other.height
        this.hPadding = other.hPadding
        this.vPadding = other.vPadding
    }

    override fun toString(): String =
        "Layout(x=${this.x}, y=${this.y}, width=${this.width}, height=${this.height}, " +
            "hPadding=${this.hPadding}, vPadding=${this.vPadding})"
}
