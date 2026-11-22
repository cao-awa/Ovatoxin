package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.style.cssColor
import javafx.scene.paint.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ListViewTest {

    @Test
    fun `a list keeps the rows it was created with`() {
        val list = ListView(listOf("One", "Two", "Three"))

        assertEquals(listOf("One", "Two", "Three"), list.items)
        assertEquals(3, list.size)
        assertEquals("Two", list.item(1))
        assertEquals("Two", list.items()[1])
    }

    @Test
    fun `items can be replaced`() {
        val list = ListView(listOf("a"))
        list.items(listOf("b", "c"))

        assertEquals(listOf("b", "c"), list.items())
        assertEquals(2, list.size)
    }

    @Test
    fun `reading a row outside the list fails`() {
        val list = ListView(listOf("only"))

        assertFailsWith<IllegalArgumentException> { list.item(1) }
        assertFailsWith<IllegalArgumentException> { list.item(-1) }
    }

    @Test
    fun `a new list has no selection`() {
        val list = ListView(listOf("a", "b"))

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
    }

    @Test
    fun `select sets the row without running the handler`() {
        val reported = mutableListOf<Int>()
        val list = ListView(listOf("a", "b", "c")).onSelect { reported += it }

        list.selectedIndex = 2

        assertEquals(2, list.selectedIndex)
        assertEquals("c", list.selectedItem)
        assertTrue(reported.isEmpty(), "a programmatic selection stays silent: $reported")
    }

    @Test
    fun `a click is what runs the handler`() {
        val reported = mutableListOf<Int>()
        val list = ListView(listOf("a", "b")).onSelect { reported += it }

        // The row handler is the only thing that reaches `selectFromUser`.
        list.selectFromUser(1)

        assertEquals(1, list.selectedIndex)
        assertEquals(listOf(1), reported)
    }

    @Test
    fun `a click reports the item, not the index`() {
        val reported = mutableListOf<String>()
        val list = ListView(listOf("a", "b")).onSelectItem { reported += it }

        list.selectFromUser(1)

        assertEquals(1, list.selectedIndex)
        assertEquals(listOf("b"), reported)
    }

    @Test
    fun `both handlers run on the same click when both are registered`() {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<String>()
        val list = ListView(listOf("a", "b"))
            .onSelect { indices += it }
            .onSelectItem { items += it }

        list.selectFromUser(0)

        assertEquals(listOf(0), indices)
        assertEquals(listOf("a"), items)
    }

    @Test
    fun `registering only one handler leaves the other unset`() {
        val items = mutableListOf<String>()
        // No onSelect at all: the click must not need one to work.
        val list = ListView(listOf("a", "b")).onSelectItem { items += it }

        list.selectFromUser(1)

        assertEquals(listOf("b"), items)
    }

    @Test
    fun `a programmatic selection never runs the item handler`() {
        val reported = mutableListOf<String>()
        val list = ListView(listOf("a", "b")).onSelectItem { reported += it }

        list.selectedIndex = 1
        list.select(0)
        list.clearSelection()

        assertTrue(reported.isEmpty(), "the user selected nothing: $reported")
    }

    @Test
    fun `the item handler is dropped by a reset`() {
        val reported = mutableListOf<String>()
        val list = ListView(listOf("a", "b")).onSelectItem { reported += it }

        list.reset()
        list.selectFromUser(0)

        assertTrue(reported.isEmpty(), "a reset drops the handler, as onSelect does: $reported")
    }

    @Test
    fun `a list of a custom type hands out the object itself`() {
        data class User(val name: String, val mail: String)
        val users = listOf(User("Ann", "ann@x"), User("Bob", "bob@x"))

        val reported = mutableListOf<User>()
        val list = ListView(users) { it.name }.onSelectItem { reported += it }

        list.selectFromUser(1)

        assertEquals(User("Bob", "bob@x"), list.selectedItem)
        assertEquals(listOf(users[1]), reported, "the same instance, not a copy or a string")
        assertTrue(reported[0] === users[1])
    }

    @Test
    fun `a custom renderer decides the row text but not the item`() {
        data class User(val name: String, val mail: String)
        val user = User("Ann", "ann@x")

        val list = ListView(listOf(user)) { it.name }

        assertEquals(user, list.item(0), "the item is untouched by the renderer")
        assertEquals("ann@x", list.item(0).mail)

        list.select(0)
        assertEquals("Ann", list.selectedText)
        assertEquals(user, list.selectedItem, "the text is a view of the item, not a replacement for it")
    }

    @Test
    fun `the default renderer is the item's toString`() {
        val list = ListView(listOf(1, 2, 3))

        assertEquals(3, list.size)
        assertEquals(2, list.item(1))
        // No renderer given, so a row would print "2" for the Integer 2.
        assertEquals(listOf(1, 2, 3), list.items)
    }

    @Test
    fun `selection and removal work on a list of a custom type`() {
        data class User(val name: String)
        val ann = User("Ann")
        val bob = User("Bob")
        val list = ListView(listOf(ann, bob)) { it.name }
        list.select(1)

        list.items.add(0, User("Zoe"))

        assertEquals(bob, list.selectedItem, "the selection follows the object, not the index")
        assertEquals(2, list.selectedIndex)

        assertTrue(list.remove(bob))
        assertNull(list.selectedItem, "removing the selected item drops the selection")
        assertEquals(listOf(User("Zoe"), ann), list.items)
    }

    @Test
    fun `clearing the selection stays silent`() {
        val reported = mutableListOf<Int>()
        val list = ListView(listOf("a")).onSelect { reported += it }
        list.select(0)

        list.clearSelection()

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
        assertTrue(reported.isEmpty(), reported.toString())
    }

    @Test
    fun `selecting a row outside the list fails`() {
        val list = ListView(listOf("a"))

        assertFailsWith<IllegalArgumentException> { list.select(1) }
        assertFailsWith<IllegalArgumentException> { list.select(-2) }
    }

    @Test
    fun `replacing the rows drops a selection that no longer points at a row`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.items(listOf("only"))

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
    }

    @Test
    fun `replacing the rows keeps a selection that still points at a row`() {
        val list = ListView(listOf("a", "b"))
        list.select(0)

        list.items(listOf("a", "b", "c"))

        assertEquals(0, list.selectedIndex)
        assertEquals("a", list.selectedItem)
    }

    @Test
    fun `a list starts on a white surface with black text`() {
        val list = ListView(listOf("a"))

        assertEquals(Color.web("#ffffff"), list.styleSpec.background.color)
        assertEquals(Color.web("#000000"), list.styleSpec.foreground)
        assertEquals(Color.web("#eeeeee"), list.selectedBackground)
        assertEquals(Style.DEFAULT_CORNER, list.styleSpec.corner)
    }

    @Test
    fun `the selected row is painted light grey and the rest transparent`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        val selected = list.rowCss(selected = true)
        val plain = list.rowCss(selected = false)

        assertTrue(selected.contains("${cssColor(Color.web("#eeeeee"))}"), selected)
        assertTrue(selected.contains("${cssColor(Color.web("#000000"))}"), selected)
        assertTrue(plain.contains("transparent"), plain)
        assertTrue(plain.contains("${cssColor(Color.web("#000000"))}"), plain)
    }

    @Test
    fun `the selected colour can be overridden`() {
        val list = ListView(listOf("a"))
        list.selectedBackground = Color.web("#cccccc")

        assertTrue(list.rowCss(selected = true).contains("${cssColor(Color.web("#cccccc"))}"))
    }

    @Test
    fun `a style block colours the list and its rows`() {
        val list = ListView(listOf("a"))
        list.applyStyle {
            background { color = Color.web("#102030") }
            foreground = Color.web("#f0f0f0")
        }

        assertEquals(Color.web("#102030"), list.styleSpec.background.color)
        assertTrue(list.rowCss(selected = false).contains("${cssColor(Color.web("#f0f0f0"))}"))
    }

    @Test
    fun `building a list does not need a toolkit`() {
        val list = ListView(listOf("a", "b"))

        assertFalse(list.isNodeCreated)
        assertEquals(2, list.size)
    }

    @Test
    fun `a row can be appended`() {
        val list = ListView(listOf("a"))
        list.add("b")

        assertEquals(listOf("a", "b"), list.items)
        assertEquals("b", list.item(1))
        assertEquals(2, list.size)
    }

    @Test
    fun `the rows grow straight through the live list`() {
        val list = ListView(listOf("a"))
        val rows = list.items

        rows.add("b")
        rows.add("c")

        assertEquals(listOf("a", "b", "c"), list.items)
        assertEquals(3, list.size)
        assertEquals("c", list.item(2))
    }

    @Test
    fun `the live list is the storage, not a copy of it`() {
        val list = ListView(listOf("a"))
        val rows = list.items

        rows.add("b")

        // Reading back through either path has to see the same rows.
        assertEquals(listOf("a", "b"), list.items())
        assertEquals(listOf("a", "b"), rows.toList())
        assertEquals(2, list.size)
    }

    @Test
    fun `the rows shrink straight through the live list`() {
        val list = ListView(listOf("a", "b", "c"))

        list.items.removeAt(1)
        assertEquals(listOf("a", "c"), list.items)

        list.items.remove("a")
        assertEquals(listOf("c"), list.items)

        list.items.clear()
        assertEquals(emptyList(), list.items)
        assertEquals(0, list.size)
    }

    @Test
    fun `a row can be replaced in place`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.items[1] = "B"

        assertEquals(listOf("a", "B"), list.items)
        assertEquals("B", list.selectedItem, "the selection stays on the row it was on")
        assertEquals(1, list.selectedIndex)
    }

    @Test
    fun `several rows can be inserted at once through the live list`() {
        val list = ListView(listOf("a", "d"))

        list.items.addAll(1, listOf("b", "c"))

        assertEquals(listOf("a", "b", "c", "d"), list.items)
    }

    @Test
    fun `removing every row of a given kind keeps the selection on its item`() {
        val list = ListView(listOf("keep", "drop", "stay"))
        list.select(2)

        list.items.removeAll(listOf("drop"))

        assertEquals(listOf("keep", "stay"), list.items)
        assertEquals("stay", list.selectedItem)
        assertEquals(1, list.selectedIndex)
    }

    @Test
    fun `clearing the live list drops the selection`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.items.clear()

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
    }

    @Test
    fun `a row can be inserted at an index through the live list`() {
        val list = ListView(listOf("a", "c"))

        list.items.add(1, "b")

        assertEquals(listOf("a", "b", "c"), list.items)
    }

    @Test
    fun `inserting past the end fails but the end itself is allowed`() {
        val list = ListView(listOf("a"))

        list.items.add(1, "b")
        assertEquals(listOf("a", "b"), list.items)

        assertFailsWith<IndexOutOfBoundsException> { list.items.add(3, "nope") }
        assertFailsWith<IndexOutOfBoundsException> { list.items.add(-1, "nope") }
    }

    @Test
    fun `several rows can be appended at once`() {
        val list = ListView(listOf("a"))

        list.addAll(listOf("b", "c"))

        assertEquals(listOf("a", "b", "c"), list.items)
    }

    @Test
    fun `appending keeps the selection on the same row`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.add("c")

        assertEquals(1, list.selectedIndex)
        assertEquals("b", list.selectedItem)
    }

    @Test
    fun `inserting above the selected row moves the selection down with it`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.items.add(0, "zero")

        assertEquals(listOf("zero", "a", "b"), list.items)
        assertEquals("b", list.selectedItem, "the selection follows the item, not the index")
        assertEquals(2, list.selectedIndex)
    }

    @Test
    fun `inserting below the selected row leaves the selection alone`() {
        val list = ListView(listOf("a", "b"))
        list.select(0)

        list.items.add(1, "mid")

        assertEquals(0, list.selectedIndex)
        assertEquals("a", list.selectedItem)
    }

    @Test
    fun `inserting above a list without a selection does not invent one`() {
        val list = ListView(listOf("a"))

        list.items.add(0, "zero")

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
    }

    @Test
    fun `a row can be removed by index`() {
        val list = ListView(listOf("a", "b", "c"))

        val removed = list.removeAt(1)

        assertEquals("b", removed)
        assertEquals(listOf("a", "c"), list.items)
    }

    @Test
    fun `removing past the end fails`() {
        val list = ListView(listOf("a"))

        assertFailsWith<IllegalArgumentException> { list.removeAt(1) }
        assertFailsWith<IllegalArgumentException> { list.removeAt(-1) }
    }

    @Test
    fun `removing the selected row drops the selection`() {
        val list = ListView(listOf("a", "b", "c"))
        list.select(1)

        list.removeAt(1)

        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
    }

    @Test
    fun `removing a row above the selected one moves the selection up with it`() {
        val list = ListView(listOf("a", "b", "c"))
        list.select(2)

        list.removeAt(0)

        assertEquals("c", list.selectedItem, "the selection follows the item, not the index")
        assertEquals(1, list.selectedIndex)
    }

    @Test
    fun `removing a row below the selected one leaves the selection alone`() {
        val list = ListView(listOf("a", "b", "c"))
        list.select(0)

        list.removeAt(2)

        assertEquals(0, list.selectedIndex)
        assertEquals("a", list.selectedItem)
    }

    @Test
    fun `a row can be removed by text and reports whether it was there`() {
        val list = ListView(listOf("a", "b"))

        assertTrue(list.remove("a"))
        assertEquals(listOf("b"), list.items)

        assertFalse(list.remove("missing"))
        assertEquals(listOf("b"), list.items)
    }

    @Test
    fun `removing by text takes the first match, because duplicates are indistinguishable`() {
        val list = ListView(listOf("dup", "dup", "other"))
        list.select(2)

        list.remove("dup")

        assertEquals(listOf("dup", "other"), list.items)
        assertEquals("other", list.selectedItem)
        assertEquals(1, list.selectedIndex)
    }

    @Test
    fun `a list can be emptied and refilled`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)

        list.clear()
        assertEquals(emptyList(), list.items)
        assertEquals(0, list.size)
        assertEquals(ListView.NO_SELECTION, list.selectedIndex)

        list.add("c")
        assertEquals(listOf("c"), list.items)
    }

    @Test
    fun `adding and removing never runs either handler`() {
        val reported = mutableListOf<Int>()
        val reportedItems = mutableListOf<String>()
        val list = ListView(listOf("a", "b"))
            .onSelect { reported += it }
            .onSelectItem { reportedItems += it }
        list.select(1)

        list.items.add(0, "zero")
        list.removeAt(0)
        list.clear()

        assertTrue(reported.isEmpty(), "the user selected nothing: $reported")
        assertTrue(reportedItems.isEmpty(), "the user selected nothing: $reportedItems")
    }

    @Test
    fun `a list resets the selection and colours but keeps the rows it grew`() {
        val list = ListView(listOf("a", "b"))
        list.select(1)
        list.selectedBackground = Color.web("#cccccc")
        list.styleSpec.background { color = Color.web("#102030") }
        list.styleSpec.foreground = Color.web("#f0f0f0")
        list.add("c")
        list.removeAt(0)

        list.reset()

        // The rows are the caller's live collection, so a reset leaves them where they are; only
        // what the component presents — selection and colours — goes back to the defaults.
        assertEquals(listOf("b", "c"), list.items)
        assertEquals(ListView.NO_SELECTION, list.selectedIndex)
        assertNull(list.selectedItem)
        assertEquals(Color.web("#eeeeee"), list.selectedBackground)
        assertEquals(ListView.DEFAULT_BACKGROUND, list.styleSpec.background.color)
        assertEquals(ListView.DEFAULT_FOREGROUND, list.styleSpec.foreground)
    }

    @Test
    fun `the rows a list was created with can be put back`() {
        val list = ListView(listOf("a", "b"))
        list.add("c")

        list.items(list.initialItems)

        assertEquals(listOf("a", "b"), list.items)
    }

    @Test
    fun `adding on every redraw grows the list instead of replacing a row`() {
        // The shape a state-driven list has: the block is re-run on every state change and appends
        // each time it does. A reset that restored the construction rows would make every append
        // overwrite the row before it.
        val state = com.github.kusa233.ovatoxin.state.State(0)
        val list = ListView(listOf("A", "B", "C"))
        val frame = com.github.kusa233.ovatoxin.window.Frame()
        frame.layout {
            draw(list) { component ->
                component.onSelectItem { }
                if (state.value > 0) component.add("item${state.value}")
            }.withState(state)
        }

        state.update(1)
        state.update(2)
        state.update(3)

        assertEquals(listOf("A", "B", "C", "item1", "item2", "item3"), list.items)
    }

    @Test
    fun `a redraw starts from the defaults, so the handler survives but not the selection`() {
        val reported = mutableListOf<String>()
        val list = ListView(listOf("a", "b"))
        var frame = com.github.kusa233.ovatoxin.window.Frame()
        frame.layout {
            draw(list) { component ->
                component.onSelectItem { reported += it }
                component.select(1)
            }
        }

        // A state-driven redraw calls the block again; selecting twice must not report twice.
        list.redraw()

        assertEquals(1, list.selectedIndex)
        assertTrue(reported.isEmpty(), reported.toString())

        list.selectFromUser(0)
        assertEquals(listOf("a"), reported)
    }

    @Test
    fun `a list cannot be drawn twice`() {
        val frame = com.github.kusa233.ovatoxin.window.Frame()
        val list = ListView(listOf("a"))
        frame.draw(list)

        assertFailsWith<IllegalArgumentException> {
            com.github.kusa233.ovatoxin.window.Frame().draw(list)
        }
    }
}
