package com.github.kusa233.ovatoxin.i18n

/**
 * A translation key whose text is looked up from [I18N] whenever it is displayed.
 *
 * A component given a `Translatable` keeps the key, not the translation that happened to be active
 * when the component was configured. Changing [I18N.currentLang] therefore switches the visible
 * text without asking every widget to be configured again. This is the difference between
 * `"translate.key".translatable` and the literal `"translate.key"`.
 *
 * The key itself is used as the fallback when [I18N] has no translation for it.
 */
data class Translatable(val key: String) {

    /** The content of this key in the current language. */
    fun content(): String = I18N.content(this.key)

    /** The content of this key in [langKey], regardless of [I18N.currentLang]. */
    fun content(langKey: String): String = I18N.content(langKey, this.key)

    /** Renders as the translation, so logging a [Translatable] shows what the user sees. */
    override fun toString(): String = this.content()
}

/**
 * Views this string as a [Translatable], so a key reads naturally at the call site:
 *
 * ```kotlin
 * label.text("translate.key".translatable)
 * ```
 */
val String.translatable: Translatable get() = Translatable(this)
