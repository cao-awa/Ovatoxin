package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollapsePaneTest {

    @Test
    fun `starts collapsed with only the header visible`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px)

        assertFalse(pane.expanded)
        assertFalse(pane.contentArea.visible)
        assertEquals(40.px, pane.layoutSpec.height)
        assertEquals(2, pane.children.size)
        assertTrue(pane.children[0] === pane.contentArea || pane.children[1] === pane.contentArea)
    }

    @Test
    fun `starts expanded when asked to`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px, initiallyExpanded = true)

        assertTrue(pane.expanded)
        assertTrue(pane.contentArea.visible)
        assertEquals(300.px, pane.layoutSpec.height)
    }

    @Test
    fun `toggling swaps the height and the body visibility`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px)

        pane.toggle()
        assertTrue(pane.expanded)
        assertTrue(pane.contentArea.visible)
        assertEquals(300.px, pane.layoutSpec.height)

        pane.toggle()
        assertFalse(pane.expanded)
        assertFalse(pane.contentArea.visible)
        assertEquals(40.px, pane.layoutSpec.height)
    }

    @Test
    fun `a collapsed body is given no room, an expanded body fills below the header`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px, headerHeight = 40.px)

        pane.beginPass(ContentBox(0.0, 0.0, 800.0, 40.0), emptyList())
        assertEquals(0.px, pane.contentArea.layoutSpec.height)

        pane.expanded = true
        pane.beginPass(ContentBox(0.0, 0.0, 800.0, 300.0), emptyList())
        assertEquals(260.px, pane.contentArea.layoutSpec.height)
    }

    @Test
    fun `content draws into the body`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px)
        pane.content {
            draw(TextArea("val answer = 42"))
        }

        assertEquals(1, pane.contentArea.children.size)
        assertTrue(pane.contentArea.children[0] is TextArea)
    }

    @Test
    fun `title and expanded are restored by reset`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px)
        pane.expanded = true
        pane.title = "新标题"

        pane.reset()

        assertFalse(pane.expanded)
        assertEquals("示例代码", pane.title)
        assertEquals(40.px, pane.layoutSpec.height)
    }

    @Test
    fun `collapsed height follows a custom header height unless overridden`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px, headerHeight = 52.px)
        assertEquals(52.px, pane.layoutSpec.height)

        val explicit = CollapsePane(
            "示例代码",
            expandedHeight = 300.px,
            collapsedHeight = 36.px,
            headerHeight = 52.px,
        )
        assertEquals(36.px, explicit.layoutSpec.height)
    }

    @Test
    fun `the body surface is transparent so the panel fill shows through`() {
        val pane = CollapsePane(
            "示例代码",
            expandedHeight = 300.px,
            fill = Color.web("#101b33"),
            headerForeground = Color.WHITE,
        )

        val header = pane.children.first { it !== pane.contentArea } as FlexBox
        assertEquals(Color.TRANSPARENT, header.styleSpec.background.color)
        assertEquals(Color.TRANSPARENT, pane.contentArea.styleSpec.background.color)
        assertEquals(Color.web("#101b33"), pane.styleSpec.background.color)
        assertEquals(Color.WHITE, header.children.single().styleSpec.foreground)
    }

    @Test
    fun `clicking the header toggles the panel without a button`() {
        val pane = CollapsePane("示例代码", expandedHeight = 300.px)
        val header = pane.children.first { it !== pane.contentArea } as FlexBox

        assertFalse(pane.expanded)
        header.performClick()
        assertTrue(pane.expanded)
        assertTrue(pane.contentArea.visible)

        header.performClick()
        assertFalse(pane.expanded)
        assertFalse(pane.contentArea.visible)
    }

    @Test
    fun `scroll is off by default and can be turned on before rendering`() {
        val div = Div()
        val flow = FlexBox()

        assertFalse(div.scrollY)
        assertFalse(flow.scrollY)

        div.scrollY = true
        flow.scrollY = true

        assertTrue(div.scrollY)
        assertTrue(flow.scrollY)
    }
}
