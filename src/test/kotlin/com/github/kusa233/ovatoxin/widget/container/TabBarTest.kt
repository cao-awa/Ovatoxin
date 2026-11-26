package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests for the tab strip, without rendering it. */
class TabBarTest {

    @Test
    fun `a new bar is horizontal and empty`() {
        val bar = TabBar()

        assertEquals(TabSide.UP, bar.side)
        assertTrue(bar.tabs.isEmpty())
        assertNull(bar.selected)
        assertNull(bar.selectedButton)
    }

    @Test
    fun `add returns the button, appends it and selects it`() {
        val bar = TabBar()
        val first = Button("One")
        val second = Button("Two")

        assertSame(first, bar.add(first))
        assertEquals(listOf(first), bar.tabs)
        assertSame(first, bar.selected)

        bar.add(second) { text = "Second" }
        assertEquals(listOf(first, second), bar.tabs)
        assertEquals("Second", second.text)
        assertSame(second, bar.selected)
    }

    @Test
    fun `clicking a tab selects it and keeps the caller's click handler`() {
        var clicked = ""
        val bar = TabBar()
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two")) {
            onClick { clicked = text }
        }

        first.performClick()
        assertSame(first, bar.selected)

        second.performClick()
        assertSame(second, bar.selected)
        assertEquals("Two", clicked)
    }

    @Test
    fun `selecting a tab can be done by button or index`() {
        val bar = TabBar()
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two"))

        bar.select(first)
        assertSame(first, bar.selected)

        bar.select(1)
        assertSame(second, bar.selected)

        bar.clearSelection()
        assertNull(bar.selected)
        assertNull(bar.selectedButton)
    }

    @Test
    fun `remove drops the tab and selects the first remaining one`() {
        val bar = TabBar()
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two"))
        val other = Button("Other")

        assertFalse(bar.remove(other))
        assertTrue(bar.remove(second))
        assertEquals(listOf(first), bar.tabs)
        assertSame(first, bar.selected)

        assertTrue(bar.remove(first))
        assertTrue(bar.tabs.isEmpty())
        assertNull(bar.selected)
    }

    @Test
    fun `tab x and y are ignored`() {
        val bar = TabBar()
        val button = bar.add(Button("One")) {
            layoutSpec.x = 15.px
            layoutSpec.y = 12.px
            layoutSpec.width = 30.px
            layoutSpec.height = 20.px
        }
        val box = bar.contentBox(100.0, 50.0)

        bar.beginPass(box, bar.children)

        assertEquals(Length.ZERO, button.layoutSpec.x)
        assertEquals(Length.ZERO, button.layoutSpec.y)
    }

    @Test
    fun `a vertical bar stacks tabs downwards with the gap between them`() {
        val bar = TabBar()
        bar.side = TabSide.LEFT
        bar.gap = 4.px
        val first = bar.add(Button("One")) { layoutSpec.height = 20.px }
        val second = bar.add(Button("Two")) { layoutSpec.height = 20.px }
        val box = bar.contentBox(90.0, 200.0)

        bar.beginPass(box, bar.children)

        assertEquals(Slot(0.0, 0.0, 20.0), bar.slotFor(first, box))
        assertEquals(Slot(0.0, 24.0, 20.0), bar.slotFor(second, box))
    }

    @Test
    fun `a horizontal bar keeps the normal flex flow`() {
        val bar = TabBar()
        bar.gap = 6.px
        val first = bar.add(Button("One")) {
            layoutSpec.width = 30.px
            layoutSpec.height = 20.px
        }
        val second = bar.add(Button("Two")) {
            layoutSpec.width = 40.px
            layoutSpec.height = 20.px
        }
        val box = bar.contentBox(200.0, 40.0)

        bar.beginPass(box, bar.children)

        assertEquals(Slot(62.0, 0.0, null), bar.slotFor(first, box))
        assertEquals(Slot(98.0, 0.0, null), bar.slotFor(second, box))
    }

    @Test
    fun `a horizontal bar stretches tabs to its height and centres them`() {
        val bar = TabBar()
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two"))

        assertEquals(Justify.CENTER, bar.justify)
        assertEquals(100.percent, first.layoutSpec.height)
        assertEquals(100.percent, second.layoutSpec.height)
    }

    @Test
    fun `a vertical bar stretches tabs to its width`() {
        val bar = TabBar()
        bar.side = TabSide.LEFT
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two"))

        assertEquals(100.percent, first.layoutSpec.width)
        assertEquals(100.percent, second.layoutSpec.width)
    }

    @Test
    fun `switching the side swaps the stretched axis and keeps the other size`() {
        val bar = TabBar()
        val button = bar.add(Button("One")) {
            layoutSpec.width = 30.px
            layoutSpec.height = 20.px
        }

        assertEquals(30.px, button.layoutSpec.width)
        assertEquals(100.percent, button.layoutSpec.height)

        bar.side = TabSide.RIGHT
        assertEquals(100.percent, button.layoutSpec.width)
        assertEquals(20.px, button.layoutSpec.height)

        bar.side = TabSide.BOTTOM
        assertEquals(30.px, button.layoutSpec.width)
        assertEquals(100.percent, button.layoutSpec.height)
    }

    @Test
    fun `a standalone bar can bind content to an explicit tab`() {
        val bar = TabBar()
        val first = bar.add(Button("One"))
        val firstContent = bar.bind(first, TextArea("one"))
        assertSame(first, bar.selected)
        assertTrue(firstContent.visible)

        bar.add(Button("Two"))
        assertFalse(firstContent.visible)

        bar.select(first)
        assertTrue(firstContent.visible)
        assertEquals(listOf(firstContent), bar.componentsFor(first))
    }

    @Test
    fun `binding to a foreign button is refused`() {
        val bar = TabBar()
        bar.add(Button("One"))

        assertFailsWith<IllegalArgumentException> {
            bar.bind(Button("not a tab"), TextArea("orphan"))
        }
    }

    @Test
    fun `selectColor paints only the selected tab`() {
        val bar = TabBar()
        bar.selectColor = Color.RED
        val first = bar.add(Button("One"))
        val second = bar.add(Button("Two"))

        assertNull(first.styleSpec.background.color)
        assertEquals(Color.RED, second.styleSpec.background.color)

        bar.select(first)
        assertEquals(Color.RED, first.styleSpec.background.color)
        assertNull(second.styleSpec.background.color)
    }

    @Test
    fun `an unstyled tab lets the bar background show through`() {
        val bar = TabBar()
        val tab = bar.add(Button("One"))

        assertNull(tab.styleSpec.background.color)
    }

    @Test
    fun `deselecting restores the button's own background`() {
        val bar = TabBar()
        val first = bar.add(Button("One")) {
            styleSpec.background { color = Color.BLUE }
        }
        val second = bar.add(Button("Two"))
        bar.selectColor = Color.RED
        assertEquals(Color.RED, second.styleSpec.background.color)

        bar.select(first)
        assertEquals(Color.RED, first.styleSpec.background.color)
        assertNull(second.styleSpec.background.color)

        bar.select(second)
        assertEquals(Color.BLUE, first.styleSpec.background.color)
        assertEquals(Color.RED, second.styleSpec.background.color)
    }

    @Test
    fun `reset restores the side and selects the first tab`() {
        val bar = TabBar()
        val first = bar.add(Button("One"))
        bar.add(Button("Two"))
        bar.side = TabSide.RIGHT
        bar.selected = bar.tabs.last()

        bar.reset()

        assertEquals(TabSide.UP, bar.side)
        assertSame(first, bar.selected)
    }
}
