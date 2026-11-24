package com.github.kusa233.ovatoxin.widget.text

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.runtime.Browser
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LinkTest {

    private val opened = mutableListOf<String>()

    @BeforeTest
    fun captureBrowser() {
        this.opened.clear()
        Browser.opener = { url -> this.opened += url }
    }

    @AfterTest
    fun restoreBrowser() {
        Browser.reset()
    }

    @Test
    fun `a click opens the url in the browser`() {
        val link = Link("Ovatoxin", "https://github.com/kusa233/Ovatoxin")
        link.performClick()
        assertEquals(listOf("https://github.com/kusa233/Ovatoxin"), this.opened)
    }

    @Test
    fun `a replaced onClick runs instead of the browser`() {
        var clicked: String? = null
        val link = Link("Ovatoxin", "https://github.com/kusa233/Ovatoxin")
        link.onClick {
            clicked = this.url
        }

        link.performClick()

        assertEquals("https://github.com/kusa233/Ovatoxin", clicked)
        assertTrue(this.opened.isEmpty(), "the browser must not be opened")
    }

    @Test
    fun `the handler works without a url`() {
        var clicked = 0
        val link = Link("plain")
        link.onClick { clicked++ }

        link.performClick()
        assertEquals(1, clicked)
        assertTrue(this.opened.isEmpty())
    }

    @Test
    fun `an empty url opens nothing by default`() {
        Link("no url").performClick()
        assertTrue(this.opened.isEmpty())
    }

    @Test
    fun `hasOnClick tells the two modes apart`() {
        val link = Link("Ovatoxin", "https://example.com")
        assertFalse(link.hasOnClick)

        link.onClick { }
        assertTrue(link.hasOnClick)
    }

    @Test
    fun `a link keeps its url readable inside the handler`() {
        var read: String? = null
        val link = Link("x", "https://a.example")
        link.onClick { read = this.url }

        link.performClick()
        assertEquals("https://a.example", read)
    }

    @Test
    fun `text supports both property and call style`() {
        val link = Link()
        link.text = "a"
        assertEquals("a", link.text())
        link.text("b")
        assertEquals("b", link.text)
    }

    @Test
    fun `onClick returns the link so calls chain`() {
        val link = Link()
        assertSame(link, link.onClick { })
    }

    @Test
    fun `a link is styled with a foreground by default`() {
        assertEquals(Link.DEFAULT_FOREGROUND, Link().styleSpec.foreground)
    }

    @Test
    fun `a link can be drawn and clicked like any other component`() {
        var clicked = 0
        val frame = Frame()
        frame.layout {
            draw(Link("Ovatoxin", "https://example.com")) { link ->
                layout {
                    x = 10.percent
                    y = 80.percent
                }
                link.onClick { clicked++ }
            }
        }

        (frame.children[0] as Link).performClick()
        assertEquals(1, clicked)
        assertTrue(this.opened.isEmpty())
    }
}
