package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * A text component.
 *
 * ```kotlin
 * draw(TextArea()) { text ->
 *     layout {
 *         x = 10.percent
 *         y = 10.percent
 *     }
 *     text.text("Test")
 * }
 * ```
 *
 * The content is exposed both as the [text] property and through the `text(..)` accessors,
 * so assignment and call style can be mixed. For editable input use [TextField][com.github.kusa233.ovatoxin.widget.input.TextField].
 */
class TextArea(text: String = "") : Component<TextArea>() {

    private val defaultText: String = text
    private val defaultFamily: String = Font.getDefault().family
    private var content: String = text
    private var boldValue: Boolean = false
    private var familyValue: String = this.defaultFamily
    private var alignValue: TextAlignment = TextAlignment.LEFT
    private var wrapValue: Boolean = false

    /** The last font handed to the label, so an unchanged pass does not build a new one. */
    private var appliedFont: Font? = null

    /** Reads the displayed text. */
    fun text(): String = this.content

    /** Replaces the displayed text. */
    fun text(value: String) {
        this.content = value
        (this.builtNode as? Label)?.text = value
    }

    /** The displayed text. */
    var text: String
        get() = this.content
        set(value) = text(value)

    /**
     * The size the text is rendered with, in points. Defaults to [DEFAULT_FONT_SIZE], i.e. `14`.
     *
     * The value is stored in the [style][com.github.kusa233.ovatoxin.style.Style] of this area, so
     * the two ways of writing it mean the same thing — this property always speaks in points, and
     * the block accepts a [Length][com.github.kusa233.ovatoxin.layout.Length] with a unit:
     *
     * ```kotlin
     * draw(TextArea("Test")) { text ->
     *     text.fontSize = 20.0            // 20 points
     *     style { fontSize = 20.px }      // the same 20 points
     *     style { fontSize = 50.percent } // half of the height this area is drawn with
     * }
     * ```
     *
     * An `em` is refused here as everywhere: it is a multiple of the font size and cannot also be
     * the font size.
     *
     * Unlike other components a text area does not inherit the size of the surface it is drawn
     * into: it renders text of its own, and a text area without a size of its own is measured with
     * the [default][DEFAULT_FONT_SIZE] rather than with an enclosing panel. A percentage is taken
     * of the height of the area itself, once it is laid out, so `50.percent` is half a tall text
     * area.
     */
    var fontSize: Double
        get() = when (val spec = this.styleSpec.fontSize) {
            null -> DEFAULT_FONT_SIZE
            // A pixel size is read straight back, so a programmatic write is visible at once.
            is com.github.kusa233.ovatoxin.layout.Pixel -> spec.value
            else -> this.resolvedFontSize
        }
        set(value) {
            this.styleSpec.fontSize = value.px
            refreshFont()
        }

    /** Whether the text is drawn in bold. Defaults to `false`. */
    var bold: Boolean
        get() = this.boldValue
        set(value) {
            this.boldValue = value
            refreshFont()
        }

    /** The font family used to render the text. */
    var fontFamily: String
        get() = this.familyValue
        set(value) {
            this.familyValue = value
            refreshFont()
        }

    /** Text alignment inside the component box. */
    var align: TextAlignment
        get() = this.alignValue
        set(value) {
            this.alignValue = value
            (this.builtNode as? Label)?.textAlignment = value
        }

    /** Whether long lines wrap inside the component box. Defaults to `false`. */
    var wrap: Boolean
        get() = this.wrapValue
        set(value) {
            this.wrapValue = value
            (this.builtNode as? Label)?.isWrapText = value
        }

    /** The size the layout resolves an `em` against: the size this area is drawn with. */
    internal override fun fontSizeForHeight(height: Double?, inherited: Double): Double =
        // A text area renders text of its own, so it never inherits: without a size of its own it
        // falls back to the default, but a percentage is still taken of its own box height.
        this.styleSpec.fontSize?.resolveFontSize(height) ?: DEFAULT_FONT_SIZE

    /** A text area assigns its font itself, so the base class must keep the CSS font off the label. */
    override val appliesOwnFont: Boolean get() = true

    /**
     * Re-applies [fontSize], [boldValue] and [familyValue] to the label.
     *
     * The font handed over is remembered in [appliedFont], so the layout pass that follows does not
     * build and assign the same font a second time.
     */
    private fun refreshFont() {
        val font = newFont()
        this.appliedFont = font
        (this.builtNode as? Label)?.font = font
    }

    /** Builds the font described by the current font properties and the style's weight. */
    private fun newFont(): Font = Font.font(
        this.familyValue,
        this.styleSpec.fontWeight ?: if (this.boldValue) FontWeight.BOLD else FontWeight.NORMAL,
        this.fontSize,
    )

    /**
     * Restores the text this area was created with, and the default font, alignment and wrapping.
     *
     * `TextArea("Test")` therefore resets to `"Test"`, not to an empty caption. The font size goes
     * back to [DEFAULT_FONT_SIZE] with the rest of the style, so a redraw starts at the same `14`
     * points it started with.
     */
    override fun reset() {
        super.reset()
        this.content = this.defaultText
        this.boldValue = false
        this.familyValue = this.defaultFamily
        this.alignValue = TextAlignment.LEFT
        this.wrapValue = false
        val label = this.builtNode as? Label ?: return
        label.text = this.content
        refreshFont()
        label.textAlignment = this.alignValue
        label.isWrapText = this.wrapValue
    }

    override fun buildNode(): Node {
        val label = Label(this.content)
        // The node is not stored yet, so the font is handed over here rather than by refreshFont.
        val font = newFont()
        this.appliedFont = font
        label.font = font
        label.textAlignment = this.alignValue
        label.isWrapText = this.wrapValue
        return label
    }

    /**
     * Applies the style, and then the font, since [fontSize] is now written in a style block too.
     *
     * ```kotlin
     * draw(TextArea("Test")) { text -> style { fontSize = 24.0 } }
     * ```
     *
     * The property setter refreshes the font on its own; this covers the same value arriving through
     * [com.github.kusa233.ovatoxin.style.Style] on a layout pass. A pass that describes the font the
     * label already has leaves it alone, so laying the tree out again stays cheap.
     */
    internal override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        if (newFont() == this.appliedFont) return
        refreshFont()
    }

    companion object {
        /**
         * The font size a text area is drawn with until it is changed: `14` points.
         *
         * The same value the library resolves an `em` against by default, since a text area is the
         * component that gives a tree its font size in the first place.
         */
        const val DEFAULT_FONT_SIZE: Double = Length.DEFAULT_FONT_SIZE
    }
}
