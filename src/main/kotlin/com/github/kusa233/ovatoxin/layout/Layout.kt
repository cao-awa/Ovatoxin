package com.github.kusa233.ovatoxin.layout

import com.github.kusa233.ovatoxin.component.LeafBox
import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl

/**
 * The position and the size of a component inside its parent container.
 *
 * Every value is a [Length], so each of the four slots takes any of the three units — a percentage for
 * a size that follows the window, a pixel for one that must not, an `em` for one that follows the
 * font:
 *
 * ```kotlin
 * layout {
 *     x = 10.percent
 *     y = 10.px
 *     width = 80.percent
 *     height = 10.percent
 *     vPadding = 0.5.em
 * }
 * ```
 *
 * Choosing a unit per slot is not the same as mixing units inside one value: a [Percent] only does
 * arithmetic with a [Percent], an [Em] only with an [Em], and a [Pixel] only with a [Pixel], so no
 * slot can end up holding a length whose unit is half of one and half of another.
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
 * The inset belongs to the *container*, so for a component that holds children it only affects the
 * children inside it — see [com.github.kusa233.ovatoxin.component.Container.contentBox] for how the
 * room they take is cut off. A component with no children of its own (a button, a text field, …)
 * instead applies the padding to its own drawn box: horizontally it is narrowed by `hPadding` on
 * each side, and vertically it prefers to *move* — keeping its height and centring itself in the
 * band it was given, so the space above and below is equal — and only gives up height (`vPadding`
 * clear on each side) when it would not fit. See [insetBox].
 *
 * A `layout { }` block is a marked DSL scope: it hides the blocks around it, so a property that
 * does not belong to it — a `corner`, a `color`, a component's `text` — is a compile error here
 * instead of quietly configuring the enclosing block.
 */
@OvatoxinDsl
class Layout {

    /**
     * Horizontal offset from the left edge of the parent: `10.percent` of its width, `10.px`, or a
     * multiple of the font size. Defaults to [Length.ZERO], i.e. flush left.
     */
    var x: Length = Length.ZERO

    /**
     * Vertical offset from the top edge of the parent: `10.percent` of its height, `10.px`, or a
     * multiple of the font size. Defaults to [Length.ZERO], i.e. flush top.
     */
    var y: Length = Length.ZERO

    /**
     * Width of the component. `null` means "as wide as the content needs".
     *
     * An `em` width resolves against the font size of this very component, so a box one `em` wide is
     * one line of the text it holds.
     */
    var width: Length? = null

    /**
     * Height of the component. `null` means "as tall as the content needs".
     *
     * An `em` height resolves against the font size of this very component.
     */
    var height: Length? = null

    /**
     * How far the children of this component are kept from its left and right edge.
     *
     * The inset is symmetric — the same amount is cut off on both sides — and it is a *content
     * inset*, not a shift: the room left over shrinks by the padding twice, so a child asking for
     * `100.percent` still ends one padding short of the edge instead of running over it. A
     * percentage resolves against the width of the component itself, an `em` against its font size —
     * so `hPadding = 1.em` keeps exactly one line of text clear on each side.
     *
     * Defaults to [Length.ZERO], i.e. flush. A component that holds children uses it as a *content
     * inset* for those children — see
     * [com.github.kusa233.ovatoxin.component.Container.contentBox]. A component that has no children
     * of its own (a button, a text field, …) uses it to inset its own box instead: the drawn fill
     * and the control shrink by the padding on each side, and the space that opens up is left clear
     * — see [insetBox]. It is an inset and
     * nothing else: the space *between* the children of a
     * [com.github.kusa233.ovatoxin.widget.container.FlexBox] is its
     * [gap][com.github.kusa233.ovatoxin.widget.container.FlexBox.gap], which is a value of the box itself.
     */
    var hPadding: Length = Length.ZERO

    /**
     * How far the children of this component are kept from its top and bottom edge.
     *
     * The vertical counterpart of [hPadding] for a component that holds children: an inset, never the
     * space between them. For a component with no children of its own the padding is a *minimum*
     * gap above and below, and the box tries to move before it shrinks: kept at its own height, it is
     * centred in the band the flow left for it so the gap above and below come out equal, and only
     * when the box plus twice the padding would overrun that band does it give up height, leaving
     * [vPadding] clear on each side. Defaults to [Length.ZERO].
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

/**
 * The rectangle a leaf component is drawn into, after this layout's padding is applied to its box.
 *
 * A component that holds children insets its children with [hPadding]/[vPadding]; a leaf has no
 * children, so the same two values instead act on the box the leaf itself occupies. This computes
 * that box for a leaf that would otherwise fill `x`, `y`, `width`, `height`.
 *
 * Horizontally the padding is a plain inset: the box is narrowed by [hPadding] on each side (capped
 * at half its width so it never turns negative).
 *
 * Vertically the padding is a *minimum* gap and the box moves before it shrinks. When [bandHeight]
 * is given it is the room the flow reserved for the child, and the box keeps its `height`, sits in
 * the middle of that band, and so leaves the same space above and below — no shrinking at all. Only
 * when the box plus twice [vPadding] would overrun the band does it give height up, shrinking to
 * `bandHeight - 2 * vPadding` and keeping `vPadding` clear at the top and the bottom. Without a
 * [bandHeight] the box is its own band, so there is no room to move into and the vertical padding is
 * a plain inset, exactly like the horizontal one.
 *
 * A percentage resolves against the band (or the box, without a band) and an `em` against
 * [fontSize], so `vPadding = 5.percent` of a `93.px` tall row is `4.65.px` at the top and bottom when
 * the button is too tall to fit, or simply centres it when there is room to spare.
 *
 * @param x the left edge of the box before padding, in pixels.
 * @param y the top edge of the box, which is also the top of the band, in pixels.
 * @param width the width of the box before padding, in pixels.
 * @param height the height of the box before padding, in pixels.
 * @param fontSize the size an `em` padding resolves against.
 * @param bandHeight the room the flow reserved for the child, in pixels, or `null` to use [height].
 * @return the drawn box, in pixels.
 */
internal fun Layout.insetBox(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    fontSize: Double = Length.DEFAULT_FONT_SIZE,
    bandHeight: Double? = null,
): LeafBox {
    val insetX = this.hPadding.resolve(width, fontSize).coerceIn(0.0, width / 2.0)
    val drawnX = x + insetX
    val drawnWidth = width - insetX * 2.0

    val band = bandHeight ?: height
    val minGap = this.vPadding.resolve(band, fontSize).coerceIn(0.0, band / 2.0)
    val slack = (band - height).coerceAtLeast(0.0)
    return if (slack >= minGap * 2.0) {
        // Enough room to honour the padding by moving: keep the height, centre it in the band.
        LeafBox(x = drawnX, y = y + slack / 2.0, width = drawnWidth, height = height)
    } else {
        // Not enough room: give up height so the minimum gap stays clear at the top and bottom.
        val drawnHeight = (band - minGap * 2.0).coerceAtLeast(0.0)
        LeafBox(x = drawnX, y = y + (band - drawnHeight) / 2.0, width = drawnWidth, height = drawnHeight)
    }
}
