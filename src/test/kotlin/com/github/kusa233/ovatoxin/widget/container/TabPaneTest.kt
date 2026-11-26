package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.state.State
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
import kotlin.time.Duration.Companion.milliseconds

/** Tests for tab selection, binding and the geometry of the bar around the content. */
class TabPaneTest {

    @Test
    fun `a new pane has an empty top bar`() {
        val pane = TabPane()

        assertEquals(TabSide.UP, pane.side)
        assertEquals(TabSide.UP, pane.tab.side)
        assertTrue(pane.tabs.isEmpty())
        assertNull(pane.selectedTab)
        assertSame(pane.tab, pane.tabBar)
        assertSame(pane.tab, pane.children[0])
    }

    @Test
    fun `bar size can be given to the constructor`() {
        val pane = TabPane(30.px)

        assertEquals(30.px, pane.barSize)
        assertEquals(30.px, pane.tabBarThickness)
    }

    @Test
    fun `bar size is the height for top and bottom and the width for left and right`() {
        val pane = TabPane(30.px)
        val box = pane.contentBox(200.0, 100.0)

        pane.side = TabSide.UP
        pane.beginPass(box, pane.children)
        assertEquals(30.px, pane.tab.layoutSpec.height)
        assertEquals(200.px, pane.tab.layoutSpec.width)

        pane.side = TabSide.LEFT
        pane.beginPass(box, pane.children)
        assertEquals(30.px, pane.tab.layoutSpec.width)
        assertEquals(100.px, pane.tab.layoutSpec.height)
    }

    @Test
    fun `the pane style is applied to its tab bar`() {
        val pane = TabPane()
        pane.applyStyle {
            background { color = Color.BLUE }
            corner = 8.px
        }

        pane.beginPass(pane.contentBox(200.0, 100.0), pane.children)

        assertEquals(Color.BLUE, pane.tab.styleSpec.background.color)
        assertEquals(8.px, pane.tab.styleSpec.corner)
    }

    @Test
    fun `a pane transition reaches its tab buttons`() {
        val pane = TabPane()
        pane.applyStyle { transition = 200.milliseconds }
        val first = pane.add(Button("One"))
        val second = pane.add(Button("Two")) {
            styleSpec.transition = 50.milliseconds
        }

        pane.beginPass(pane.contentBox(200.0, 100.0), pane.children)

        assertEquals(200.milliseconds, pane.tab.styleSpec.transition)
        assertEquals(200.milliseconds, first.styleSpec.transition)
        assertEquals(50.milliseconds, second.styleSpec.transition)
    }

    @Test
    fun `selectColor is shared with the tab bar`() {
        val pane = TabPane()
        pane.selectColor = Color.RED

        assertEquals(Color.RED, pane.tab.selectColor)
        assertEquals(Color.RED, pane.selectColor)

        val first = pane.add(Button("One"))
        val second = pane.add(Button("Two"))
        assertNull(first.styleSpec.background.color)
        assertEquals(Color.RED, second.styleSpec.background.color)
    }

    @Test
    fun `the legacy thickness property writes the same size`() {
        val pane = TabPane()
        pane.tabBarThickness = 20.px

        assertEquals(20.px, pane.barSize)
        pane.barSize = 25.px
        assertEquals(25.px, pane.tabBarThickness)
    }

    @Test
    fun `side can be set on the pane or on its bar`() {
        val pane = TabPane()

        pane.side = TabSide.LEFT
        assertEquals(TabSide.LEFT, pane.tab.side)
        assertEquals(TabSide.LEFT, pane.side)

        pane.tab.side = TabSide.RIGHT
        assertEquals(TabSide.RIGHT, pane.side)
    }

    @Test
    fun `the bar is placed above the content by default`() {
        val pane = TabPane()
        pane.barSize = 30.px
        val box = pane.contentBox(200.0, 100.0)

        pane.beginPass(box, pane.children)

        assertEquals(0.px, pane.tab.layoutSpec.x)
        assertEquals(0.px, pane.tab.layoutSpec.y)
        assertEquals(200.px, pane.tab.layoutSpec.width)
        assertEquals(30.px, pane.tab.layoutSpec.height)

        val content = pane.children[1]
        assertEquals(0.px, content.layoutSpec.x)
        assertEquals(30.px, content.layoutSpec.y)
        assertEquals(200.px, content.layoutSpec.width)
        assertEquals(70.px, content.layoutSpec.height)
    }

    @Test
    fun `every side puts the bar and the content in the right place`() {
        val pane = TabPane()
        pane.barSize = 30.px

        fun place(side: TabSide): Pair<Container<*>, Container<*>> {
            pane.side = side
            val box = pane.contentBox(200.0, 100.0)
            pane.beginPass(box, pane.children)
            return pane.tab to (pane.children[1] as Container<*>)
        }

        val (bottomBar, bottomContent) = place(TabSide.BOTTOM)
        assertEquals(70.px, bottomBar.layoutSpec.y)
        assertEquals(30.px, bottomBar.layoutSpec.height)
        assertEquals(0.px, bottomContent.layoutSpec.y)
        assertEquals(70.px, bottomContent.layoutSpec.height)

        val (leftBar, leftContent) = place(TabSide.LEFT)
        assertEquals(0.px, leftBar.layoutSpec.x)
        assertEquals(30.px, leftBar.layoutSpec.width)
        assertEquals(100.px, leftBar.layoutSpec.height)
        assertEquals(30.px, leftContent.layoutSpec.x)
        assertEquals(170.px, leftContent.layoutSpec.width)
        assertEquals(100.px, leftContent.layoutSpec.height)

        val (rightBar, rightContent) = place(TabSide.RIGHT)
        assertEquals(170.px, rightBar.layoutSpec.x)
        assertEquals(30.px, rightBar.layoutSpec.width)
        assertEquals(100.px, rightBar.layoutSpec.height)
        assertEquals(0.px, rightContent.layoutSpec.x)
        assertEquals(170.px, rightContent.layoutSpec.width)
        assertEquals(100.px, rightContent.layoutSpec.height)
    }

    @Test
    fun `add attaches the button to the bar so it can be drawn`() {
        val pane = TabPane()
        val button = pane.add(Button("One"))

        assertSame(pane.tab, button.parent)
        assertTrue(pane.tab.children.contains(button))
        assertSame(button, pane.selectedTab)
    }

    @Test
    fun `binding to a foreign button is refused`() {
        val pane = TabPane()
        pane.add(Button("One"))

        assertFailsWith<IllegalArgumentException> {
            pane.bind(Button("not a tab"), TextArea("orphan"))
        }
    }

    @Test
    fun `only the selected tab's components are visible`() {
        val pane = TabPane()

        val first = pane.add(Button("One"))
        val firstContent = pane.bind(first, TextArea()) { it.text = "one" }
        assertSame(first, pane.selectedTab)
        assertTrue(firstContent.visible)

        val second = pane.add(Button("Two"))
        val secondContent = pane.bind(second, TextArea("two"))
        assertFalse(firstContent.visible)
        assertTrue(secondContent.visible)

        pane.tab.select(first)
        assertTrue(firstContent.visible)
        assertFalse(secondContent.visible)

        first.performClick()
        assertSame(first, pane.selectedTab)
        assertTrue(firstContent.visible)
    }

    @Test
    fun `content can be bound to a tab that is not selected yet`() {
        val pane = TabPane()
        val first = pane.add(Button("One"))
        val second = pane.add(Button("Two"))
        assertSame(second, pane.selectedTab)

        val firstContent = pane.bind(first, TextArea("one"))
        val secondContent = pane.bind(second, TextArea("two"))
        assertFalse(firstContent.visible)
        assertTrue(secondContent.visible)

        pane.select(first)
        assertTrue(firstContent.visible)
        assertFalse(secondContent.visible)
    }

    @Test
    fun `a bound component fills the page but its x and y are ignored`() {
        val pane = TabPane()
        val tab = pane.add(Button("One"))

        val content = pane.bind(tab, TextArea("one")) {
            layout {
                x = 10.px
                y = 20.px
            }
        }

        assertEquals(Length.ZERO, content.layoutSpec.x)
        assertEquals(Length.ZERO, content.layoutSpec.y)
        assertEquals(100.percent, content.layoutSpec.width)
        assertEquals(100.percent, content.layoutSpec.height)

        val explicit = pane.bind(tab, TextArea("two")) {
            layout {
                width = 50.px
                height = 25.px
            }
        }
        assertEquals(50.px, explicit.layoutSpec.width)
        assertEquals(25.px, explicit.layoutSpec.height)
    }

    @Test
    fun `a bound page fills the area left over after the tab bar`() {
        val pane = TabPane()
        pane.barSize = 30.px
        val tab = pane.add(Button("One"))
        val content = pane.bind(tab, TextArea("one"))

        val box = pane.contentBox(200.0, 100.0)
        pane.beginPass(box, pane.children)

        val host = pane.children[1] as Container<*>
        assertEquals(listOf(content), host.children)

        val hostWidth = host.layoutSpec.width!!.resolve(box.width)
        val hostHeight = host.layoutSpec.height!!.resolve(box.height)
        assertEquals(200.0, hostWidth)
        assertEquals(70.0, hostHeight)

        val hostBox = host.contentBox(hostWidth, hostHeight)
        assertEquals(hostBox.width, content.layoutSpec.width!!.resolve(hostBox.width))
        assertEquals(hostBox.height, content.layoutSpec.height!!.resolve(hostBox.height))
        assertEquals(200.0, content.layoutSpec.width!!.resolve(hostBox.width))
        assertEquals(70.0, content.layoutSpec.height!!.resolve(hostBox.height))
    }

    @Test
    fun `a bound component replays its block when its state changes`() {
        val pane = TabPane()
        val tab = pane.add(Button("One"))
        val state = State("one")
        val content = pane.bind(tab, TextArea()) { it.text = state.value }

        content.withState(state)
        assertEquals("one", content.text)

        state.update("two")
        assertEquals("two", content.text)
        assertEquals(Length.ZERO, content.layoutSpec.x)
        assertEquals(Length.ZERO, content.layoutSpec.y)
        assertEquals(100.percent, content.layoutSpec.width)
        assertEquals(100.percent, content.layoutSpec.height)
    }

    @Test
    fun `a redraw cannot reveal a component of an unselected tab`() {
        val pane = TabPane()
        val first = pane.add(Button("One"))
        val state = State("one")
        val content = pane.bind(first, TextArea()) { it.text = state.value }
        content.withState(state)

        pane.add(Button("Two"))
        assertFalse(content.visible)

        state.update("two")
        assertEquals("two", content.text)
        assertFalse(content.visible)
    }

    @Test
    fun `the bar can bind content on the pane's behalf`() {
        val pane = TabPane()
        val first = pane.add(Button("One"))
        val content = pane.tab.bind(first, TextArea("one"))

        assertSame(first, pane.selectedTab)
        assertEquals(listOf(content), pane.componentsFor(first))
        assertTrue(content.visible)
    }

    @Test
    fun `removing a tab removes its content and selects the next tab`() {
        val pane = TabPane()
        val first = pane.add(Button("One"))
        val firstContent = pane.bind(first, TextArea("one"))
        val second = pane.add(Button("Two"))
        val secondContent = pane.bind(second, TextArea("two"))

        assertTrue(pane.remove(first))
        assertNull(firstContent.parent)
        assertTrue(pane.componentsFor(first).isEmpty())
        assertSame(second, pane.selectedTab)
        assertTrue(secondContent.visible)
        assertFalse(pane.remove(first))

        assertTrue(pane.remove(second))
        assertNull(secondContent.parent)
        assertTrue(pane.tabs.isEmpty())
        assertNull(pane.selectedTab)
    }

    @Test
    fun `the side constants can be used unqualified`() {
        val pane = TabPane()

        pane.side = LEFT
        assertEquals(TabSide.LEFT, pane.side)

        pane.tab.side = BOTTOM
        assertEquals(TabSide.BOTTOM, pane.side)
    }

    @Test
    fun `clearing the selection hides every bound component`() {
        val pane = TabPane()
        val tab = pane.add(Button("One"))
        val content = pane.bind(tab, TextArea("one"))
        assertTrue(content.visible)

        pane.clearSelection()
        assertNull(pane.selected)
        assertFalse(content.visible)
    }

    @Test
    fun `more than one component can be bound to the same tab`() {
        val pane = TabPane()
        val tab = pane.add(Button("One"))
        val first = pane.bind(tab, TextArea("one"))
        val second = pane.bind(tab, TextArea("two"))

        assertEquals(listOf(first, second), pane.componentsFor(tab))
        assertTrue(first.visible)
        assertTrue(second.visible)
    }
}
