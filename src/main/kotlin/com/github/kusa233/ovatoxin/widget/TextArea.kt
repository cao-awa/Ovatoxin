package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.Component
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
 * so assignment and call style can be mixed. For editable input use [TextField].
 */
class TextArea(text: String = "") : Component<TextArea>() {

    private var content: String = text
    private var fontSizeValue: Double = 14.0
    private var boldValue: Boolean = false
    private var familyValue: String = Font.getDefault().family
    private var alignValue: TextAlignment = TextAlignment.LEFT
    private var wrapValue: Boolean = false

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

    /** Font size in points. Defaults to `14`. */
    var fontSize: Double
        get() = this.fontSizeValue
        set(value) {
            this.fontSizeValue = value
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

    /** Re-applies [fontSizeValue], [boldValue] and [familyValue] to the label. */
    private fun refreshFont() {
        (this.builtNode as? Label)?.font = newFont()
    }

    /** Builds the font described by the current font properties. */
    private fun newFont(): Font = Font.font(
        this.familyValue,
        if (this.boldValue) FontWeight.BOLD else FontWeight.NORMAL,
        this.fontSizeValue,
    )

    override fun buildNode(): Node {
        val label = Label(this.content)
        label.font = newFont()
        label.textAlignment = this.alignValue
        label.isWrapText = this.wrapValue
        return label
    }
}
