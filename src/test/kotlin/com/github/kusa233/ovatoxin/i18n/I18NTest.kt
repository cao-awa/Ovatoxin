package com.github.kusa233.ovatoxin.i18n

import com.github.kusa233.ovatoxin.widget.text.Label
import com.github.kusa233.ovatoxin.widget.text.TextArea
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18NTest {

    @BeforeTest
    fun resetRegistry() {
        I18N.clear()
    }

    @AfterTest
    fun cleanUp() {
        I18N.clear()
    }

    @Test
    fun `lang and currentLang resolve a translation`() {
        I18N.lang("zh-cn", "greeting", "你好")
        I18N.lang("en-us", "greeting", "Hello")

        assertEquals("zh-cn", I18N.currentLang, "the first language registered becomes current")
        assertEquals("你好", I18N.content("greeting"))

        I18N.currentLang = "en-us"
        assertEquals("Hello", I18N.content("greeting"))
    }

    @Test
    fun `an unknown key falls back to itself`() {
        assertEquals("missing.key", I18N.content("missing.key"))
        assertEquals("missing.key", Translatable("missing.key").content())
    }

    @Test
    fun `a translated text area follows the current language`() {
        I18N.lang("zh-cn", "hello", "你好")
        I18N.lang("en-us", "hello", "Hello")

        val area = TextArea().apply { text("hello".translatable) }
        assertEquals("你好", area.text())

        I18N.currentLang = "en-us"
        assertEquals("Hello", area.text())
    }

    @Test
    fun `a Translatable value resolves the same way`() {
        I18N.lang("zh-cn", "hello", "你好")

        val label = Label().apply { text(Translatable("hello")) }
        assertEquals("你好", label.text)
    }

    @Test
    fun `a plain string is shown literally`() {
        I18N.lang("zh-cn", "hello", "你好")

        val label = Label("hello")
        assertEquals("hello", label.text, "only a Translatable asks I18N for content")

        label.text("hello")
        assertEquals("hello", label.text())
    }

    @Test
    fun `replacing a translation refreshes a live component`() {
        I18N.lang("zh-cn", "greeting", "旧")
        val area = TextArea().apply { text("greeting".translatable) }
        assertEquals("旧", area.text())

        I18N.lang("zh-cn", "greeting", "新")
        assertEquals("新", area.text())
    }

    @Test
    fun `languages exposes a snapshot of every registration`() {
        I18N.lang("zh-cn", "a", "甲")
        I18N.lang("en-us", "a", "A")

        val snapshot = I18N.languages
        assertEquals(mapOf("a" to "甲"), snapshot["zh-cn"])
        assertEquals(mapOf("a" to "A"), snapshot["en-us"])
        assertTrue(snapshot.isNotEmpty())
    }

    @Test
    fun `clear removes every language and resets currentLang`() {
        I18N.lang("zh-cn", "a", "甲")
        I18N.clear()

        assertEquals("", I18N.currentLang)
        assertTrue(I18N.languages.isEmpty())
        assertEquals("a", I18N.content("a"))
    }
}
