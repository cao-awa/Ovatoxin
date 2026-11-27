package com.github.kusa233.ovatoxin.widget.action

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.i18n.TextContent
import com.github.kusa233.ovatoxin.i18n.Translatable
import javafx.scene.Node
import javafx.scene.control.Button as FxButton

/**
 * A push button.
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     layout {
 *         x = 10.percent
 *         y = 10.percent
 *         width = 80.percent
 *         height = 10.percent
 *     }
 *     button.text("Button")
 *     button.onClick {
 *         println(text1.text())
 *     }
 * }
 * ```
 *
 * [onClick] runs the handler with the button as receiver, so inside the block `this` is the
 * button and `this.text()` reads its caption. A disabled button reports no clicks at all.
 */
class Button(text: String = "") : Component<Button>() {

    private val defaultText: String = text
    private val content: TextContent = TextContent(text, this)
    private var enabledValue: Boolean = true

    /** Reads the caption of the button, resolving a translatable key. */
    fun text(): String = this.content.value()

    /** Shows a literal caption. */
    fun text(value: String) {
        this.content.literal(value)
        refreshText()
    }

    /** Shows the content of a translation key, re-resolved whenever the language changes. */
    fun text(value: Translatable) {
        this.content.translation(value)
        refreshText()
    }

    /** The caption of the button. Read-only; call `text(...)` to replace it. */
    val text: String
        get() = this.content.value()

    private fun refreshText() {
        (this.builtNode as? FxButton)?.text = this.content.value()
    }

    internal override fun onLanguageChanged() = refreshText()

    /** Whether the button can be pressed. Defaults to `true`. */
    var enabled: Boolean
        get() = this.enabledValue
        set(value) {
            this.enabledValue = value
            (this.builtNode as? FxButton)?.isDisable = !value
        }

    // The JavaFX skin turns a press into an action on its own, so no extra mouse wiring is needed.
    override val reportsClicksAsAction: Boolean get() = true

    /** Runs the click handler as if the user had pressed the button; disabled buttons stay mute. */
    override fun performClick() {
        if (this.enabledValue) super.performClick()
    }

    /**
     * Restores the caption this button was created with and enables it again.
     *
     * `Button("Ok")` therefore resets to `"Ok"`, not to an empty caption.
     */
    override fun reset() {
        super.reset()
        this.content.reset(this.defaultText)
        (this.builtNode as? FxButton)?.text = this.content.value()
        this.enabledValue = true
        (this.builtNode as? FxButton)?.isDisable = false
    }

    override fun buildNode(): Node {
        val button = FxButton(this.content.value())
        button.isDisable = !this.enabledValue
        button.setOnAction { performClick() }
        // Marked so the flat look can also be tuned from a stylesheet if needed. The skinned
        // border, gradient and focus ring are switched off by the style layer's inline CSS.
        button.styleClass += OVATOXIN_STYLE_CLASS
        return button
    }

    companion object {
        /** Style class every OvatoXin button carries. */
        const val OVATOXIN_STYLE_CLASS: String = "ovatoxin-button"
    }
}
