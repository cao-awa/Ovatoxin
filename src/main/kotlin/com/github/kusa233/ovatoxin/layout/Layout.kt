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

    /** Applies [block] to this layout and returns it. */
    fun configure(block: Layout.() -> Unit): Layout {
        block(this)
        return this
    }

    /** Restores every value to the documented default, i.e. to the top left corner, unsized. */
    internal fun reset() {
        this.x = Length.ZERO
        this.y = Length.ZERO
        this.width = null
        this.height = null
    }

    /** Copies every value of [other] into this layout. */
    fun copyFrom(other: Layout) {
        this.x = other.x
        this.y = other.y
        this.width = other.width
        this.height = other.height
    }

    override fun toString(): String =
        "Layout(x=${this.x}, y=${this.y}, width=${this.width}, height=${this.height})"
}
