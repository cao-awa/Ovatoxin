package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.text.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [Card].
 *
 * The card is a [FlexBox] with a seeded look, so these tests check the two things that make it a
 * widget rather than a helper: the constructed values are restored by [Card.reset], and the
 * [Card.text] / [Card.button] configuration methods really attach their children to the card.
 */
class CardTest {

    @Test
    fun `a card starts with its documented look`() {
        val card = Card(h = 180.px, w = 50.percent)

        Frame().layout { draw(card) {} }

        assertEquals(50.percent, card.layoutSpec.width)
        assertEquals(180.px, card.layoutSpec.height)
        assertEquals(18.px, card.layoutSpec.hPadding)
        assertEquals(18.px, card.layoutSpec.vPadding)
        assertEquals(Card.DEFAULT_FILL, card.styleSpec.background.color)
        assertEquals(16.px, card.styleSpec.corner)
        assertEquals(14.px, card.styleSpec.fontSize)
        assertEquals(1.px, card.styleSpec.border.size)
        assertEquals(Card.DEFAULT_BORDER, card.styleSpec.border.color)
        assertEquals(8.px, card.gap)
        assertEquals(Align.START, card.align)
    }

    @Test
    fun `constructor values replace the defaults and survive a reset`() {
        val card = Card(
            h = 120.px,
            w = 80.percent,
            fill = Color.web("#101b33"),
            borderColor = null,
            pad = 10.0,
            gap = 4.0,
            corner = 8.px,
        )

        Frame().layout { draw(card) {} }

        assertEquals(80.percent, card.layoutSpec.width)
        assertEquals(10.px, card.layoutSpec.hPadding)
        assertEquals(Color.web("#101b33"), card.styleSpec.background.color)
        assertEquals(8.px, card.styleSpec.corner)
        assertEquals(0.px, card.styleSpec.border.size)
        assertEquals(4.px, card.gap)

        // A redraw resets the component first; the card has to put its own values back.
        card.applyStyle {
            background { color = Color.RED }
            corner = 1.px
        }
        card.applyLayout { height = 20.px }
        card.reset()

        assertEquals(120.px, card.layoutSpec.height)
        assertEquals(80.percent, card.layoutSpec.width)
        assertEquals(Color.web("#101b33"), card.styleSpec.background.color)
        assertEquals(8.px, card.styleSpec.corner)
        assertEquals(4.px, card.gap)
    }

    @Test
    fun `the card factory draws a card with a body`() {
        val frame = Frame()
        var built: Card? = null

        frame.layout {
            built = card(h = 140.px, w = 70.percent, gap = 6.0) {
                draw(TextArea("inside"))
            }
        }

        val card = built!!
        assertSame(card, frame.children.single())
        assertEquals(card, card.children.single().parent)
        assertEquals(1, card.children.size)
        assertEquals(70.percent, card.layoutSpec.width)
        assertEquals(6.px, card.gap)
    }

    @Test
    fun `text and button configure the card itself`() {
        val card = Card(200.px)

        Frame().layout {
            draw(card) { current ->
                current.text(
                    content = "标题",
                    h = 26.px,
                    size = 18.0,
                    color = Color.web("#162033"),
                    bold = true,
                )
                current.button("开始", w = 96.px, h = 36.px) {
                    // The handler receives the button, not the card.
                    text("clicked")
                }
            }
        }

        assertEquals(2, card.children.size)

        val title = card.children[0] as TextArea
        assertEquals("标题", title.text())
        assertEquals(26.px, title.layoutSpec.height)
        assertEquals(18.px, title.styleSpec.fontSize)
        assertEquals(Color.web("#162033"), title.styleSpec.foreground)
        assertTrue(title.bold)
        assertEquals(Color.TRANSPARENT, title.styleSpec.background.color)

        val action = card.children[1] as Button
        assertEquals("开始", action.text())
        assertEquals(96.px, action.layoutSpec.width)
        assertEquals(36.px, action.layoutSpec.height)
        assertEquals(Card.DEFAULT_BUTTON_FILL, action.styleSpec.background.color)
        assertEquals(Card.DEFAULT_BUTTON_TEXT, action.styleSpec.foreground)
        assertTrue(action.hasOnClick)
    }

    @Test
    fun `the library text and button helpers can be used in a card body`() {
        val frame = Frame()

        frame.layout {
            card(h = 150.px) {
                text("说明", h = 24.px)
                button("确定", w = 72.px)
            }
        }

        val card = frame.children.single() as Card
        assertEquals(2, card.children.size)
        assertTrue(card.children[0] is TextArea)
        assertTrue(card.children[1] is Button)
    }

    @Test
    fun `a card body keeps nested layouts`() {
        val frame = Frame()

        frame.layout {
            card(h = 260.px) {
                draw(FlexBox()) { row ->
                    layout {
                        width = 100.percent
                        height = 32.px
                    }
                    row.gap = 8.px
                    row.layout {
                        text("left", w = 100.px)
                        text("right", w = 100.px)
                    }
                }
            }
        }

        val card = frame.children.single() as Card
        val row = card.children.single() as FlexBox
        assertEquals(2, row.children.size)
    }
}
