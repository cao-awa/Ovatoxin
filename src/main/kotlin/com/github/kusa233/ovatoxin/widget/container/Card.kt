package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.SurfaceScope
import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

/**
 * A rounded panel that groups related content, with a small DSL of its own for text and buttons.
 *
 * A [Card] is a [FlexBox] that starts with the look a card needs: a white fill, a rounded outline,
 * symmetric padding and a vertical gap between its children. It is a real drawing surface, so the
 * usual `layout { ... }` block still decides its position and size, and percentages inside it
 * resolve against the card rather than against the page. On top of that it offers [text] and
 * [button], which draw the two kinds of content a card almost always holds:
 *
 * ```kotlin
 * frame.layout {
 *     draw(Card(h = 180.px)) { card ->
 *         card.text("今日概览", h = 28.px, size = 19.0, bold = true)
 *         card.text("这段说明会自动换行。", h = 36.px, size = 13.0, color = Color.web("#6b7280"), wrap = true)
 *         card.button("开始探索") { println("clicked") }
 *     }
 * }
 * ```
 *
 * The card body can also be declared the ordinary way, through [layout][FlexBox.layout], when a
 * row, a divider or any other component is needed between the text and the buttons:
 *
 * ```kotlin
 * val card = card(h = 220.px, gap = 10.0) {
 *     text("标题", h = 26.px, size = 18.0, bold = true)
 *     draw(Div()) { layout { width = 100.percent; height = 1.px } }
 *     button("保存")
 * }
 * ```
 *
 * [text] and [button] are deliberately instance members rather than free functions: writing
 * `card.text(...)` inside the `draw(Card(...))` block makes it clear which card receives the child.
 * The `card(...)` factory below is only a shorthand for the common case where the body is written
 * as a block; it returns the same [Card], so the two styles can be mixed.
 *
 * Every value passed to the constructor survives a [reset], exactly like the caption of a
 * [Button][com.github.kusa233.ovatoxin.widget.action.Button]: a card drawn again after a redraw
 * starts from its own fill, padding and gap instead of the library defaults.
 *
 * @param height the height of the card; a card is normally given an explicit height.
 * @param width the width of the card, `100.percent` (the full content width) by default.
 * @param fill the background colour; white by default.
 * @param borderColor the colour of the one-pixel outline, or `null` for no outline.
 * @param pad the inset kept on all four sides, `18.0` pixels by default.
 * @param gap the space between two children, `8.0` pixels by default.
 * @param corner the corner radius, `16.px` by default.
 */
@OvatoxinDsl
class Card(
    height: Length,
    width: Length = 100.percent,
    fill: Color = DEFAULT_FILL,
    borderColor: Color? = DEFAULT_BORDER,
    pad: Double = DEFAULT_PADDING,
    gap: Double = DEFAULT_GAP,
    corner: Length = DEFAULT_CORNER,
) : FlexBox() {

    private val cardHeight: Length = height
    private val cardWidth: Length = width
    private val cardFill: Color = fill
    private val cardBorder: Color? = borderColor
    private val cardPadding: Double = pad
    private val cardGap: Double = gap
    private val cardCorner: Length = corner

    /**
     * Draws a [TextArea] into this card.
     *
     * The parameters mirror the small text helper used throughout the samples: the width defaults
     * to the full content width, the height is explicit (a flow cannot infer how tall a line of
     * prose should be), and [wrap] turns long text into several lines.
     *
     * @return the drawn text area, so it can be bound to a [state][com.github.kusa233.ovatoxin.state.State].
     */
    fun text(
        content: String,
        w: Length? = 100.percent,
        h: Length = 20.px,
        size: Double = 14.0,
        color: Color = DEFAULT_TEXT_COLOR,
        bold: Boolean = false,
        align: TextAlignment = TextAlignment.LEFT,
        wrap: Boolean = false,
    ): TextArea = surface().drawText(
        content = content,
        width = w,
        height = h,
        size = size,
        tone = color,
        bold = bold,
        align = align,
        wrap = wrap,
    )

    /**
     * Draws a [Button] into this card.
     *
     * The default is the primary action of a card: a blue fill with white text. Pass [fill],
     * [fg] and [borderColor] for a secondary or outline button.
     *
     * @return the drawn button, so its [enabled][Button.enabled] flag can be changed later.
     */
    fun button(
        label: String,
        w: Length = 120.px,
        h: Length = 40.px,
        fill: Color = DEFAULT_BUTTON_FILL,
        fg: Color = DEFAULT_BUTTON_TEXT,
        borderColor: Color? = null,
        radius: Length = 10.px,
        onClick: Button.() -> Unit = {},
    ): Button = surface().drawButton(
        label = label,
        width = w,
        height = h,
        fill = fill,
        foreground = fg,
        borderColor = borderColor,
        radius = radius,
        onClick = onClick,
    )

    /** The scope used by [text] and [button] to attach one child at a time. */
    private fun surface(): SurfaceScope<FlexBox> = SurfaceScope(this)

    /**
     * Restores the look this card was created with.
     *
     * A redraw resets a component before replaying its block, so the card has to put its own fill,
     * corner, padding and flow back; otherwise the first redraw would turn it into a plain white
     * [FlexBox]. Direct field writes are used here instead of [applyStyle] so a reset does not
     * trigger a relayout of its own — the caller is already laying the card out.
     */
    override fun reset() {
        super.reset()
        this.layoutSpec.width = this.cardWidth
        this.layoutSpec.height = this.cardHeight
        this.layoutSpec.hPadding = this.cardPadding.px
        this.layoutSpec.vPadding = this.cardPadding.px
        this.styleSpec.background { color = this@Card.cardFill }
        this.styleSpec.corner = this.cardCorner
        this.styleSpec.fontSize = 14.px
        if (this.cardBorder != null) {
            this.styleSpec.border {
                size = 1.px
                color = this@Card.cardBorder
            }
        }
        this.gap = this.cardGap.px
        this.align = Align.START
    }

    companion object {
        /** The default card fill: plain white. */
        val DEFAULT_FILL: Color = Color.WHITE

        /** The default card outline. */
        val DEFAULT_BORDER: Color = Color.web("#e4e9f2")

        /** The default padding, in pixels. */
        const val DEFAULT_PADDING: Double = 18.0

        /** The default gap between two children, in pixels. */
        const val DEFAULT_GAP: Double = 8.0

        /** The default corner radius. */
        val DEFAULT_CORNER: Length = 16.px

        /** The default colour of text drawn by [Card.text]. */
        val DEFAULT_TEXT_COLOR: Color = Color.web("#162033")

        /** The default fill of a button drawn by [Card.button]. */
        val DEFAULT_BUTTON_FILL: Color = Color.web("#4c8bf5")

        /** The default text colour of a button drawn by [Card.button]. */
        val DEFAULT_BUTTON_TEXT: Color = Color.WHITE
    }
}

/**
 * Draws a [Card] and declares its body in one call.
 *
 * This is a convenience over `draw(Card(...)) { card -> card.layout { body() } }`; the body runs
 * on the card's own [SurfaceScope], so `draw(...)`, `text(...)` and `button(...)` can be written
 * directly inside it, as in every other surface. The returned value is a real [Card], so it can be
 * clicked, bound to a state or inspected afterwards.
 *
 * ```kotlin
 * frame.layout {
 *     card(h = 160.px) {
 *         text("标题", h = 24.px, size = 17.0, bold = true)
 *         button("详情")
 *     }
 * }
 * ```
 */
fun SurfaceScope<*>.card(
    height: Length,
    width: Length = 100.percent,
    fill: Color = Card.DEFAULT_FILL,
    borderColor: Color? = Card.DEFAULT_BORDER,
    pad: Double = Card.DEFAULT_PADDING,
    gap: Double = Card.DEFAULT_GAP,
    corner: Length = Card.DEFAULT_CORNER,
    body: SurfaceScope<FlexBox>.() -> Unit,
): Card = draw(Card(height, width, fill, borderColor, pad, gap, corner)) { card ->
    card.layout { body() }
}

/** The public spelling of the body DSL: `text(...)` inside a [card] block. */
fun SurfaceScope<*>.text(
    content: String,
    width: Length? = 100.percent,
    height: Length = 20.px,
    size: Double = 14.0,
    color: Color = Card.DEFAULT_TEXT_COLOR,
    bold: Boolean = false,
    align: TextAlignment = TextAlignment.LEFT,
    wrap: Boolean = false,
): TextArea = drawText(content, width, height, size, color, bold, align, wrap)

/** The public spelling of the body DSL: `button(...)` inside a [card] block. */
fun SurfaceScope<*>.button(
    label: String,
    width: Length = 120.px,
    height: Length = 40.px,
    fill: Color = Card.DEFAULT_BUTTON_FILL,
    foreground: Color = Card.DEFAULT_BUTTON_TEXT,
    borderColor: Color? = null,
    radius: Length = 10.px,
    onClick: Button.() -> Unit = {},
): Button = drawButton(label, width, height, fill, foreground, borderColor, radius, onClick)

/** Draws a transparent text area and applies the card-text defaults. */
private fun SurfaceScope<*>.drawText(
    content: String,
    width: Length?,
    height: Length,
    size: Double,
    tone: Color,
    bold: Boolean,
    align: TextAlignment,
    wrap: Boolean,
): TextArea = draw(TextArea(content)) { area ->
    layout {
        this.width = width
        this.height = height
    }
    style {
        background {
            color = Color.TRANSPARENT
        }
        fontSize = size.px
        foreground = tone
    }
    area.bold = bold
    area.align = align
    area.wrap = wrap
}

/** Draws a flat button and applies the card-button defaults. */
private fun SurfaceScope<*>.drawButton(
    label: String,
    width: Length,
    height: Length,
    fill: Color,
    foreground: Color,
    borderColor: Color?,
    radius: Length,
    onClick: Button.() -> Unit,
): Button = draw(Button(label)) { button ->
    layout {
        this.width = width
        this.height = height
    }
    style {
        background {
            color = fill
        }
        this.foreground = foreground
        corner = radius
        fontSize = 14.px
        if (borderColor != null) {
            border {
                size = 1.px
                color = borderColor
            }
        }
    }
    button.onClick(onClick)
}
