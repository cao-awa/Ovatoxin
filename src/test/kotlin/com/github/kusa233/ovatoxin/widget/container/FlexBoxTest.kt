package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.em
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.text.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
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
    fun `a partial flow pass keeps a child below the siblings before it`() {
        val box = FlexBox()
        val first = TextArea("first")
        val second = TextArea("second")
        box.layout {
            draw(first) { layout { width = 100.percent; height = 20.px } }
            draw(second) { layout { width = 100.percent; height = 24.px } }
        }
        val content = this.boxOf(box, 200.0, 100.0)

        // A state-bound child is repainted on its own, but the flow still needs the whole child
        // list: the first child occupies the first line, so the second one starts below it.
        box.beginPass(content, listOf(second))
        val slot = box.slotFor(second, content)

        assertEquals(0.0, slot.x)
        assertEquals(20.0, slot.y)
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
    fun `children flow to the right with the gap between them`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        box.gap = 10.px
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
    fun `a fresh box keeps no gap`() {
        val box = FlexBox()
        box.layout {
            draw(TextArea("one")) { layout { width = 50.px; height = 20.px } }
            draw(TextArea("two")) { layout { width = 50.px; height = 20.px } }
        }

        assertEquals(Length.ZERO, box.gap)
        assertEquals(Justify.START, box.justify)
        assertEquals(Align.START, box.align)

        // With nothing between them the second child starts where the first one ends.
        val second = box.children[1]
        assertEquals(50.0, this.slotOf(box, second, 400.0, 200.0).x)
    }

    @Test
    fun `the gap is added between the children only`() {
        val box = FlexBox()
        box.gap = 8.px
        val first = TextArea("one")
        val second = TextArea("two")
        val third = TextArea("three")
        box.layout {
            draw(first) { layout { width = 20.px; height = 10.px } }
            draw(second) { layout { width = 20.px; height = 10.px } }
            draw(third) { layout { width = 20.px; height = 10.px } }
        }

        // Three widths and two gaps, and the box still starts at its own corner: the gap is space
        // *between* children, so it does not push the first one inwards the way padding does.
        assertEquals(0.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(28.0, this.slotOf(box, second, 200.0, 100.0).x)
        assertEquals(56.0, this.slotOf(box, third, 200.0, 100.0).x)
    }

    @Test
    fun `a percentage gap resolves against the content box`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        box.gap = 10.percent
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 10.px } }
            draw(second) { layout { width = 40.px; height = 10.px } }
        }

        // Ten percent of the 180px content box is 18px, measured from the inset corner.
        assertEquals(10.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(68.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `an em gap follows the font size of the box`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        box.gap = 1.em
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 10.px } }
            draw(second) { layout { width = 40.px; height = 10.px } }
        }

        // A box without text of its own has no font size, so one em is the 14px default: the
        // second child starts after 40px of child and 14px of gap.
        assertEquals(10.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(64.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `an em gap inherits the font size of the surface it sits in`() {
        val frame = Frame()
        val box = FlexBox()
        val first = TextArea("one")
        val second = TextArea("two")
        frame.layout {
            draw(Div()) { panel ->
                style { fontSize = 20.px }
                panel.layout {
                    draw(box) {
                        layout { width = 200.px }
                        box.gap = 1.em
                        box.layout {
                            draw(first) { layout { width = 40.px; height = 10.px } }
                            draw(second) { layout { width = 40.px; height = 10.px } }
                        }
                    }
                }
            }
        }

        // The box carries no font of its own, so it inherits the 20px of the panel it was drawn
        // into — one em is 20px, not the 14px default.
        assertEquals(20.0, box.resolvedFontSize)
        assertEquals(60.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `a font size of its own wins over the inherited one`() {
        val frame = Frame()
        val box = FlexBox()
        frame.layout {
            draw(Div()) { panel ->
                style { fontSize = 10.px }
                panel.layout {
                    draw(box) {
                        style { fontSize = 30.px }
                        box.gap = 1.em
                        box.layout {
                            draw(TextArea("one")) { layout { width = 40.px; height = 10.px } }
                        }
                    }
                }
            }
        }

        assertEquals(30.0, box.resolvedFontSize)
        assertEquals(30.0, box.gap.resolve(200.0, box.resolvedFontSize))
    }

    @Test
    fun `a percentage font size is taken of the box height`() {
        val box = FlexBox()
        box.applyStyle { fontSize = 150.percent }

        // 150% of a 50px tall box is 75px of text.
        assertEquals(75.0, box.fontSizeForHeight(50.0, 10.0))
        // Without a box height there is nothing to take a fraction of, so the inherited size stays.
        assertEquals(10.0, box.fontSizeForHeight(null, 10.0))
        // An auto-height box does not know its height, so it falls back to the inherited default.
        assertEquals(Length.DEFAULT_FONT_SIZE, box.resolvedFontSize)
    }

    @Test
    fun `a cleared font size falls back to the inherited one`() {
        val frame = Frame()
        val panel = Div()
        frame.layout {
            draw(panel) {
                layout { hPadding = 1.em }
                style { fontSize = 18.px }
            }
        }
        assertEquals(18.0, panel.resolvedFontSize)

        panel.applyStyle { fontSize = null }
        assertEquals(Length.DEFAULT_FONT_SIZE, panel.resolvedFontSize)
    }

    @Test
    fun `an em padding resolves against the font size of the surface`() {
        val div = padded(Div(), 1.em, 2.em)

        // No text around it and no size of its own, so the 14px default applies: 14px left and
        // right, 28px top and bottom.
        val content = div.contentBox(200.0, 100.0)
        assertEquals(14.0, content.offsetX)
        assertEquals(28.0, content.offsetY)
        assertEquals(172.0, content.width)
        assertEquals(44.0, content.height)
    }

    @Test
    fun `an em padding follows the size the surface was given`() {
        val div = padded(Div(), 1.em, 1.em)
        div.applyStyle { fontSize = 20.px }

        val content = div.contentBox(200.0, 100.0)
        assertEquals(20.0, content.offsetX)
        assertEquals(20.0, content.offsetY)
        assertEquals(160.0, content.width)
        assertEquals(60.0, content.height)
    }

    @Test
    fun `a child sized in em measures against its own font size`() {
        val box = FlexBox()
        val child = TextArea("one")
        box.layout {
            draw(child) {
                layout { width = 10.em; height = 2.em }
                // A text area renders text of its own, so the size is stated here — either way of
                // writing it lands in the same style, and a draw block always starts from it again.
                child.fontSize = 20.0
            }
        }

        // 10 x 20px wide and 2 x 20px tall, resolved from the font of the child, not of the box.
        assertEquals(200.0, child.layoutSpec.width!!.resolve(400.0, child.resolvedFontSize))
        assertEquals(40.0, child.layoutSpec.height!!.resolve(200.0, child.resolvedFontSize))
        assertEquals(0.0, this.slotOf(box, child, 400.0, 200.0).x)
    }

    @Test
    fun `a child that does not fit starts a new line`() {
        val box = padded(FlexBox(), 10.px, 6.px)
        box.gap = 6.px
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 100.px; height = 20.px } }
            draw(second) { layout { width = 100.px; height = 20.px } }
        }

        // 10 + 100 + 6 + 100 exceeds the 200px content box, so the second child wraps below the
        // first line: the inset, that line, and one gap.
        val secondSlot = this.slotOf(box, second, 220.0, 200.0)
        assertEquals(10.0, secondSlot.x)
        assertEquals(6.0 + 20.0 + 6.0, secondSlot.y)
    }

    @Test
    fun `justify leaves the start of a line alone`() {
        val box = FlexBox()
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        // START is the default, and it is what a line does when it has no room to share out.
        assertEquals(0.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(40.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `justify centre splits the leftover room in two`() {
        val box = FlexBox()
        box.justify = Justify.CENTER
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        // 200 - 80 leaves 120 to share, so the pair starts 60px in and keeps touching.
        assertEquals(60.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(100.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `justify end pushes the line to the right edge`() {
        val box = FlexBox()
        box.justify = Justify.END
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        assertEquals(120.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(160.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `justify space between moves the room into the gaps`() {
        val box = FlexBox()
        box.justify = Justify.SPACE_BETWEEN
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        // First against the start, last against the end, all 120px of room between them.
        assertEquals(0.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(160.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `justify space between keeps the gap it was given`() {
        val box = FlexBox()
        box.gap = 10.px
        box.justify = Justify.SPACE_BETWEEN
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        // The gap is part of what the line uses up, so only 110px are left to share out.
        assertEquals(0.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(160.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `justify space around leaves half a share at each end`() {
        val box = FlexBox()
        box.justify = Justify.SPACE_AROUND
        val first = TextArea("one")
        val second = TextArea("two")
        box.layout {
            draw(first) { layout { width = 40.px; height = 20.px } }
            draw(second) { layout { width = 40.px; height = 20.px } }
        }

        // A share of 60px per child: 30 before the first, 60 between, 30 after the last.
        assertEquals(30.0, this.slotOf(box, first, 200.0, 100.0).x)
        assertEquals(130.0, this.slotOf(box, second, 200.0, 100.0).x)
    }

    @Test
    fun `a lone child is packed by between and centred by around`() {
        val between = FlexBox()
        between.justify = Justify.SPACE_BETWEEN
        val packed = TextArea("one")
        between.layout { draw(packed) { layout { width = 40.px; height = 20.px } } }
        assertEquals(0.0, this.slotOf(between, packed, 200.0, 100.0).x)

        val around = FlexBox()
        around.justify = Justify.SPACE_AROUND
        val centred = TextArea("two")
        around.layout { draw(centred) { layout { width = 40.px; height = 20.px } } }
        assertEquals(80.0, this.slotOf(around, centred, 200.0, 100.0).x)
    }

    @Test
    fun `justify shares out nothing when a line overflows`() {
        val box = FlexBox()
        box.justify = Justify.CENTER
        val wide = TextArea("wide")
        box.layout { draw(wide) { layout { width = 300.px; height = 20.px } } }

        // A child is never squeezed to fit, so this line is wider than the box and has nothing left
        // over: it packs from the start, exactly as if it had been asked to.
        assertEquals(0.0, this.slotOf(box, wide, 200.0, 100.0).x)
    }

    @Test
    fun `justify does not move a line past the inset`() {
        val box = padded(FlexBox(), 10.px, 10.px)
        box.justify = Justify.END
        val first = TextArea("one")
        box.layout { draw(first) { layout { width = 40.px; height = 20.px } } }

        // 10 + 140 + 40 + 10: the line ends against the inset, not against the box.
        assertEquals(150.0, this.slotOf(box, first, 200.0, 100.0).x)
    }

    @Test
    fun `align packs a child against the top of its line`() {
        val box = FlexBox()
        val short = TextArea("short")
        val tall = TextArea("tall")
        box.layout {
            draw(short) { layout { width = 40.px; height = 20.px } }
            draw(tall) { layout { width = 40.px; height = 60.px } }
        }

        // START is the default: the shorter child keeps the top of the 60px line.
        assertEquals(0.0, this.slotOf(box, short, 200.0, 100.0).y)
        assertEquals(0.0, this.slotOf(box, tall, 200.0, 100.0).y)
    }

    @Test
    fun `align centre centres a child in its line`() {
        val box = FlexBox()
        box.align = Align.CENTER
        val short = TextArea("short")
        val tall = TextArea("tall")
        box.layout {
            draw(short) { layout { width = 40.px; height = 20.px } }
            draw(tall) { layout { width = 40.px; height = 60.px } }
        }

        // The line is 60 tall, so the 20px child gets 20 above and 20 below it.
        assertEquals(20.0, this.slotOf(box, short, 200.0, 100.0).y)
        assertEquals(0.0, this.slotOf(box, tall, 200.0, 100.0).y)
    }

    @Test
    fun `align end packs a child against the bottom of its line`() {
        val box = FlexBox()
        box.align = Align.END
        val short = TextArea("short")
        val tall = TextArea("tall")
        box.layout {
            draw(short) { layout { width = 40.px; height = 20.px } }
            draw(tall) { layout { width = 40.px; height = 60.px } }
        }

        assertEquals(40.0, this.slotOf(box, short, 200.0, 100.0).y)
        assertEquals(0.0, this.slotOf(box, tall, 200.0, 100.0).y)
    }

    @Test
    fun `align leaves the height of a line alone`() {
        val box = FlexBox()
        box.align = Align.END
        val tall = TextArea("tall")
        val next = TextArea("next")
        box.layout {
            draw(tall) { layout { width = 40.px; height = 60.px } }
            draw(next) { layout { width = 400.px; height = 10.px } }
        }

        // The second child cannot fit beside the first, so it opens a line below it — the first
        // line is still as tall as its tallest child, whichever edge the children lean on.
        assertEquals(60.0, this.slotOf(box, next, 200.0, 200.0).y)
    }

    @Test
    fun `a line is as tall as its tallest child`() {
        val box = padded(FlexBox(), 5.px, 5.px)
        box.gap = 5.px
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
    fun `flexFlow draws a configured flow row`() {
        val frame = Frame()
        var built: FlexBox? = null

        frame.layout {
            built = flexFlow(
                rowHeight = 80.px,
                justify = Justify.SPACE_BETWEEN,
                align = Align.CENTER,
                gap = 6.0,
                pad = 10.0,
                fill = Color.web("#f8fafc"),
                borderColor = Color.web("#e4e9f2"),
            ) {
                draw(TextArea("one")) {
                    layout {
                        width = 40.px
                        height = 20.px
                    }
                }
            }
        }

        val flow = built!!
        assertSame(flow, frame.children.single())
        assertEquals(80.px, flow.layoutSpec.height)
        assertEquals(10.px, flow.layoutSpec.hPadding)
        assertEquals(10.px, flow.layoutSpec.vPadding)
        assertEquals(6.px, flow.gap)
        assertEquals(Justify.SPACE_BETWEEN, flow.justify)
        assertEquals(Align.CENTER, flow.align)
        assertEquals(Color.web("#f8fafc"), flow.styleSpec.background.color)
        assertEquals(1.px, flow.styleSpec.border.size)
        assertEquals(Color.web("#e4e9f2"), flow.styleSpec.border.color)
        assertEquals(12.px, flow.styleSpec.corner)
        assertEquals(1, flow.children.size)
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
