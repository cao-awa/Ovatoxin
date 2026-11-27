package com.github.kusa233.ovatoxin.i18n

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.runtime.Fx
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The process-wide translation registry used by [Translatable] text.
 *
 * Register translations once, choose the language with [currentLang], and every text component
 * holding a [Translatable] displays the corresponding content. Text configured with a plain
 * [String] is never translated.
 *
 * ```kotlin
 * I18N.lang("en-us", "hello", "Hello")
 * I18N.lang("zh-cn", "hello", "你好")
 * I18N.currentLang = "zh-cn"
 * label.text("hello".translatable) // 你好
 * ```
 *
 * Registering the first translation selects its language, so a small application only needs
 * [lang] calls; [currentLang] is what changes the visible language afterwards.
 */
object I18N {

    /** language key -> translation key -> content. */
    private val translations: MutableMap<String, MutableMap<String, String>> = LinkedHashMap()

    /**
     * All registered translations, by language and then by key.
     *
     * This is a snapshot: mutating the returned maps does not change what [I18N] resolves.
     */
    val languages: Map<String, Map<String, String>>
        @Synchronized get() = this.translations.mapValues { (_, values) -> values.toMap() }

    private val watchers: CopyOnWriteArrayList<WeakReference<Component<*>>> = CopyOnWriteArrayList()

    /**
     * The language [Translatable] text is resolved in. Starts empty, and the first [lang] call sets
     * it, so text before any registration falls back to its key.
     */
    @Volatile
    var currentLang: String = ""
        set(value) {
            if (field == value) return
            field = value
            notifyLanguageChanged()
        }

    /**
     * Registers [content] as the translation of [translationKey] in [langKey].
     *
     * Calling it again for the same pair replaces the previous content. The first language
     * registered becomes [currentLang] if no language has been selected yet.
     */
    @Synchronized
    fun lang(langKey: String, translationKey: String, content: String) {
        this.translations.getOrPut(langKey) { LinkedHashMap() }[translationKey] = content
        if (this.currentLang.isEmpty()) {
            this.currentLang = langKey
        } else {
            // The same key may be visible right now even when the language did not change.
            notifyLanguageChanged()
        }
    }

    /** The content of [translationKey] in [currentLang], falling back to the key itself. */
    fun content(translationKey: String): String = content(this.currentLang, translationKey)

    /** The content of [translationKey] in [langKey], falling back to the key itself. */
    @Synchronized
    fun content(langKey: String, translationKey: String): String =
        this.translations[langKey]?.get(translationKey) ?: translationKey

    /** Removes every translation and resets [currentLang] to empty. */
    @Synchronized
    fun clear() {
        this.translations.clear()
        this.currentLang = ""
    }

    /**
     * Adds [component] to the weak set notified when translations change.
     *
     * Components that own text call this from their constructor. The registry only keeps a weak
     * reference, so a component that is no longer used can still be collected.
     */
    @Synchronized
    internal fun watch(component: Component<*>) {
        this.watchers.removeAll { it.get() == null }
        if (this.watchers.none { it.get() === component }) {
            this.watchers.add(WeakReference(component))
        }
    }

    /** Drops collected components and asks the live ones to re-read their translatable text. */
    private fun notifyLanguageChanged() {
        val live = this.watchers.mapNotNull { it.get() }
        if (live.isEmpty()) return
        Fx.dispatchToFxThread {
            live.forEach { component ->
                runCatching { component.onLanguageChanged() }
            }
        }
    }
}
