package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.runtime.Browser
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.paint.Color

/**
 * A text link that opens a URL in the default browser.
 *
 * ```kotlin
 * draw(Link("Ovatoxin", "https://github.com/kusa233/Ovatoxin")) { link ->
 *     layout {
 *         x = 10.percent
 *         y = 80.percent
 *     }
 * }
 * ```
 *
 * Clicking opens [url] in the browser of the operating system — that is the behaviour of a link
 * without a handler. Giving it an [onClick][Component.onClick] handler *replaces* that default:
 *
 * ```kotlin
 * draw(Link("Copy", "")) { link ->
 *     link.onClick { println("no browser, custom logic instead") }
 * }
 * ```
 *
 * A replaced link does not open anything, it only runs the handler. The handler receives the
 * link as `this`, so `this.url` is available inside it.
 *
 * The caption is plain text styled by the [foreground][com.github.kusa233.ovatoxin.style.Style.foreground]
 * of the style, which defaults to [DEFAULT_FOREGROUND] for a link.
 */
class Link(text: String = "", url: String = "") : Component<Link>() {

    private val defaultText: String = text
    private val defaultUrl: String = url
    private var content: String = text

    /** The URL opened by a click, or an empty string for a link that does nothing by default. */
    var url: String = url

    init {
        // A link reads as a link even when nobody styles it.
        this.styleSpec.foreground = DEFAULT_FOREGROUND
    }

    /** Reads the caption of the link. */
    fun text(): String = this.content

    /** Replaces the caption of the link. */
    fun text(value: String) {
        this.content = value
        (this.builtNode as? Label)?.text = value
    }

    /** The caption of the link. */
    var text: String
        get() = this.content
        set(value) = text(value)

    /** Opens [url] in the default browser. Does nothing when [url] is empty. */
    fun open() {
        if (this.url.isNotEmpty()) Browser.open(this.url)
    }

    /**
     * A click opens the browser, unless [onClick][Component.onClick] replaced that default with a
     * handler of its own.
     */
    override fun performClick() {
        if (this.hasOnClick) {
            super.performClick()
        } else {
            open()
        }
    }

    /**
     * Restores the caption and URL this link was created with, and reseeds its default colour.
     *
     * The colour set in `init` is a style seed, not a style change, so a reset brings it back:
     * after a redraw the link still reads as a link.
     */
    override fun reset() {
        super.reset()
        this.content = this.defaultText
        this.url = this.defaultUrl
        this.styleSpec.foreground = DEFAULT_FOREGROUND
        (this.builtNode as? Label)?.text = this.defaultText
    }

    override fun buildNode(): Node {
        val label = Label(this.content)
        // The classic link look: underlined, with a hand cursor on hover.
        label.isUnderline = true
        label.cursor = Cursor.HAND
        return label
    }

    companion object {
        /** The colour a link uses until a style overrides it. */
        val DEFAULT_FOREGROUND: Color = Color.web("#4c8bf5")
    }
}
