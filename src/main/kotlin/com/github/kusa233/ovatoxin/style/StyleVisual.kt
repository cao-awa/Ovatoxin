package com.github.kusa233.ovatoxin.style

import javafx.scene.paint.Color

/**
 * The concrete, pixel-resolved appearance of a [Style] at one instant.
 *
 * A [Style] stores values that may still be relative — a percentage corner needs the box, an
 * opacity is a whole percentage — so it cannot be animated as it stands. [Style.resolveVisual]
 * freezes it for a box of known size, and this snapshot is what the animation interpolates: two
 * snapshots are blended frame by frame and rendered again.
 *
 * [background] is the effective fill colour with the [Background.opacity] already folded into its
 * alpha; it is `null` when the style paints an image or no colour at all. [corner] is a radius in
 * pixels, [foreground] is the text colour or `null`, [borderSize] is the outline thickness in
 * pixels, and [borderColor] is its colour or `null`.
 */
internal data class StyleVisual(
    val background: Color?,
    val corner: Double,
    val foreground: Color?,
    val borderSize: Double = 0.0,
    val borderColor: Color? = null,
) {

    /** Whether this snapshot paints exactly the same thing as [other]. */
    fun matches(other: StyleVisual): Boolean =
        this.background == other.background &&
            this.corner == other.corner &&
            this.foreground == other.foreground &&
            this.borderSize == other.borderSize &&
            this.borderColor == other.borderColor

    /**
     * Blends this snapshot towards [target] by [progress] in `0..1`.
     *
     * Colours are interpolated channel by channel, including alpha, so a fill or border that fades
     * in or out also fades its transparency. A missing colour is treated as a fully transparent
     * version of the other side, which makes "no colour to a colour" a plain fade rather than a
     * jump. The corner radius and border thickness are interpolated numerically.
     */
    fun lerp(target: StyleVisual, progress: Double): StyleVisual {
        val t = progress.coerceIn(0.0, 1.0)
        return StyleVisual(
            background = lerpColor(this.background, target.background, t),
            corner = this.corner + (target.corner - this.corner) * t,
            foreground = lerpColor(this.foreground, target.foreground, t),
            borderSize = this.borderSize + (target.borderSize - this.borderSize) * t,
            borderColor = lerpColor(this.borderColor, target.borderColor, t),
        )
    }
}

/** Blends two optional colors; `null` fades against transparency instead of jumping. */
private fun lerpColor(from: Color?, to: Color?, t: Double): Color? = when {
    t <= 0.0 -> from
    t >= 1.0 -> to
    from == null && to == null -> null
    from == null -> to!!.let { Color(it.red, it.green, it.blue, it.opacity * t) }
    to == null -> Color(from.red, from.green, from.blue, from.opacity * (1.0 - t))
    else -> Color(
        from.red + (to.red - from.red) * t,
        from.green + (to.green - from.green) * t,
        from.blue + (to.blue - from.blue) * t,
        from.opacity + (to.opacity - from.opacity) * t,
    )
}
