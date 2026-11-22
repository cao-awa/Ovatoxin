package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.paint.Color
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The visual appearance of a component.
 *
 * The library draws every component the same flat way: **a fill and no border**. There is no
 * border support on purpose, so a component is a filled box, optionally with rounded corners.
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     layout {
 *         width = 80.percent
 *         height = 10.percent
 *     }
 *     button.style {
 *         background {
 *             color = Color.web("#4c8bf5")
 *         }
 *         corner = 50.percent // A pill: half of the shorter side
 *     }
 * }
 * ```
 *
 * A percentage [corner] resolves against the *shorter* side of the box, so `50.percent` is
 * always the roundest possible corner — a pill for a wide box, a circle for a square one — no
 * matter how the window is resized.
 */
class Style {

    /**
     * The fill of the component, configured in a [Background] block.
     *
     * ```kotlin
     * style {
     *     background {
     *         color = Style.DEFAULT_COLOR
     *     }
     * }
     * ```
     *
     * Defaults to the plain fill of [DEFAULT_COLOR]. Because the library renders no border, this
     * fill is all that defines the shape of the component.
     */
    val background: Background = Background()

    /**
     * The corner radius. Defaults to [DEFAULT_CORNER], i.e. square corners.
     *
     * Percentages resolve against the shorter side of the box, pixels are used as they are.
     */
    var corner: Length = DEFAULT_CORNER

    /**
     * The colour of the text drawn by the component, or `null` to keep the colour the component
     * itself brings along. Defaults to `null`.
     *
     * ```kotlin
     * style {
     *     foreground = Color.web("#4c8bf5")
     * }
     * ```
     *
     * A component with no text of its own — a [com.github.kusa233.ovatoxin.widget.Div] or a
     * [com.github.kusa233.ovatoxin.window.Frame] — simply ignores it.
     */
    var foreground: Color? = DEFAULT_FOREGROUND

    /** Replaces every property with the values configured in [block]. */
    fun configure(block: Style.() -> Unit): Style {
        block()
        return this
    }

    /**
     * Configures the [background] in place:
     *
     * ```kotlin
     * style {
     *     background {
     *         color = Style.DEFAULT_COLOR
     *     }
     * }
     * ```
     *
     * The block mutates the existing [Background] rather than replacing it, so anything set
     * before the block — a frame's surface colour, or an earlier `background { … }` — is kept
     * unless the block overrides that same property.
     */
    fun background(block: Background.() -> Unit): Style {
        this.background.configure(block)
        return this
    }

    /** Restores every property to the documented default: a plain white fill, square, no text colour. */
    internal fun reset() {
        this.background.reset()
        this.corner = DEFAULT_CORNER
        this.foreground = DEFAULT_FOREGROUND
    }

    /** Copies every property of [other] into this style. */
    fun copyFrom(other: Style) {
        this.background.copyFrom(other.background)
        this.corner = other.corner
        this.foreground = other.foreground
    }

    /** Resolves [corner] against the shorter side of a `width` x `height` box, in pixels. */
    fun cornerPixels(width: Double, height: Double): Double =
        this.corner.resolve(min(width, height)).coerceAtLeast(0.0)

    /**
     * Renders this style as the inline CSS of a `width` x `height` box.
     *
     * The border is always disabled and the focus ring is suppressed, so the computed appearance
     * is exactly the fill with the requested radius.
     */
    fun toCss(width: Double, height: Double): String {
        val radius = cornerPixels(width, height).css()
        val fill = this.background.cssColor()
        val image = this.background.image
        val textFill = this.foreground
        return buildString {
            append("-fx-background-color: ").append(fill).append("; ")
            if (image != null) {
                append("-fx-background-image: url(\"").append(cssUrl(image)).append("\"); ")
                append("-fx-background-size: cover; ")
                append("-fx-background-position: center center; ")
                append("-fx-background-repeat: no-repeat; ")
            }
            append("-fx-background-insets: 0; ")
            append("-fx-background-radius: ").append(radius).append("px; ")
            textFill?.let { append("-fx-text-fill: ").append(cssColor(it)).append("; ") }
            append("-fx-border-width: 0; ")
            append("-fx-focus-color: transparent; ")
            append("-fx-faint-focus-color: transparent;")
        }
    }

    override fun toString(): String =
        "Style(background=${this.background}, corner=${this.corner}, foreground=${this.foreground})"

    companion object {
        /** The fill every component uses until a style overrides it. */
        val DEFAULT_COLOR: Color = Color.web("#ffffff")

        /** The corner radius every component uses until a style overrides it: square. */
        val DEFAULT_CORNER: Length = 0.px

        /**
         * The text colour every component starts with: `null`, meaning the component keeps the
         * colour it brings along itself.
         */
        val DEFAULT_FOREGROUND: Color? = null
    }
}

/** Formats the colour layer, or `transparent` when there is nothing to paint. */
private fun Background.cssColor(): String =
    if (this.image != null) {
        "transparent"
    } else {
        this.color?.let { cssColor(it, this.opacity) } ?: "transparent"
    }

/**
 * Formats a color as `rgba()` so a partially transparent fill stays expressible in CSS.
 *
 * [opacity] is the whole percentage from [Background.opacity] and scales the alpha the colour
 * already carries, so `Color.web("#4c8bf5", 0.5)` at `opacity = 50` ends up a quarter opaque.
 */
internal fun cssColor(color: Color, opacity: Int = Background.MAX_OPACITY): String {
    val red = (color.red * 255.0).roundToInt()
    val green = (color.green * 255.0).roundToInt()
    val blue = (color.blue * 255.0).roundToInt()
    val alpha = color.opacity * opacity / Background.MAX_OPACITY.toDouble()
    return if (alpha >= 1.0) {
        "#%02x%02x%02x".format(red, green, blue)
    } else {
        "rgba($red, $green, $blue, ${alpha.css()})"
    }
}

/** Escapes a URL so it survives as a quoted CSS string. */
private fun cssUrl(url: String): String =
    url.replace("\\", "\\\\").replace("\"", "\\\"")

/** Formats a pixel amount with at most two decimals, so CSS stays short and readable. */
private fun Double.css(): String {
    val rounded = (this * 100.0).roundToInt() / 100.0
    val asLong = rounded.toLong()
    return if (rounded == asLong.toDouble()) asLong.toString() else rounded.toString()
}
