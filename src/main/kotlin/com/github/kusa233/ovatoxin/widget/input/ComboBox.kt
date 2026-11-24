package com.github.kusa233.ovatoxin.widget.input

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.widget.list.LiveItems
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView as FxListView
import javafx.scene.control.ComboBox as FxComboBox
import javafx.scene.paint.Color
import javafx.util.Callback

/**
 * A field that shows one of its items and drops a list of the rest down when clicked.
 *
 * ```kotlin
 * draw(ComboBox(listOf("Small", "Medium", "Large"))) { combo ->
 *     layout {
 *         x = 10.percent
 *         y = 40.percent
 *         width = 40.percent
 *         height = 8.percent
 *     }
 *     combo.selectedIndex = 1
 *     combo.onSelectItem { size -> println("size: $size") }
 * }
 * ```
 *
 * The items are any type; pass a formatter to choose what a row reads as:
 *
 * ```kotlin
 * draw(ComboBox(languages) { it.name }) { combo -> ... }
 * ```
 *
 * [items] is a live collection, exactly as [items][com.github.kusa233.ovatoxin.widget.list.ListView.items] is: adding to it grows the dropdown
 * before the call returns, and the selection follows the item it points at — a row inserted above
 * the selected one shifts the selection down so it keeps pointing at the same item, and losing the
 * selected item drops the selection:
 *
 * ```kotlin
 * combo.items.add("Extra Large")
 * combo.items.removeAt(0)
 * ```
 *
 * The selection is silent when the caller sets it. [onSelect] and [onSelectItem] only run for a row
 * the user picks, so a combo box driven from a
 * [State][com.github.kusa233.ovatoxin.state.State] cannot feed its own handler.
 */
class ComboBox<T>(
    items: List<T> = emptyList(),
    private val itemText: (T) -> String = { it.toString() },
) : Component<ComboBox<T>>() {

    /** The items to start from again if the dropdown should be put back; see [reset]. */
    val initialItems: List<T> = items.toList()

    /** The items the dropdown holds. This is the only storage; [items] is a view over it. */
    private val rowValues: MutableList<T> = this.initialItems.toMutableList()

    /**
     * The items of the dropdown, live.
     *
     * Working with it is the same as working with [items][com.github.kusa233.ovatoxin.widget.list.ListView.items]: every `MutableList` method is
     * available, and each one updates the dropdown and the selection immediately.
     */
    val items: MutableList<T> = LiveItems(this.rowValues) { start, removed, inserted ->
        onItemsChanged(start, removed, inserted)
    }

    private var selectedValue: Int = NO_SELECTION
    private var promptValue: String = ""
    private var enabledValue: Boolean = true
    private var selectHandler: ((Int) -> Unit)? = null
    private var selectItemHandler: ((T) -> Unit)? = null

    /**
     * Whether the selection is being written *to* the node rather than read from it.
     *
     * A JavaFX combo box reports every selection change, including the ones it makes itself when
     * its items are replaced. Without this guard a growable dropdown would look like the user was
     * picking a row on every mutation, and `items.add(...)` would run [onSelectItem].
     */
    private var syncing: Boolean = false

    /**
     * The JavaFX combo box itself; `null` until the node is built.
     *
     * Kept because the node is created once and then written to on every change: pushing items onto
     * it needs its own typed `items` list, which a `Node` reference cannot offer.
     */
    private var combo: FxComboBox<T>? = null

    init {
        // A combo box is a light surface with dark text even when nobody styles it.
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
        this.styleSpec.foreground = DEFAULT_FOREGROUND
    }

    /** Reads the items of the dropdown. */
    fun items(): List<T> = this.items

    /**
     * Replaces every item of the dropdown.
     *
     * The selection is kept if the item it pointed at is still there, and dropped otherwise; the
     * handlers are not told, because nothing was picked by the user.
     */
    fun items(value: List<T>) {
        applyItems(value)
    }

    /** The text shown in the field while no item is selected. Defaults to empty. */
    var prompt: String
        get() = this.promptValue
        set(value) {
            this.promptValue = value
            this.combo?.promptText = value
        }

    /** Whether the dropdown can be opened. Defaults to `true`. */
    var enabled: Boolean
        get() = this.enabledValue
        set(value) {
            this.enabledValue = value
            this.combo?.isDisable = !value
        }

    /** The index of the selected item, or [NO_SELECTION] when nothing is selected. */
    var selectedIndex: Int
        get() = this.selectedValue
        set(value) = select(value)

    /** The selected item, or `null` when nothing is selected. */
    val selectedItem: T?
        get() = this.rowValues.getOrNull(this.selectedValue)

    /** The text of the selected item, as [itemText] formats it, or `null` when nothing is selected. */
    val selectedText: String?
        get() = this.selectedItem?.let(this.itemText)

    /** The number of items in the dropdown. */
    val size: Int
        get() = this.items.size

    /** Reads the item at [index]. */
    fun item(index: Int): T {
        require(index in this.items.indices) {
            "index $index is outside 0..${this.items.lastIndex}"
        }
        return this.items[index]
    }

    /**
     * Selects the item at [index], or clears the selection with `-1` ([NO_SELECTION]).
     *
     * This is the programmatic path and it stays silent: [onSelect] and [onSelectItem] only run for
     * a row the user picks. That is what makes `combo.selectedIndex = state.value` safe inside a
     * draw block that is re-run on every change of that state.
     *
     * An index outside the items is a programming mistake, so it throws instead of silently doing
     * nothing.
     */
    fun select(index: Int) {
        require(isSelectable(index)) {
            "index $index is outside -1..${this.rowValues.lastIndex}"
        }
        this.selectedValue = index
        applySelectionToNode()
    }

    /** Drops the selection, as [select] with [NO_SELECTION] does. */
    fun clearSelection() {
        select(NO_SELECTION)
    }

    /**
     * Registers a handler invoked with the index of every item the user picks.
     *
     * Use [onSelectItem] instead when the index is only a way to get at the item.
     */
    fun onSelect(handler: (Int) -> Unit): ComboBox<T> {
        this.selectHandler = handler
        return this
    }

    /**
     * Registers a handler invoked with the item the user picks.
     *
     * The item arrives as it is — a `ComboBox<User>` hands over the `User` — so a selection needs no
     * follow-up [item] lookup. Both handlers may be registered, and then both run on the same pick.
     */
    fun onSelectItem(handler: (T) -> Unit): ComboBox<T> {
        this.selectItemHandler = handler
        return this
    }

    /**
     * Selects the item at [index] on behalf of the user, then runs the registered handlers.
     *
     * Separate from [select] so the programmatic path cannot re-enter its own handlers; a pick in
     * the dropdown is the only caller in production code.
     */
    internal fun selectFromUser(index: Int) {
        select(index)
        if (index == NO_SELECTION) return
        this.selectHandler?.invoke(index)
        this.selectItemHandler?.invoke(this.rowValues[index])
    }

    /**
     * Restores what the dropdown looks like, keeping the items it currently holds.
     *
     * The items are not part of the draw block the way a caption is: [items] is a live collection
     * the caller owns, so a reset leaves it alone, exactly as [reset][com.github.kusa233.ovatoxin.widget.list.ListView.reset] does. To go back to
     * the items the combo box was created with, call [items] with [initialItems].
     */
    override fun reset() {
        super.reset()
        this.selectedValue = NO_SELECTION
        this.promptValue = ""
        this.enabledValue = true
        this.selectHandler = null
        this.selectItemHandler = null
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
        this.styleSpec.foreground = DEFAULT_FOREGROUND
        val node = this.combo
        node?.promptText = ""
        node?.isDisable = false
        applySelectionToNode()
    }

    override fun buildNode(): Node {
        val created = FxComboBox<T>()
        this.combo = created
        // Both the field and the rows of the dropdown read an item through `itemText`, so the
        // formatter given at construction decides what every item looks like.
        created.buttonCell = ItemCell(this.itemText)
        created.cellFactory = Callback<FxListView<T>, ListCell<T>> { ItemCell(this.itemText) }
        this.syncing = true
        created.items.setAll(this.rowValues)
        this.syncing = false
        created.selectionModel.selectedIndexProperty().addListener { _, _, value ->
            // The property is an `Integer`, so it arrives boxed and can also be null while the
            // selection model is being rebuilt; both cases land on "nothing selected".
            onNodeSelectionChanged(value?.toInt() ?: NO_SELECTION)
        }
        created.isDisable = !this.enabledValue
        created.promptText = this.promptValue
        created.styleClass += STYLE_CLASS
        applySelectionToNode()
        return created
    }

    /** Whether [index] is either no selection at all or points at an item. */
    private fun isSelectable(index: Int): Boolean =
        index == NO_SELECTION || index in this.rowValues.indices

    /**
     * Handles a change to the items: follows the selection through it, then renders the new items.
     *
     * The same arithmetic as [ListView][com.github.kusa233.ovatoxin.widget.list.ListView]: [start] is the index the change begins at, [removed] how
     * many items it took out there and [inserted] how many it put back, which is enough to tell a
     * change below the selection, above it and on it apart.
     */
    private fun onItemsChanged(start: Int, removed: Int, inserted: Int) {
        val selected = this.selectedValue
        if (selected != NO_SELECTION) {
            this.selectedValue = when {
                // Below the selection: it keeps pointing at the same item.
                selected < start -> selected
                // Above it: the selection slides by the size difference.
                selected >= start + removed -> selected - removed + inserted
                // The selected item itself is gone, so there is nothing left to point at.
                else -> NO_SELECTION
            }
        }
        rebuildItems()
    }

    /**
     * Replaces every item of the dropdown.
     *
     * Unlike [items] this can only lose the selection, never shift it: the items before and after
     * have nothing in common to follow.
     */
    private fun applyItems(value: List<T>) {
        val selected = this.selectedItem
        this.rowValues.clear()
        this.rowValues.addAll(value)
        this.selectedValue = if (selected == null) NO_SELECTION else this.rowValues.indexOf(selected)
        rebuildItems()
    }

    /** Pushes the items to the node, if it exists, and re-applies the selection. */
    private fun rebuildItems() {
        val node = this.combo ?: return
        this.syncing = true
        try {
            node.items.setAll(this.rowValues)
        } finally {
            this.syncing = false
        }
        applySelectionToNode()
    }

    /** Writes [selectedIndex] onto the node without it being read back as a user pick. */
    private fun applySelectionToNode() {
        val node = this.combo ?: return
        this.syncing = true
        try {
            if (this.selectedValue == NO_SELECTION) {
                node.selectionModel.clearSelection()
            } else {
                node.selectionModel.select(this.selectedValue)
            }
        } finally {
            this.syncing = false
        }
    }

    /** Mirrors a selection the node made into the field, then reports it as a user pick. */
    private fun onNodeSelectionChanged(index: Int) {
        if (this.syncing) return
        if (!isSelectable(index)) return
        selectFromUser(index)
    }

    companion object {
        /** The [selectedIndex] value of a combo box without a selection. */
        const val NO_SELECTION: Int = -1

        /** The fill of a combo box until a style overrides it. */
        val DEFAULT_BACKGROUND: Color = Color.web("#ffffff")

        /** The text colour of a combo box until a style overrides it. */
        val DEFAULT_FOREGROUND: Color = Color.web("#000000")

        /** Style class every OvatoXin combo box carries. */
        const val STYLE_CLASS: String = "ovatoxin-combo"
    }
}

/** A dropdown row, and the field of the combo box itself, painted with [itemText]. */
private class ItemCell<T>(private val itemText: (T) -> String) : ListCell<T>() {

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)
        this.text = if (empty) null else item?.let(this.itemText)
    }
}
