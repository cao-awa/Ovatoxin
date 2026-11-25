package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Em
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
 *     style {
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
 *
 * A `style { }` block is a marked DSL scope, and so is the [background][background] block inside
 * it: a `color`, which belongs to the background, cannot be written directly in the style block,
 * and an `x`, which belongs to the layout, cannot be written here at all.
 */
@OvatoxinDsl
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
     * Percentages resolve against the shorter side of the box, pixels are used as they are, and an
     * `em` resolves against the font size of the component.
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
     * A component with no text of its own — a [com.github.kusa233.ovatoxin.widget.container.Div] or a
     * [com.github.kusa233.ovatoxin.window.Frame] — simply ignores it.
     */
    var foreground: Color? = DEFAULT_FOREGROUND

    /**
     * The font size that `em` lengths are resolved against inside this component — its own
     * `hPadding`, `vPadding` and [corner], the `gap` of a flowing box, and every `em` written by a
     * child that carries no font size of its own. Defaults to [DEFAULT_FONT_SIZE], i.e. "inherit".
     *
     * It is a [Length], but only two of the units mean anything here: a [Pixel][com.github.kusa233.ovatoxin.layout.Pixel]
     * is an absolute size, and a [Percent][com.github.kusa233.ovatoxin.layout.Percent] is that
     * fraction of the height of the component's **own box**, so the text scales with the box it is
     * drawn in: a `50.percent` font on a component `40.px` tall is `20px` of text. An `em` is
     * **refused** with an [IllegalArgumentException], because an `em` is defined as a multiple of
     * the font size and would therefore be measuring the font against itself:
     *
     * ```kotlin
     * style { fontSize = 20.px }        // twenty device independent pixels
     * style { fontSize = 50.percent }   // half of the component's own box height
     * style { fontSize = 1.em }         // throws: an em has no font size to lean on
     * ```
     *
     * A percentage needs the box height, which is not known before the component is laid out.
     * Because a component whose height is `auto` or written in `em` would grow with the very font
     * being resolved, a percentage only applies when the height is a pixel or percentage value; on
     * an `auto`/`em` height it falls back to the inherited size. Give the component an explicit
     * `height` to make a percentage font size meaningful.
     *
     * `null` means *inherit*: the size is taken from the enclosing component, and a component in an
     * unbroken chain of `null`s resolves against
     * [Length.DEFAULT_FONT_SIZE][com.github.kusa233.ovatoxin.layout.Length.DEFAULT_FONT_SIZE]. A
     * component that draws text of its own carries a size and is not asked — a
     * [TextArea][com.github.kusa233.ovatoxin.widget.text.TextArea] always resolves against the font
     * it renders its text with.
     *
     * ```kotlin
     * draw(Div()) { panel ->
     *     layout {
     *         width = 40.em          // 40 lines of the panel's own font
     *         hPadding = 1.em        // one line of it, kept clear on each side
     *     }
     *     style {
     *         fontSize = 20.px       // the panel and its children now resolve em against 20px
     *     }
     *     panel.layout {
     *         draw(TextArea("Hello")) { title ->
     *             style { fontSize = 32.px }
     *             // the title's own ems are 32px each; the panel's stay at 20px
     *         }
     *     }
     * }
     * ```
     *
     * The value lives in the style rather than in the component itself because it is part of how
     * the component is *drawn* — like [corner], it is what an `em` is measured with — and a
     * redraw therefore drops it together with the rest of the style, back to inheriting.
     */
    var fontSize: Length? = DEFAULT_FONT_SIZE
        set(value) {
            // An em is a multiple of the font size, so it cannot also be the font size. Refusing it
            // here points the error at the line that wrote it rather than at a later layout pass.
            if (value is Em) {
                throw IllegalArgumentException(
                    "fontSize is a font size and cannot be measured in em: an em is a multiple of " +
                        "the font size, so it would measure the font against itself. Use px or percent.",
                )
            }
            field = value
        }

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

    /**
     * Restores every property to the documented default: a plain white fill, square, no text
     * colour, and a font size inherited from the parent component.
     */
    internal fun reset() {
        this.background.reset()
        this.corner = DEFAULT_CORNER
        this.foreground = DEFAULT_FOREGROUND
        this.fontSize = DEFAULT_FONT_SIZE
    }

    /** Copies every property of [other] into this style. */
    fun copyFrom(other: Style) {
        this.background.copyFrom(other.background)
        this.corner = other.corner
        this.foreground = other.foreground
        this.fontSize = other.fontSize
    }

    /**
     * Resolves [corner] against the shorter side of a `width` x `height` box, in pixels.
     *
     * An `em` corner resolves against [fontSize], the font size of the component the style belongs
     * to.
     */
    fun cornerPixels(
        width: Double,
        height: Double,
        fontSize: Double = Length.DEFAULT_FONT_SIZE,
    ): Double = this.corner.resolve(min(width, height), fontSize).coerceAtLeast(0.0)

    /**
     * Renders this style as the inline CSS of a `width` x `height` box.
     *
     * The border is always disabled and the focus ring is suppressed, so the computed appearance
     * is exactly the fill with the requested radius. An `em` radius resolves against [fontSize].
     */
    fun toCss(
        width: Double,
        height: Double,
        fontSize: Double = Length.DEFAULT_FONT_SIZE,
    ): String {
        val radius = cornerPixels(width, height, fontSize).css()
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
        "Style(background=${this.background}, corner=${this.corner}, foreground=${this.foreground}, " +
            "fontSize=${this.fontSize})"

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

        /**
         * The font size every component starts with: `null`, meaning *inherit*.
         *
         * A component that is asked for its font size walks up to its parent until one of them
         * writes a size; when none does, [Length.DEFAULT_FONT_SIZE] applies. This is deliberately a
         * plain `null` rather than that number, so "nobody said anything" stays distinguishable
         * from "somebody said 14".
         */
        val DEFAULT_FONT_SIZE: Length? = null
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
