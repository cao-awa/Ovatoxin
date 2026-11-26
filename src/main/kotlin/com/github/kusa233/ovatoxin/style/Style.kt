package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Em
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * The visual appearance of a component.
 *
 * A component is drawn as a filled box, optionally with rounded corners and an outline. The fill
 * and the outline are configured in their own blocks, [background] and [border].
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
 *         border {
 *             size = 2.px
 *             color = Color.WHITE
 *         }
 *         corner = 50.percent // A pill: half of the shorter side
 *     }
 * }
 * ```
 *
 * A percentage [corner] resolves against the *shorter* side of the box, so `50.percent` is
 * always the roundest possible corner — a pill for a wide box, a circle for a square one — no
 * matter how the window is resized. A percentage border [Border.size] resolves against the same
 * shorter side.
 *
 * A `style { }` block is a marked DSL scope, and so are the [background][background] and
 * [border][border] blocks inside it: a background `color` cannot be written directly in the style
 * block, and an `x`, which belongs to the layout, cannot be written here at all.
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
     * Defaults to the plain fill of [DEFAULT_COLOR]. The fill defines the shape of the component;
     * an optional outline is configured separately in [border].
     */
    val background: Background = Background()

    /**
     * The outline of the component, configured in a [Border] block.
     *
     * ```kotlin
     * style {
     *     border {
     *         size = 1.px
     *         color = Color.BLACK
     *     }
     * }
     * ```
     *
     * Defaults to no border. The outline follows [corner], so it stays attached to a rounded fill.
     */
    val border: Border = Border()

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

    /**
     * The weight of the text drawn by the component, or `null` to keep the weight the component
     * brings along. Defaults to `null`.
     *
     * ```kotlin
     * import javafx.scene.text.FontWeight
     *
     * style {
     *     fontWeight = FontWeight.BOLD
     * }
     * ```
     *
     * A component with no text of its own — a [Div][com.github.kusa233.ovatoxin.widget.container.Div]
     * or a [Frame][com.github.kusa233.ovatoxin.window.Frame] — simply ignores it, like
     * [foreground]. Text that the component paints itself, such as a
     * [TextArea][com.github.kusa233.ovatoxin.widget.text.TextArea], uses the same value when it
     * builds its font.
     */
    var fontWeight: FontWeight? = DEFAULT_FONT_WEIGHT

    /**
     * How long a change of this style is animated, or `null` for no animation.
     *
     * The unit is Kotlin's [Duration], so a duration is written the way Kotlin reads it:
     *
     * ```kotlin
     * import kotlin.time.Duration.Companion.milliseconds
     *
     * style {
     *     background { color = Color.web("#4c8bf5") }
     *     transition = 200.milliseconds
     * }
     * ```
     *
     * The first style a component receives is applied immediately; from then on a change of the
     * [background][Background], the [foreground], the [corner] or the [border][Border] is blended
     * from the old value to the new one over the given [Duration]. That is what turns a
     * state-driven colour switch into a fade:
     *
     * ```kotlin
     * draw(Button()) { button ->
     *     style {
     *         background { color = if (selected.value) Color.WHITE else Color.web("#4c8bf5") }
     *         transition = 200.milliseconds
     *     }
     *     button.onClick { selected.update(!selected.value) }
     * }.withState(selected)
     * ```
     *
     * `Duration.ZERO` — or leaving this `null`, the default — applies changes at once. A negative
     * or infinite duration is refused. Because the value lives in the style, a redraw drops it
     * together with the rest of the style, so a block that wants the animation re-states it each
     * time it runs.
     */
    var transition: Duration? = null
        set(value) {
            if (value != null) {
                require(value.isFinite()) {
                    "a transition duration must be finite, was $value"
                }
                require(value >= Duration.ZERO) {
                    "a transition duration cannot be negative, was $value"
                }
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
     * Configures the [border] in place:
     *
     * ```kotlin
     * style {
     *     border {
     *         size = 1.px
     *         color = Color.BLACK
     *     }
     * }
     * ```
     *
     * The block mutates the existing [Border] rather than replacing it, so anything set before the
     * block — an earlier `border { … }`, for instance — is kept unless the block overrides that
     * same property.
     */
    fun border(block: Border.() -> Unit): Style {
        this.border.configure(block)
        return this
    }

    /**
     * Restores every property to the documented default: a plain white fill, no border, square
     * corners, no text colour, no font weight, no animation, and a font size inherited from the
     * parent component.
     */
    internal fun reset() {
        this.background.reset()
        this.border.reset()
        this.corner = DEFAULT_CORNER
        this.foreground = DEFAULT_FOREGROUND
        this.fontSize = DEFAULT_FONT_SIZE
        this.fontWeight = DEFAULT_FONT_WEIGHT
        this.transition = null
    }

    /** Copies every property of [other] into this style. */
    fun copyFrom(other: Style) {
        this.background.copyFrom(other.background)
        this.border.copyFrom(other.border)
        this.corner = other.corner
        this.foreground = other.foreground
        this.fontSize = other.fontSize
        this.fontWeight = other.fontWeight
        this.transition = other.transition
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
     * Resolves [Border.size] against the shorter side of a `width` x `height` box, in pixels.
     *
     * An `em` size resolves against [fontSize], the font size of the component the style belongs
     * to. A negative size — and therefore a border that would be inside out — is treated as zero.
     */
    fun borderPixels(
        width: Double,
        height: Double,
        fontSize: Double = Length.DEFAULT_FONT_SIZE,
    ): Double = this.border.size.resolve(min(width, height), fontSize).coerceAtLeast(0.0)

    /**
     * Renders this style as the inline CSS of a `width` x `height` box.
     *
     * The focus ring is suppressed, and the border is either the configured outline or disabled
     * when [Border.size] is zero, so the computed appearance is exactly the fill and optional
     * outline with the requested radius. An `em` radius or border size resolves against [fontSize].
     */
    fun toCss(
        width: Double,
        height: Double,
        fontSize: Double = Length.DEFAULT_FONT_SIZE,
    ): String = renderCss(this.background.image, resolveVisual(width, height, fontSize))

    /**
     * Freezes this style into the concrete values of a `width` x `height` box.
     *
     * Unlike [toCss], which produces text, this keeps the parts of the appearance that can be
     * animated — the fill with its opacity folded in, the corner radius in pixels, the text
     * colour, and the border size and colour — as values. It is the starting point of a transition:
     * two snapshots of the same component are blended by [StyleVisual.lerp] and rendered again with
     * [renderCss].
     *
     * The fill is `null` either when an image paints the box or when no colour is set, which is
     * exactly when [toCss] would write `transparent`.
     */
    internal fun resolveVisual(
        width: Double,
        height: Double,
        fontSize: Double = Length.DEFAULT_FONT_SIZE,
    ): StyleVisual {
        val backgroundColor = this.background.color
        val effectiveFill = if (this.background.image != null || backgroundColor == null) {
            null
        } else {
            val alpha = backgroundColor.opacity * this.background.opacity /
                Background.MAX_OPACITY.toDouble()
            Color(backgroundColor.red, backgroundColor.green, backgroundColor.blue, alpha)
        }
        val borderSize = borderPixels(width, height, fontSize)
        return StyleVisual(
            background = effectiveFill,
            corner = cornerPixels(width, height, fontSize),
            foreground = this.foreground,
            borderSize = borderSize,
            borderColor = if (borderSize > 0.0) this.border.color else null,
        )
    }

    /**
     * Renders a resolved [visual] as inline CSS, exactly as [toCss] would for the same values.
     *
     * [image] is the background image the style currently names — an image is not interpolated, it
     * simply stays on the node while the colour layers around it animate.
     */
    internal fun renderCss(image: String?, visual: StyleVisual): String {
        val radius = visual.corner.css()
        val fill = visual.background?.let { cssColor(it) } ?: "transparent"
        val textFill = visual.foreground
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
            val borderColor = visual.borderColor
            if (visual.borderSize > 0.0 && borderColor != null) {
                append("-fx-border-width: ").append(visual.borderSize.css()).append("px; ")
                append("-fx-border-color: ").append(cssColor(borderColor)).append("; ")
                append("-fx-border-style: solid; ")
                append("-fx-border-radius: ").append(radius).append("px; ")
            } else {
                append("-fx-border-width: 0; ")
            }
            append("-fx-focus-color: transparent; ")
            append("-fx-faint-focus-color: transparent;")
        }
    }

    override fun toString(): String =
        "Style(background=${this.background}, border=${this.border}, corner=${this.corner}, " +
            "foreground=${this.foreground}, fontSize=${this.fontSize}, " +
            "fontWeight=${this.fontWeight ?: "none"}, transition=${this.transition ?: "none"})"

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

        /**
         * The font weight every component starts with: `null`, meaning *keep the component's own
         * weight*. This keeps plain controls looking native until a style explicitly asks for a
         * weight.
         */
        val DEFAULT_FONT_WEIGHT: FontWeight? = null
    }
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
