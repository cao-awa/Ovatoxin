package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.Component
import javafx.scene.Node
import javafx.scene.control.CheckBox as FxCheckBox

/**
 * A box with a caption that is either ticked or not.
 *
 * ```kotlin
 * draw(CheckBox("Remember me")) { box ->
 *     layout {
 *         x = 10.percent
 *         y = 70.percent
 *         width = 80.percent
 *         height = 8.percent
 *     }
 *     box.checked = true
 *     box.onChange { checked -> println("checked: $checked") }
 * }
 * ```
 *
 * The caption sits to the right of the box, as it does in every desktop toolkit; use [text] to
 * change it and [checked] to read or set the tick.
 *
 * [onChange] is the handler meant for a checkbox: it receives the new state as a plain `Boolean`, so
 * the usual case — mirroring the tick into a
 * [State][com.github.kusa233.ovatoxin.state.State] — needs no `this` at all. The generic [onClick]
 * still works and runs on the same click, in addition to [onChange], when both are registered.
 *
 * A tick the caller writes is silent: `box.checked = true` changes what is on screen and runs no
 * handler, exactly like a programmatic selection in a [ListView]. Only a click on the box reaches
 * the handlers, so a handler that also writes [checked] cannot call itself in a loop.
 */
class CheckBox(text: String = "") : Component<CheckBox>() {

    /** The caption this box was created with, restored by [reset]. */
    private val defaultText: String = text

    /** The tick this box was created with, restored by [reset]. */
    private val defaultChecked: Boolean = false

    private var content: String = text
    private var checkedValue: Boolean = this.defaultChecked
    private var enabledValue: Boolean = true
    private var changeHandler: ((Boolean) -> Unit)? = null

    /** Reads the caption of the box. */
    fun text(): String = this.content

    /** Replaces the caption of the box. */
    fun text(value: String) {
        this.content = value
        (this.builtNode as? FxCheckBox)?.text = value
    }

    /** The caption of the box. */
    var text: String
        get() = this.content
        set(value) = text(value)

    /** Whether the box is ticked. Defaults to `false`. */
    var checked: Boolean
        get() = this.checkedValue
        set(value) {
            this.checkedValue = value
            (this.builtNode as? FxCheckBox)?.isSelected = value
        }

    /** Whether the box can be clicked. Defaults to `true`. */
    var enabled: Boolean
        get() = this.enabledValue
        set(value) {
            this.enabledValue = value
            (this.builtNode as? FxCheckBox)?.isDisable = !value
        }

    /**
     * Registers the handler that runs when the user ticks or unticks the box.
     *
     * The handler receives the state the box now has, so it does not have to read [checked] back —
     * though it may, since the field is updated before the handler runs.
     *
     * ```kotlin
     * box.onChange { checked -> state.value = checked }
     * ```
     *
     * Registering again replaces the previous handler; [reset] removes it.
     */
    fun onChange(handler: (Boolean) -> Unit): CheckBox {
        this.changeHandler = handler
        return this
    }

    // Clicking a checkbox fires an action on its own node, as a button does, so the click must not
    // also be wired up by the base class — the two would report the same press twice.
    override val reportsClicksAsAction: Boolean get() = true

    /**
     * Reports a click on the box: syncs the field with the node, runs [onChange], then [onClick].
     *
     * The node has already flipped its own state by the time the action fires, so the field is read
     * back from it rather than guessed. Without a node — a click in a test that never showed the
     * window, or on a component that has not been rendered yet — the field is flipped instead,
     * because that is what a click does to a checkbox. A disabled box reports nothing.
     */
    override fun performClick() {
        if (!this.enabledValue) return
        val node = this.builtNode as? FxCheckBox
        this.checkedValue = node?.isSelected ?: !this.checkedValue
        this.changeHandler?.invoke(this.checkedValue)
        super.performClick()
    }

    /**
     * Restores the caption and tick this box was created with, and enables it again.
     *
     * `CheckBox("Keep open")` therefore resets to an unticked `"Keep open"`, not to an empty caption.
     * The tick is assigned directly, so the reset is silent: no [onChange] runs for it, and the
     * handler is dropped anyway.
     */
    override fun reset() {
        super.reset()
        this.content = this.defaultText
        this.checkedValue = this.defaultChecked
        this.enabledValue = true
        this.changeHandler = null
        val node = this.builtNode as? FxCheckBox
        node?.text = this.defaultText
        node?.isSelected = this.defaultChecked
        node?.isDisable = false
    }

    override fun buildNode(): Node {
        val box = FxCheckBox(this.content)
        box.isSelected = this.checkedValue
        box.isDisable = !this.enabledValue
        box.setOnAction { performClick() }
        box.styleClass += STYLE_CLASS
        CheckBox::class.java.getResource(STYLESHEET)?.let { box.stylesheets += it.toExternalForm() }
        return box
    }

    companion object {
        /** Style class every OvatoXin checkbox carries. */
        const val STYLE_CLASS: String = "ovatoxin-checkbox"

        /** Stylesheet that flattens the gradients Modena paints into the box and the tick. */
        private const val STYLESHEET: String = "checkbox.css"
    }
}
