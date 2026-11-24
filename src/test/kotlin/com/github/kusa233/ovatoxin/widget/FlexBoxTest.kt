package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the flow of a [FlexBox].
 *
 * The flow is decided in [Container.beginPass], which needs no node and no toolkit — a box whose
 * children were never rendered can still be asked where it *would* put them, because every child
 * resolves to its preferred size. Those are the assertions below; the pixels that actually reach
 * the screen are checked by `RenderProbe`, which runs on the FX thread.
 */
class FlexBoxTest {

    /** The content box of a box of [width] by [height], i.e. its own size minus its padding. */
    private fun boxOf(box: FlexBox, width: Double, height: Double): ContentBox =
        box.contentBox(width, height)

    /** Where [box] would put [child] inside a [width] by [height] box. */
    private fun slotOf(box: FlexBox, child: Component<*>, width: Double, height: Double): Slot {
        val content = box.contentBox(width, height)
        // The flow is computed by the layout pass; asking for it directly is enough here, since
        // it needs neither a node nor a toolkit.
        box.beginPass(content, box.children)
        return box.slotFor(child, content)
    }

    /**
     * Declares the padding of [component] the way its layout block would.
     *
     * Padding is a value of the layout a component is drawn with, not an argument of the call that
     * declares its children, so it is set the same way `x` or `width` would be.
     */
    private fun <C : Component<*>> padded(component: C, horizontal: Length, vertical: Length): C {
        component.applyLayout {
            hPadding = horizontal
            vPadding = vertical
        }
        return component
    }

    @Test
    fun `a flex box is a drawing surface`() {
        val box = FlexBox()
        box.layout {
            draw(TextArea("one"))
        }
        assertEquals(1, box.children.size)
    }

    @Test
    fun `padding insets the content on all four sides`() {
        val box = padded(FlexBox(), 10.px, 6.px)

        val content = this.boxOf(box, 200.0, 100.0)
        assertEquals(10.0, content.offsetX)
        assertEquals(6.0, content.offsetY)
        assertEquals(180.0, content.width)
        assertEquals(88.0, content.height)
    }

    @Test
    fun `the layout block of a surface declares its padding`() {
        val panel = Div()
        Frame().layout {
            draw(panel) {
                layout {
                    width = 100.percent
                    hPadding = 14.px
                    vPadding = 7.px
                }
            }
        }

        assertEquals(14.px, panel.layoutSpec.hPadding)
        assertEquals(7.px, panel.layoutSpec.vPadding)

        val content = panel.contentBox(200.0, 100.0)
        assertEquals(14.0, content.offsetX)
        assertEquals(7.0, content.offsetY)
        assertEquals(172.0, content.width)
        assertEquals(86.0, content.height)
    }

    @Test
    fun `a plain surface has no padding`() {
        val div = Div()
        div.layout { }
        assertEquals(Length.ZERO, div.layoutSpec.hPadding)
        assertEquals(Length.ZERO, div.layoutSpec.vPadding)

        val content = div.contentBox(100.0, 50.0)
        assertEquals(0.0, content.offsetX)
        assertEquals(0.0, content.offsetY)
        assertEquals(100.0, content.width)
        assertEquals(50.0, content.height)
    }

    @Test
    fun `a percentage padding resolves against the surface`() {
        val div = padded(Div(), 10.percent, 20.percent)

        val content = div.contentBox(200.0, 100.0)
        assertEquals(20.0, content.offsetX)
        assertEquals(20.0, content.offsetY)
        assertEquals(160.0, content.width)
        assertEquals(60.0, content.height)
    }

    @Test
    fun `padding larger than the surface leaves an empty content box`() {
        val div = padded(Div(), 500.px, 500.px)

        // Capped at half the box, so the content collapses into the middle instead of going
        // negative — a negative size would resolve percentages into nonsense.
        val content = div.contentBox(200.0, 100.0)
        assertEquals(100.0, content.offsetX)
        assertEquals(50.0, content.offsetY)
        assertEquals(0.0, content.width)
        assertEquals(0.0, content.height)
    }

    @Test
    fun `the first child starts in the corner of the content box`() {
        val box = padded(FlexBox(), 8.px, 8.px)
        val first = TextArea("one")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
        }

        val slot = this.slotOf(box, first, 400.0, 200.0)
        assertEquals(8.0, slot.x)
        assertEquals(8.0, slot.y)
    }

    @Test
    fun `children flow to the right with the padding as the gap`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 50.px; height = 20.px } }
            draw(second) { layout { width = 50.px; height = 20.px } }
        }

        val firstSlot = this.slotOf(box, first, 400.0, 200.0)
        val secondSlot = this.slotOf(box, second, 400.0, 200.0)
        assertEquals(10.0, firstSlot.x)
        assertEquals(10.0, firstSlot.y)
        assertEquals(70.0, secondSlot.x)
        assertEquals(10.0, secondSlot.y)
    }

    @Test
    fun `a child that does not fit starts a new line`() {
        val box = padded(FlexBox(), 10.px, 6.px)
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 100.px; height = 20.px } }
            draw(second) { layout { width = 100.px; height = 20.px } }
        }

        // 10 + 100 + 10 + 100 exceeds the 200px content box, so the second child wraps below the
        // first line: the inset, that line, and one gap.
        val secondSlot = this.slotOf(box, second, 220.0, 200.0)
        assertEquals(10.0, secondSlot.x)
        assertEquals(6.0 + 20.0 + 6.0, secondSlot.y)
    }

    @Test
    fun `a line is as tall as its tallest child`() {
        val box = padded(FlexBox(), 5.px, 5.px)
        val short = TextArea("short")
        val tall = TextArea("tall")
        val next = TextArea("next")
        box.layout {
            draw(short) { layout { width = 40.px; height = 10.px } }
            draw(tall) { layout { width = 40.px; height = 40.px } }
            draw(next) { layout { width = 400.px; height = 10.px } }
        }

        // The third child is wider than one line, so it wraps below the tallest of the first two.
        val nextSlot = this.slotOf(box, next, 200.0, 200.0)
        assertEquals(5.0, nextSlot.x)
        assertEquals(5.0 + 40.0 + 5.0, nextSlot.y)
    }

    @Test
    fun `a child keeps its own offsets inside its slot`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        val child = TextArea("nudged")
        box.layout {
            draw(child) {
                layout {
                    x = 4.px
                    y = 3.px
                    width = 30.px
                    height = 20.px
                }
            }
        }

        // The flow decides the slot; the child's own x and y only shift it from there.
        val slot = this.slotOf(box, child, 400.0, 200.0)
        assertEquals(10.0, slot.x)
        assertEquals(10.0, slot.y)
    }

    @Test
    fun `a flex box nests inside a div`() {
        val panel = Div()
        val box = padded(FlexBox(), 4.px, 4.px)
        panel.layout {
            draw(box) {
                layout { width = 100.percent }
                box.layout {
                    draw(TextArea("a"))
                    draw(TextArea("b"))
                }
            }
        }

        assertSame(panel, box.parent)
        assertEquals(2, box.children.size)
        assertSame(box, box.children[0].parent)
    }

    @Test
    fun `building a flex box does not need a toolkit`() {
        val box = FlexBox()
        box.layout {
            draw(TextArea("a"))
            draw(Div()) { nested -> nested.layout { draw(Button()) } }
        }

        assertFalse(box.isNodeCreated)
        assertFalse(box.children[0].isNodeCreated)
        assertTrue(box.children[1] is Div)
    }

    @Test
    fun `a flex box cannot be drawn twice`() {
        val box = FlexBox()
        Frame().layout { draw(box) }

        val failure = runCatching { Frame().layout { draw(box) } }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `removing a child removes it from the flow`() {
        val box = FlexBox()
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first)
            draw(second)
        }

        box.remove(first)
        assertEquals(listOf(second), box.children)
    }
}
