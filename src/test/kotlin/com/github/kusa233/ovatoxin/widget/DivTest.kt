package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DivTest {

    @Test
    fun `a div nests its own children`() {
        val frame = Frame()
        val panel = Div()
        frame.layout {
            draw(panel) {
                // Configures the panel itself.
                layout { width = 80.percent }
                // Declares what lives inside the panel.
                panel.layout {
                    draw(TextArea())
                }
            }
        }

        assertEquals(1, frame.children.size)
        assertEquals(1, panel.children.size)
        assertSame(panel, panel.children[0].parent)
        assertSame(frame, panel.parent)
    }

    @Test
    fun `nested children resolve percentages against the div`() {
        val frame = Frame()
        val panel = Div()
        val text = TextArea()
        frame.layout {
            draw(panel) {
                layout {
                    x = 10.percent
                    width = 80.percent
                }
                panel.layout {
                    draw(text) {
                        layout {
                            x = 50.percent
                            width = 50.percent
                        }
                    }
                }
            }
        }

        // The child's own spec is relative to the panel, not to the frame.
        assertEquals(50.percent, text.layoutSpec.x)
        assertEquals(50.percent, text.layoutSpec.width)
        assertEquals(80.percent, panel.layoutSpec.width)
        assertSame(panel, text.parent)
    }

    @Test
    fun `divs nest arbitrarily deep`() {
        val outer = Div()
        val inner = Div()
        val text = TextArea()

        outer.layout {
            draw(inner) {
                layout { width = 100.percent }
                inner.layout {
                    draw(text) {
                        layout { x = 25.percent }
                    }
                }
            }
        }

        assertSame(outer, inner.parent)
        assertSame(inner, text.parent)
        assertEquals(1, outer.children.size)
        assertEquals(1, inner.children.size)
        assertEquals(25.percent, text.layoutSpec.x)
    }

    @Test
    fun `building a div tree does not need a toolkit`() {
        val root = Div()
        root.layout {
            draw(TextArea())
            draw(Div()) { nested ->
                nested.layout { draw(Button()) }
            }
        }

        assertFalse(root.isNodeCreated)
        assertFalse(root.children[0].isNodeCreated)
        assertFalse(root.children[1].isNodeCreated)
        assertTrue(root.children[1] is Div)
    }

    @Test
    fun `remove works on nested children`() {
        val root = Div()
        val text = TextArea()
        root.layout { draw(text) }

        root.remove(text)
        assertEquals(0, root.children.size)
        assertNull(text.parent)
    }

    @Test
    fun `clip is on by default and can be turned off`() {
        val div = Div()
        // Toggling the flag needs no built node, so it stays usable before rendering.
        assertEquals(true, div.clip)
        div.clip = false
        assertEquals(false, div.clip)
        div.clip = true
        assertEquals(true, div.clip)
    }

    @Test
    fun `a div cannot be drawn twice`() {
        val div = Div()
        Frame().draw(div)

        val failure = runCatching { Frame().draw(div) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}
