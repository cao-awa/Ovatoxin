package com.github.kusa233.ovatoxin.widget.input

import com.github.kusa233.ovatoxin.window.Frame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ComboBoxTest {

    private data class Size(val name: String)

    @Test
    fun `a combo box keeps the items it was created with`() {
        val combo = ComboBox(listOf("Small", "Medium", "Large"))

        assertEquals(listOf("Small", "Medium", "Large"), combo.items)
        assertEquals(listOf("Small", "Medium", "Large"), combo.items())
        assertEquals(3, combo.size)
        assertEquals("Medium", combo.item(1))
    }

    @Test
    fun `a combo box starts empty`() {
        val combo = ComboBox<String>()

        assertTrue(combo.items.isEmpty())
        assertEquals(0, combo.size)
        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
    }

    @Test
    fun `items can be replaced`() {
        val combo = ComboBox(listOf("a"))
        combo.items(listOf("b", "c"))

        assertEquals(listOf("b", "c"), combo.items)
        assertEquals(2, combo.size)
    }

    @Test
    fun `reading an item outside the dropdown fails`() {
        val combo = ComboBox(listOf("only"))

        assertFailsWith<IllegalArgumentException> { combo.item(1) }
        assertFailsWith<IllegalArgumentException> { combo.item(-1) }
    }

    @Test
    fun `a new combo box has no selection`() {
        val combo = ComboBox(listOf("a", "b"))

        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
        assertNull(combo.selectedItem)
        assertNull(combo.selectedText)
    }

    @Test
    fun `selecting an index points at the item and its text`() {
        val combo = ComboBox(listOf("a", "b", "c"))

        combo.selectedIndex = 2

        assertEquals(2, combo.selectedIndex)
        assertEquals("c", combo.selectedItem)
        assertEquals("c", combo.selectedText)
    }

    @Test
    fun `a programmatic selection stays silent`() {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<String>()
        val combo = ComboBox(listOf("a", "b")).onSelect { indices += it }.onSelectItem { items += it }

        combo.selectedIndex = 1
        combo.select(0)
        combo.clearSelection()

        assertTrue(indices.isEmpty(), "the user picked nothing: $indices")
        assertTrue(items.isEmpty(), "the user picked nothing: $items")
    }

    @Test
    fun `a user pick reports the index and the item`() {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<String>()
        val combo = ComboBox(listOf("a", "b")).onSelect { indices += it }.onSelectItem { items += it }

        combo.selectFromUser(1)

        assertEquals(1, combo.selectedIndex)
        assertEquals(listOf(1), indices)
        assertEquals(listOf("b"), items)
    }

    @Test
    fun `registering only one handler leaves the other unset`() {
        val items = mutableListOf<String>()
        val combo = ComboBox(listOf("a", "b")).onSelectItem { items += it }

        combo.selectFromUser(0)

        assertEquals(listOf("a"), items)
    }

    @Test
    fun `selecting outside the items fails`() {
        val combo = ComboBox(listOf("a", "b"))

        assertFailsWith<IllegalArgumentException> { combo.select(2) }
        assertFailsWith<IllegalArgumentException> { combo.select(-2) }
    }

    @Test
    fun `clearing the selection is allowed and stays silent`() {
        val reported = mutableListOf<String>()
        val combo = ComboBox(listOf("a", "b")).onSelectItem { reported += it }
        combo.select(1)

        combo.clearSelection()

        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
        assertNull(combo.selectedItem)
        assertTrue(reported.isEmpty(), reported.toString())
    }

    @Test
    fun `the formatter decides what an item reads as`() {
        val combo = ComboBox(listOf(Size("S"), Size("M"))) { it.name }

        combo.select(1)

        assertEquals("M", combo.selectedText)
        assertEquals(Size("M"), combo.selectedItem, "the item itself, not its text")
    }

    @Test
    fun `the default formatter is the item's toString`() {
        val combo = ComboBox(listOf(1, 2, 3))
        combo.select(2)

        assertEquals("3", combo.selectedText)
    }

    @Test
    fun `a user pick hands over the object itself`() {
        val sizes = listOf(Size("S"), Size("M"))
        val picked = mutableListOf<Size>()
        val combo = ComboBox(sizes) { it.name }.onSelectItem { picked += it }

        combo.selectFromUser(1)

        assertEquals(listOf(Size("M")), picked)
        assertSame(sizes[1], picked.single(), "the very object, not a copy")
    }

    @Test
    fun `the items are live`() {
        val combo = ComboBox(listOf("a"))
        val values = combo.items

        values.add("b")
        values.add("c")

        assertEquals(listOf("a", "b", "c"), combo.items)
        assertEquals(3, combo.size)
        assertEquals("c", combo.item(2))
    }

    @Test
    fun `adding above the selected item moves the selection down with it`() {
        val combo = ComboBox(listOf("a", "b", "c"))
        combo.select(2)

        combo.items.add(0, "z")

        assertEquals(3, combo.selectedIndex, "the selection follows the item it points at")
        assertEquals("c", combo.selectedItem)
    }

    @Test
    fun `adding below the selected item leaves the selection alone`() {
        val combo = ComboBox(listOf("a", "b"))
        combo.select(0)

        combo.items.add("c")

        assertEquals(0, combo.selectedIndex)
        assertEquals("a", combo.selectedItem)
    }

    @Test
    fun `removing the selected item drops the selection`() {
        val combo = ComboBox(listOf("a", "b", "c"))
        combo.select(1)

        combo.items.removeAt(1)

        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
        assertNull(combo.selectedItem)
    }

    @Test
    fun `removing an item above the selection moves it up`() {
        val combo = ComboBox(listOf("a", "b", "c"))
        combo.select(2)

        combo.items.removeAt(0)

        assertEquals(1, combo.selectedIndex)
        assertEquals("c", combo.selectedItem)
    }

    @Test
    fun `removing an item below the selection leaves it alone`() {
        val combo = ComboBox(listOf("a", "b", "c"))
        combo.select(0)

        combo.items.removeAt(2)

        assertEquals(0, combo.selectedIndex)
        assertEquals("a", combo.selectedItem)
    }

    @Test
    fun `replacing an item in place keeps the selection where it is`() {
        val combo = ComboBox(listOf("a", "b"))
        combo.select(1)

        combo.items[1] = "B"

        assertEquals(1, combo.selectedIndex)
        assertEquals("B", combo.selectedItem)
    }

    @Test
    fun `a mutation never reports a pick`() {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<String>()
        val combo = ComboBox(listOf("a", "b")).onSelect { indices += it }.onSelectItem { items += it }
        combo.select(0)

        combo.items.add("c")
        combo.items.add(0, "z")
        combo.items.removeAt(1)
        combo.items[0] = "zz"
        combo.items.clear()

        assertTrue(indices.isEmpty(), "mutating the items is not a user pick: $indices")
        assertTrue(items.isEmpty(), "mutating the items is not a user pick: $items")
    }

    @Test
    fun `clearing the items drops the selection`() {
        val combo = ComboBox(listOf("a", "b"))
        combo.select(1)

        combo.items.clear()

        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
        assertEquals(0, combo.size)
    }

    @Test
    fun `replacing the items keeps a selection that still points at an item`() {
        val combo = ComboBox(listOf("a", "b", "c"))
        combo.select(1)

        combo.items(listOf("x", "b", "y"))

        assertEquals(1, combo.selectedIndex)
        assertEquals("b", combo.selectedItem)
    }

    @Test
    fun `replacing the items drops a selection that no longer points at an item`() {
        val combo = ComboBox(listOf("a", "b"))
        combo.select(1)

        combo.items(listOf("x", "y"))

        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
    }

    @Test
    fun `a reset drops the selection and the handlers but keeps the items`() {
        val reported = mutableListOf<String>()
        val combo = ComboBox(listOf("a"))
        combo.items.add("b")
        combo.select(1)
        combo.onSelectItem { reported += it }
        combo.prompt = "Pick one"
        combo.enabled = false

        combo.reset()

        assertEquals(listOf("a", "b"), combo.items, "the live items are the caller's, so they stay")
        assertEquals(ComboBox.NO_SELECTION, combo.selectedIndex)
        assertEquals("", combo.prompt)
        assertTrue(combo.enabled)
        assertEquals(ComboBox.DEFAULT_BACKGROUND, combo.styleSpec.background.color)
        assertEquals(ComboBox.DEFAULT_FOREGROUND, combo.styleSpec.foreground)

        combo.selectFromUser(0)
        assertTrue(reported.isEmpty(), "a reset drops the handler: $reported")
    }

    @Test
    fun `the items a combo box was created with can be put back`() {
        val combo = ComboBox(listOf("a", "b"))
        combo.items.add("c")

        combo.items(combo.initialItems)

        assertEquals(listOf("a", "b"), combo.items)
    }

    @Test
    fun `a prompt is just text on the field`() {
        val combo = ComboBox<String>()
        combo.prompt = "Choose a size"

        assertEquals("Choose a size", combo.prompt)
    }

    @Test
    fun `a combo box starts on a white surface with black text`() {
        val combo = ComboBox(listOf("a"))

        assertEquals(ComboBox.DEFAULT_BACKGROUND, combo.styleSpec.background.color)
        assertEquals(ComboBox.DEFAULT_FOREGROUND, combo.styleSpec.foreground)
    }

    @Test
    fun `a style block colours the field`() {
        val combo = ComboBox(listOf("a"))
        combo.applyStyle { background { color = javafx.scene.paint.Color.web("#102030") } }

        assertEquals(
            javafx.scene.paint.Color.web("#102030"),
            combo.styleSpec.background.color,
        )
    }

    @Test
    fun `a combo box builds no node until it is shown`() {
        val combo = ComboBox(listOf("a", "b"))

        assertFalse(combo.isNodeCreated, "items alone must not build a JavaFX control")
        assertEquals("ovatoxin-combo", ComboBox.STYLE_CLASS)
    }

    @Test
    fun `an empty combo box is allowed inside a frame`() {
        val frame = Frame()
        val combo = ComboBox<String>()
        frame.layout {
            draw(combo) { it.prompt = "Nothing yet" }
        }

        assertSame(combo, frame.children[0])
        assertEquals("Nothing yet", combo.prompt)
    }

    @Test
    fun `onSelect returns the combo box so calls chain`() {
        val combo = ComboBox(listOf("a"))
        assertSame(combo, combo.onSelect { })
        assertSame(combo, combo.onSelectItem { })
    }
}
