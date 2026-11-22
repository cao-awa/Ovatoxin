package com.github.kusa233.ovatoxin.state

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.Node
import javafx.scene.layout.Pane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateTest {

    private val languages = mapOf(
        "zh-cn" to mapOf("test.translation" to "翻译测试"),
        "en-us" to mapOf("test.translation" to "Test translation"),
    )

    @Test
    fun `a state starts at its initial value`() {
        val state = State("zh-cn")

        assertEquals("zh-cn", state.value)
        assertTrue(state.components.isEmpty())
    }

    @Test
    fun `updating saves the new value`() {
        val state = State("zh-cn")

        state.update("en-us")

        assertEquals("en-us", state.value)
    }

    @Test
    fun `two states are independent of each other`() {
        val language = State("zh-cn")
        val theme = State("light")

        language.update("en-us")

        assertEquals("en-us", language.value)
        assertEquals("light", theme.value)
    }

    @Test
    fun `a state with no bound component is harmless`() {
        val state = State("zh-cn")

        state.update("en-us")

        assertEquals("en-us", state.value)
        assertEquals(0, state.components.size)
    }

    @Test
    fun `a state remembers what is bound to it`() {
        val state = State(0)
        val first = CountingComponent()
        val second = CountingComponent()

        state.bind(first)
        state.bind(second)

        assertEquals(2, state.components.size)
        assertTrue(state.components.contains(first))
        assertTrue(state.components.contains(second))
    }

    @Test
    fun `updating with the same value does not redraw anything`() {
        val state = State("zh-cn")
        val counter = CountingComponent()
        state.bind(counter)

        state.update("zh-cn")

        assertEquals(0, counter.redraws)
    }

    @Test
    fun `a changed value redraws every bound component once`() {
        val state = State("zh-cn")
        val first = CountingComponent()
        val second = CountingComponent()
        state.bind(first)
        state.bind(second)

        state.update("en-us")

        assertEquals(1, first.redraws)
        assertEquals(1, second.redraws)
    }

    @Test
    fun `a component bound twice is only redrawn once`() {
        val state = State("zh-cn")
        val counter = CountingComponent()
        state.bind(counter)
        state.bind(counter)

        state.update("en-us")

        assertEquals(1, state.components.size)
        assertEquals(1, counter.redraws)
    }

    @Test
    fun `a component bound to two states redraws for each of them`() {
        val language = State("zh-cn")
        val theme = State("light")
        val counter = CountingComponent()
        language.bind(counter)
        theme.bind(counter)

        language.update("en-us")
        theme.update("dark")

        assertEquals(2, counter.redraws)
    }

    @Test
    fun `writing the value directly redraws just like an update`() {
        val state = State("zh-cn")
        val counter = CountingComponent()
        state.bind(counter)

        state.value = "en-us"

        assertEquals("en-us", state.value)
        assertEquals(1, counter.redraws)
    }

    @Test
    fun `only the bound text area is redrawn, its sibling keeps its text`() {
        val state = State("zh-cn")
        val translations = this.languages
        val frame = Frame()
        val bound = frame.draw(TextArea()) { area ->
            area.text(translations.getValue(state.value).getValue("test.translation"))
        }.withState(state)
        val sibling = frame.draw(TextArea()) { area -> area.text("never changes") }

        assertEquals("翻译测试", bound.text)
        assertEquals("never changes", sibling.text)

        state.update("en-us")

        assertEquals("en-us", state.value)
        assertEquals("Test translation", bound.text)
        assertEquals("never changes", sibling.text)
        assertEquals(2, frame.children.size)
    }

    @Test
    fun `a redraw does not duplicate the children of a re-drawn container`() {
        val state = State("zh-cn")
        val translations = this.languages
        val frame = Frame()
        frame.draw(TextArea()) { area ->
            area.text(translations.getValue(state.value).getValue("test.translation"))
        }.withState(state)

        state.update("en-us")
        state.update("zh-cn")

        assertEquals(1, frame.children.size)
    }

    @Test
    fun `a text area without a state keeps the text it was given`() {
        val text = TextArea()

        text.text("standalone")

        assertEquals("standalone", text.text)
    }

    /** A component that counts redraws instead of building a node. */
    private class CountingComponent : Component<CountingComponent>() {

        var redraws: Int = 0
            private set

        override fun buildNode(): Node = Pane()

        override fun replayDrawBlock() {
            this.redraws++
        }
    }
}
