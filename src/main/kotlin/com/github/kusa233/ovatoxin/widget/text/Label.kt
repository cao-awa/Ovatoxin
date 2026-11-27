package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.i18n.TextContent
import com.github.kusa233.ovatoxin.i18n.Translatable
import javafx.scene.Node
import javafx.scene.control.Label as FxLabel

/**
 * A form label: a short caption that annotates the control standing next to it.
 *
 * ```kotlin
 * frame.layout {
 *     val name = draw(TextField()) { field ->
 *         layout {
 *             x = 45.percent
 *             y = 10.percent
 *             width = 45.percent
 *             height = 6.percent
 *         }
 *         field.hint = "your name"
 *     }
 *     draw(Label("User name", labelFor = name)) { label ->
 *         layout {
 *             x = 10.percent
 *             y = 10.percent
 *             width = 30.percent
 *             height = 6.percent
 *         }
 *     }
 * }
 * ```
 *
 * A label does not lay anything out: it is a caption that describes the control it was
 * associated with, and that association is the whole point of the component. Once
 * [labelFor] points at a control, clicking the caption moves the focus to that control — the
 * usual behaviour of a desktop form — and a screen reader announces the two together, so a
 * [TextField][com.github.kusa233.ovatoxin.widget.input.TextField] next to
 * `Label("User name")` is reachable without repeating its caption in the placeholder.
 *
 * For a block of read-only text that carries the content itself — a paragraph, a translation —
 * use [TextArea] instead; a label is the short name of a neighbour, a text area is the text.
 *
 * The caption is plain text styled by the [foreground][com.github.kusa233.ovatoxin.style.Style.foreground]
 * of the style, like every other text component.
 */
class Label(text: String = "", labelFor: Component<*>? = null) : Component<Label>() {

    /** The caption this label was created with, restored by [reset]. */
    private val defaultText: String = text

    /** The control this label was created for, restored by [reset]. */
    private val defaultTarget: Component<*>? = labelFor

    private val content: TextContent = TextContent(text, this)
    private var targetValue: Component<*>? = labelFor

    /** Reads the caption of the label, resolving a translatable key. */
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

    /** The caption of the label. Read-only; call `text(...)` to replace it. */
    val text: String
        get() = this.content.value()

    private fun refreshText() {
        (this.builtNode as? FxLabel)?.text = this.content.value()
    }

    internal override fun onLanguageChanged() = refreshText()

    /** Reads the control this label annotates, or `null` while it annotates nothing. */
    fun labelFor(): Component<*>? = this.targetValue

    /**
     * Annotates [target], replacing the control this label annotated before.
     *
     * ```kotlin
     * val label = Label("User name")
     * label.labelFor(nameField)
     * ```
     *
     * Order does not matter: a control that has not been rendered yet is associated as soon as
     * both nodes exist, because the association is re-applied whenever this label builds its
     * node. Passing `null` drops the association and leaves a plain caption.
     */
    fun labelFor(target: Component<*>?) {
        this.targetValue = target
        refreshTarget()
    }

    /** The control this label annotates. */
    var labelFor: Component<*>?
        get() = this.targetValue
        set(value) = labelFor(value)

    /**
     * Writes the association into the node that was already built, if there is one.
     *
     * Without a node there is nothing to wire up yet, and the association is applied by
     * [buildNode] when the label is actually rendered — that is what keeps a label model
     * inspectable without starting the JavaFX toolkit.
     */
    private fun refreshTarget() {
        (this.builtNode as? FxLabel)?.labelFor = targetNode()
    }

    /**
     * The node of the control this label annotates, or `null` when it annotates nothing.
     *
     * Asking a control for its node builds it if necessary. The callers that reach this point
     * are [buildNode] and [labelFor], both of which only touch an already built label, so the
     * target is built on the same thread the label was built on.
     */
    private fun targetNode(): Node? {
        val target = this.targetValue ?: return null
        // A label that annotates itself would make JavaFX chase its own node forever.
        if (target === this) return null
        return target.node
    }

    /**
     * Restores the caption and the association this label was created with.
     *
     * `Label("User name", labelFor = field)` therefore resets to that pair, not to an empty
     * caption that annotates nothing.
     */
    override fun reset() {
        super.reset()
        this.content.reset(this.defaultText)
        this.targetValue = this.defaultTarget
        val label = this.builtNode as? FxLabel ?: return
        label.text = this.content.value()
        label.labelFor = targetNode()
    }

    override fun buildNode(): Node {
        val label = FxLabel(this.content.value())
        // The target may not have been rendered yet; its node is built here, on this thread.
        label.labelFor = targetNode()
        return label
    }
}
