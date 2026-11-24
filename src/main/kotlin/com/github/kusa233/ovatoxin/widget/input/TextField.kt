package com.github.kusa233.ovatoxin.widget.input

import com.github.kusa233.ovatoxin.component.Component
import javafx.scene.Node
import javafx.scene.control.TextField as FxTextField

/**
 * An editable single line text box.
 *
 * ```kotlin
 * draw(TextField()) { input ->
 *     layout {
 *         x = 20.percent
 *         y = 20.percent
 *         width = 60.percent
 *         height = 8.percent
 *     }
 *     input.hint = "type here"
 *     input.onChange { println(it) }
 * }
 * ```
 */
class TextField(text: String = "") : Component<TextField>() {

    private val defaultText: String = text
    private var content: String = text
    private var hintValue: String = ""
    private var editableValue: Boolean = true
    private var changeHandler: ((String) -> Unit)? = null
    private var submitHandler: ((String) -> Unit)? = null

    /** Reads the current content of the text box. */
    fun text(): String = this.content

    /** Replaces the content of the text box. */
    fun text(value: String) {
        this.content = value
        (this.builtNode as? FxTextField)?.text = value
        this.changeHandler?.invoke(value)
    }

    /** The current content of the text box. */
    var text: String
        get() = this.content
        set(value) = text(value)

    /** The placeholder shown while the box is empty. */
    var hint: String
        get() = this.hintValue
        set(value) {
            this.hintValue = value
            (this.builtNode as? FxTextField)?.promptText = value
        }

    /** Whether the content can be edited. Defaults to `true`. */
    var editable: Boolean
        get() = this.editableValue
        set(value) {
            this.editableValue = value
            (this.builtNode as? FxTextField)?.isEditable = value
        }

    /** Registers a handler invoked on every content change. */
    fun onChange(handler: (String) -> Unit): TextField {
        this.changeHandler = handler
        return this
    }

    /** Registers a handler invoked when the user presses `Enter`. */
    fun onSubmit(handler: (String) -> Unit): TextField {
        this.submitHandler = handler
        return this
    }

    /** Simulates the user submitting the content, mainly useful for tests. */
    fun performSubmit() {
        this.submitHandler?.invoke(this.content)
    }

    /**
     * Restores the content this box was created with, and clears the hint, the [editable] flag
     * and both handlers.
     *
     * The content is assigned directly instead of through [text], so a reset never reports a
     * change to the previous `onChange` handler.
     */
    override fun reset() {
        super.reset()
        this.content = this.defaultText
        this.hintValue = ""
        this.editableValue = true
        this.changeHandler = null
        this.submitHandler = null
        val field = this.builtNode as? FxTextField ?: return
        // The change handler is already gone, so assigning the text reports nothing.
        field.text = this.content
        field.promptText = ""
        field.isEditable = true
    }

    override fun buildNode(): Node {
        val field = FxTextField(this.content)
        field.promptText = this.hintValue
        field.isEditable = this.editableValue
        field.textProperty().addListener { _, _, value ->
            this.content = value ?: ""
            this.changeHandler?.invoke(this.content)
        }
        field.setOnAction { this.submitHandler?.invoke(this.content) }
        return field
    }
}
