package com.github.kusa233.ovatoxin.dsl

import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the `layout { }` / `style { }` template presets that live outside any component.
 *
 * A preset is an ordinary [com.github.kusa233.ovatoxin.layout.Layout] /
 * [com.github.kusa233.ovatoxin.style.Style] value, so every assertion here is about the values a
 * component ends up holding. Nothing needs a node or a toolkit: the presets are copied into the
 * specs, and the specs are read back.
 */
class PresetTest {

    @Test
    fun `a layout preset holds what the block set and defaults elsewhere`() {
        val preset = layout {
            width = 20.percent
            height = 100.percent
        }
        assertEquals(20.percent, preset.width)
        assertEquals(100.percent, preset.height)
        assertEquals(Length.ZERO, preset.x)
        assertEquals(Length.ZERO, preset.y)
        assertEquals(Length.ZERO, preset.hPadding)
        assertEquals(Length.ZERO, preset.vPadding)
    }

    @Test
    fun `applying a layout preset copies it into the component`() {
        val preset = layout {
            width = 20.percent
            height = 100.percent
        }
        val frame = Frame()
        lateinit var div: Div
        frame.layout { div = draw(Div()) { layout(preset) } }

        assertEquals(20.percent, div.layoutSpec.width)
        assertEquals(100.percent, div.layoutSpec.height)
    }

    @Test
    fun `the tuning block overrides the preset`() {
        val preset = layout {
            x = 10.percent
            width = 50.percent
        }
        val frame = Frame()
        lateinit var div: Div
        frame.layout { div = draw(Div()) { layout(preset) { y = 20.percent } } }

        assertEquals(10.percent, div.layoutSpec.x)
        assertEquals(20.percent, div.layoutSpec.y)
        assertEquals(50.percent, div.layoutSpec.width)
    }

    @Test
    fun `applying a preset replaces what the component held before`() {
        val preset = layout { width = 25.percent }
        val frame = Frame()
        lateinit var div: Div
        frame.layout {
            div = draw(Div()) {
                layout {
                    x = 40.percent
                    height = 10.px
                }
                layout(preset)
            }
        }

        assertEquals(Length.ZERO, div.layoutSpec.x)
        assertNull(div.layoutSpec.height)
        assertEquals(25.percent, div.layoutSpec.width)
    }

    @Test
    fun `a preset is a snapshot, so later edits do not move the component`() {
        val preset = layout { width = 20.percent }
        val frame = Frame()
        lateinit var div: Div
        frame.layout { div = draw(Div()) { layout(preset) } }

        preset.width = 40.percent
        assertEquals(20.percent, div.layoutSpec.width)
    }

    @Test
    fun `a style preset carries appearance and a tuning block can override it`() {
        val preset = style {
            background { color = Color.web("#4c8bf5") }
            corner = 30.percent
        }
        val frame = Frame()
        lateinit var button: Button
        frame.layout {
            button = draw(Button()) {
                style(preset) { foreground = Color.web("#000000") }
            }
        }

        assertEquals(30.percent, button.styleSpec.corner)
        assertEquals(Color.web("#000000"), button.styleSpec.foreground)
        assertEquals(Color.web("#4c8bf5"), button.styleSpec.background.color)
    }

    @Test
    fun `two components sharing a style template keep separate fills`() {
        val preset = style {
            background { color = Color.web("#4c8bf5") }
        }
        val frame = Frame()
        lateinit var first: Div
        lateinit var second: Div
        frame.layout {
            first = draw(Div()) { style(preset) }
            second = draw(Div()) { style(preset) }
        }

        first.styleSpec.background.color = Color.web("#000000")
        assertEquals(Color.web("#000000"), first.styleSpec.background.color)
        assertEquals(Color.web("#4c8bf5"), second.styleSpec.background.color)
        assertEquals(Color.web("#4c8bf5"), preset.background.color)
    }

    @Test
    fun `a component applies presets at runtime`() {
        val div = Div()
        div.applyLayout(
            layout {
                width = 50.percent
                height = 10.px
            }
        )
        assertEquals(50.percent, div.layoutSpec.width)
        assertEquals(10.px, div.layoutSpec.height)

        div.applyStyle(style { corner = 10.px })
        assertEquals(10.px, div.styleSpec.corner)
    }
}
