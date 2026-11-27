package com.github.kusa233.ovatoxin.i18n

import com.github.kusa233.ovatoxin.component.Component

/** The literal or translatable value a text widget is currently showing. */
internal sealed class TextSource {

    abstract fun resolve(): String

    data class Literal(val value: String) : TextSource() {
        override fun resolve(): String = this.value
    }

    data class Translation(val translatable: Translatable) : TextSource() {
        override fun resolve(): String = this.translatable.content()
    }
}

/**
 * The text state shared by text-bearing widgets.
 *
 * It remembers whether the widget was given a literal or a [Translatable], resolves the latter
 * through [I18N] every time [value] is read, and registers its owning component for language
 * changes so a built node can refresh itself.
 */
internal class TextContent(initial: String, owner: Component<*>) {

    private var source: TextSource = TextSource.Literal(initial)

    init {
        I18N.watch(owner)
    }

    /** The text to show right now, with translatable keys resolved in the current language. */
    fun value(): String = this.source.resolve()

    /** Replaces the source with a literal string. */
    fun literal(value: String) {
        this.source = TextSource.Literal(value)
    }

    /** Replaces the source with a [Translatable] key. */
    fun translation(value: Translatable) {
        this.source = TextSource.Translation(value)
    }

    /** Restores the literal the component was created with. */
    fun reset(initial: String) {
        this.source = TextSource.Literal(initial)
    }
}
