package com.github.kusa233.ovatoxin.layout

/**
 * The position and the size of a component inside its parent container.
 *
 * Every value is a [Length], so absolute pixels and percentages can be mixed freely:
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
 * When [width] or [height] is `null` the component keeps its natural (content based) size.
 */
class Layout {

    /** Horizontal position, relative to the width of the parent. Defaults to `0`. */
    var x: Length = Length.ZERO

    /** Vertical position, relative to the height of the parent. Defaults to `0`. */
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
