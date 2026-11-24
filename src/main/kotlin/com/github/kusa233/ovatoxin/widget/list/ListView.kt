package com.github.kusa233.ovatoxin.widget.list

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.style.cssColor
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseButton
import javafx.scene.layout.VBox
import javafx.scene.paint.Color

/**
 * A vertical list of rows, one of which can be selected.
 *
 * The rows are of whatever type the list is given, not just text; [itemText] decides how an item is
 * drawn, and defaults to the item's own `toString()` — which is what a list of strings wants:
 *
 * ```kotlin
 * draw(ListView(listOf("One", "Two", "Three"))) { list ->
 *     layout {
 *         x = 10.percent
 *         y = 10.percent
 *         width = 40.percent
 *         height = 30.percent
 *     }
 *     list.onSelectItem { item -> println(item) }
 * }
 * ```
 *
 * A list of your own type passes a renderer and then hands out the item itself:
 *
 * ```kotlin
 * class User(val name: String, val mail: String)
 *
 * draw(ListView(users) { it.name }) { list ->
 *     list.onSelectItem { user -> println(user.mail) }   // the User, not an index
 * }
 * ```
 *
 * Clicking a row selects it and runs the registered handlers: [onSelect] with the index, and
 * [onSelectItem] with the item. Register whichever one you have a use for — both may be registered,
 * and then both run. [selectedIndex], [selectedItem] and [select] change the selection without
 * running either handler, so a list can be driven from a
 * [com.github.kusa233.ovatoxin.state.State] without the handler feeding itself.
 *
 * The rows can change while the list is on screen — the list grows and shrinks as rows are added
 * and removed, and every change renders immediately. [items] is a live `MutableList`, so there is no
 * index to pass and no second call to make:
 *
 * ```kotlin
 * list.items.add("Four")            // the list gets longer, the selection is untouched
 * list.items.removeAt(0)            // removing the selected row drops the selection
 * list.items.clear()                // no rows, no selection
 * ```
 *
 * The selection follows the item it points at rather than the index it happens to sit on: removing
 * a row above the selected one shifts the selection up so it keeps pointing at the same item.
 *
 * [add], [addAll], [remove], [removeAt] and [clear] are the same thing spelled shorter, plus the
 * null checks in [removeAt]; reach for them when they read better, or use [items] directly when the
 * code is already working with a collection.
 *
 * The selected row is painted in [selectedBackground], the rest of the rows stay transparent so the
 * fill of the component itself shows through:
 *
 * - the background is [DEFAULT_BACKGROUND] (`#ffffff`), overridable through the `background` of a
 *   [style][com.github.kusa233.ovatoxin.component.DrawScope.style] block;
 * - the text colour is [DEFAULT_FOREGROUND] (`#000000`), overridable through its `foreground`.
 *
 * Once the rows are taller than the list, the list scrolls: a bar appears along its right edge with
 * a flat grey thumb on it and nothing else — no frame, no track, no arrow buttons, no focus ring —
 * so the only thing the bar adds to the list is the thumb. The rows keep sitting on the fill of the
 * list itself, which stays clean behind the bar. Nothing has to be switched on for this, and there
 * is no scroll position to keep: adding a row past the bottom simply makes the list scrollable.
 *
 * The selection is the only thing a redraw resets. The rows are the live collection the caller
 * mutates, not a value a draw block writes, so they survive a redraw — which is what makes it
 * possible to add to a list from a block that is itself re-run on every state change:
 *
 * ```kotlin
 * val list = draw(ListView(listOf("A", "B", "C"))) { list ->
 *     when (val op = opState.value) {
 *         is Add -> list.items.add(op.item)   // each state change appends; nothing is replaced
 *         is Remove -> list.remove(op.item)
 *         else -> {}
 *     }
 * }.withState(opState)
 * ```
 *
 * The block runs again on every redraw, so an append written in it appends each time it runs —
 * including the redraw of an unrelated change. Guard it on the value that describes the change, as
 * above, rather than appending unconditionally.
 *
 * To put the rows back to what the list was created with, pass [initialItems] to [items].
 */
class ListView<T>(
    items: List<T> = emptyList(),
    private val itemText: (T) -> String = { it.toString() },
) : Component<ListView<T>>() {

    /** The rows to start from again if the rows should be put back; see [reset]. */
    val initialItems: List<T> = items.toList()

    private val defaultSelectedBackground: Color = DEFAULT_SELECTED_BACKGROUND

    /** The rows the list holds. This is the only storage; [items] is a view over it. */
    private val rowValues: MutableList<T> = this.initialItems.toMutableList()

    /**
     * The rows of the list, live.
     *
     * Adding to it, removing from it or clearing it grows and shrinks the list on screen before the
     * call returns — there is no index to pass and nothing to call afterwards:
     *
     * ```kotlin
     * list.items.add("Four")
     * list.items.removeAt(0)
     * list.items.addAll(listOf("Five", "Six"))
     * list.items.clear()
     * ```
     *
     * Every `MutableList` method works, and the selection follows the item it pointed at through
     * each of them. To replace the rows in one step, use [items] with an argument.
     */
    val items: MutableList<T> = LiveItems(this.rowValues) { start, removed, inserted ->
        onRowsChanged(start, removed, inserted)
    }

    private var selectedValue: Int = NO_SELECTION
    private var selectedBackgroundValue: Color = this.defaultSelectedBackground
    private var selectHandler: ((Int) -> Unit)? = null
    private var selectItemHandler: ((T) -> Unit)? = null

    /** The rows of the list, in order. */
    private var rows: MutableList<Label> = mutableListOf()

    /** The box the rows live in; `null` until the node is built. */
    private var box: VBox? = null

    init {
        // A list is a light surface with dark text even when nobody styles it.
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
        this.styleSpec.foreground = DEFAULT_FOREGROUND
    }

    /** Reads the rows of the list. */
    fun items(): List<T> = this.items

    /**
     * Replaces every row of the list.
     *
     * A selection that no longer points at a row is dropped; the handlers are not told, because
     * nothing was selected by the user.
     */
    fun items(value: List<T>) {
        applyItems(value)
    }

    /**
     * Appends a row to the end of the list.
     *
     * Shorthand for `items.add(item)`; the selection is untouched, because every row that was there
     * stays where it was.
     */
    fun add(item: T): ListView<T> {
        this.items.add(item)
        return this
    }

    /** Appends every row of [items] to the end of the list, in order. */
    fun addAll(items: List<T>): ListView<T> {
        this.items.addAll(items)
        return this
    }

    /**
     * Removes the row at [index] and returns it.
     *
     * Removing the selected row drops the selection, since the item it pointed at is gone; removing
     * a row above it shifts the selection up so it keeps pointing at the same item. The handlers are
     * not told either way — the user did not select anything.
     *
     * Shorthand for `items.removeAt(index)`.
     */
    fun removeAt(index: Int): T {
        require(index in this.items.indices) {
            "index $index is outside 0..${this.items.lastIndex}"
        }
        return this.items.removeAt(index)
    }

    /**
     * Removes the first row equal to [item], and reports whether there was one.
     *
     * Removing by item cannot tell duplicates apart, so it always takes the first match; [removeAt]
     * removes a specific row of a list that repeats itself.
     */
    fun remove(item: T): Boolean = this.items.remove(item)

    /** Removes every row, and with them the selection. */
    fun clear(): ListView<T> {
        this.items.clear()
        return this
    }

    /** The number of rows. */
    val size: Int
        get() = this.items.size

    /** Reads the row at [index]. */
    fun item(index: Int): T {
        require(index in this.items.indices) {
            "index $index is outside 0..${this.items.lastIndex}"
        }
        return this.items[index]
    }

    /** The index of the selected row, or [NO_SELECTION] when no row is selected. */
    var selectedIndex: Int
        get() = this.selectedValue
        set(value) = select(value)

    /** The selected row, or `null` when no row is selected. */
    val selectedItem: T?
        get() = this.rowValues.getOrNull(this.selectedValue)

    /** The text of the selected row, or `null` when no row is selected. */
    val selectedText: String?
        get() = this.selectedItem?.let(this.itemText)

    /**
     * Selects the row at [index], or drops the selection with `-1` ([NO_SELECTION]).
     *
     * This is the programmatic path and it stays silent: the [onSelect] handler only runs when the
     * user clicks a row. That is what makes a list safe to drive from a [State] — a draw block can
     * write [selectedIndex] on every redraw, and the handler never feeds itself.
     *
     * An index outside the rows is a programming mistake, so it throws instead of silently doing
     * nothing.
     */
    fun select(index: Int) {
        require(isSelectable(index)) {
            "index $index is outside -1..${this.rowValues.lastIndex}"
        }
        this.selectedValue = index
        refreshRows()
    }

    /** Drops the selection, as [select] with [NO_SELECTION] does. */
    fun clearSelection() {
        select(NO_SELECTION)
    }

    /** The colour the selected row is painted in. Defaults to [DEFAULT_SELECTED_BACKGROUND]. */
    var selectedBackground: Color
        get() = this.selectedBackgroundValue
        set(value) {
            this.selectedBackgroundValue = value
            refreshRows()
        }

    /**
     * Registers a handler invoked with the index of every row the user selects.
     *
     * Use [onSelectItem] instead when the index is only a way to get at the item.
     */
    fun onSelect(handler: (Int) -> Unit): ListView<T> {
        this.selectHandler = handler
        return this
    }

    /**
     * Registers a handler invoked with the item of every row the user selects.
     *
     * The item arrives as it is — a `ListView<User>` hands over the `User` — so a selection needs no
     * follow-up [item] lookup. Registering this instead of [onSelect] is the usual choice; both may
     * be registered, and then both run on the same click.
     */
    fun onSelectItem(handler: (T) -> Unit): ListView<T> {
        this.selectItemHandler = handler
        return this
    }

    /**
     * Selects the row at [index] on behalf of the user, then runs the registered handlers.
     *
     * Separate from [select] so the programmatic path cannot re-enter its own handlers; a row click
     * is the only caller in production code.
     */
    internal fun selectFromUser(index: Int) {
        select(index)
        val item = this.rowValues[index]
        this.selectHandler?.invoke(index)
        this.selectItemHandler?.invoke(item)
    }

    /**
     * Restores what the list looks like, keeping the rows it currently holds.
     *
     * The rows are not part of the draw block the way a caption is: [items] is a live collection the
     * caller owns, and adding to it is the documented way to grow the list. Restoring the
     * construction rows here would undo exactly that — a redraw runs before the block's own
     * `list.add(...)`, so the row just added would be thrown away and re-appended in its place, and
     * a list driven from a [com.github.kusa233.ovatoxin.state.State] would never hold more than its
     * first extra row.
     *
     * So a reset drops the selection, the [selectedBackground] override and the handlers, and
     * reseeds the default colours, leaving the rows alone. To go back to the rows the list was
     * created with, call [items] with [initialItems].
     */
    override fun reset() {
        super.reset()
        this.selectedValue = NO_SELECTION
        this.selectedBackgroundValue = this.defaultSelectedBackground
        this.selectHandler = null
        this.selectItemHandler = null
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
        this.styleSpec.foreground = DEFAULT_FOREGROUND
        // The rows stay, but the selection just went away, so the highlight has to be repainted.
        refreshRows()
    }

    /**
     * The rows, inside a scroll pane that shows a plain grey bar once they outgrow the list.
     *
     * A growable list ends up with more rows than fit, so the rows live in a scroll pane and it
     * brings the bar that makes the rest reachable. Only the bar is meant to be seen: the pane is
     * dressed down to no frame, no focus ring, no arrows and no track, just a grey thumb, so the
     * rows still sit on the fill the style layer paints on the list itself.
     *
     * The dressing is a stylesheet ([STYLESHEET] in `resources`) rather than inline CSS, because
     * the bar is a child of the pane and the style layer only ever writes onto the list's own node.
     * A stylesheet of ours also outranks Modena's, which is what a plain grey bar needs.
     */
    override fun buildNode(): Node {
        val box = VBox()
        // Rows span the full width, so the selection highlight reads as a bar rather than a tag.
        box.isFillWidth = true
        this.box = box
        rebuildRows()

        val pane = ScrollPane(box)
        // A list only ever scrolls down: its rows are as wide as the list by construction.
        pane.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        pane.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        // Rows stretch to the width of the viewport instead of asking for their own text width.
        pane.isFitToWidth = true
        pane.styleClass += STYLE_CLASS
        ListView::class.java.getResource(STYLESHEET)?.let { pane.stylesheets += it.toExternalForm() }
        return pane
    }

    /** Re-paints the rows after a style pass, because a row carries the text colour itself. */
    override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        refreshRows()
    }

    /** Whether [index] is either no selection at all or points at a row. */
    private fun isSelectable(index: Int): Boolean =
        index == NO_SELECTION || index in this.rowValues.indices

    /**
     * Handles a change to the rows: follows the selection through it, then renders the new rows.
     *
     * The selection belongs to the item, not to the number it sits under, so it is shifted by the
     * change: rows inserted or removed above it move it, and losing the item it points at drops it.
     * [start] is the index the change begins at, [removed] how many rows it took out there and
     * [inserted] how many it put back, which is enough to tell the two cases apart — an append has
     * `removed == 0` and a `start` at or past the selection.
     */
    private fun onRowsChanged(start: Int, removed: Int, inserted: Int) {
        val selected = this.selectedValue
        if (selected != NO_SELECTION) {
            if (selected < start) {
                // The change is below the selection, which keeps pointing at the same row.
            } else if (selected >= start + removed) {
                // The change is above the selection, so it slides by the size difference.
                this.selectedValue = selected - removed + inserted
            } else {
                // The selected row itself was removed, and there is no item left to point at.
                this.selectedValue = NO_SELECTION
            }
        }
        rebuildRows()
    }

    /**
     * Replaces every row of the list.
     *
     * Unlike [items] this can only lose the selection, never shift it: the rows before and after
     * have nothing in common to follow.
     */
    private fun applyItems(value: List<T>) {
        val selected = this.selectedItem
        this.rowValues.clear()
        this.rowValues.addAll(value)
        this.selectedValue =
            if (selected == null) NO_SELECTION else this.rowValues.indexOf(selected)
        rebuildRows()
    }

    /** Recreates one row per item, used whenever the rows themselves change. */
    private fun rebuildRows() {
        val box = this.box ?: return
        box.children.clear()
        this.rows = mutableListOf()
        this.rowValues.forEachIndexed { index, item ->
            val row = Label(this.itemText(item))
            // A label asks for exactly its text width, so `isFillWidth` alone would leave it narrow.
            // Widening it keeps the row the whole width of the list, and the highlight with it.
            row.maxWidth = Double.MAX_VALUE
            row.setOnMouseClicked { event ->
                if (event.button == MouseButton.PRIMARY) selectFromUser(index)
            }
            styleRow(row, index)
            this.rows += row
            box.children += row
        }
    }

    /** Re-applies the row styling, used when only the selection or the colours changed. */
    private fun refreshRows() {
        this.rows.forEachIndexed { index, row -> styleRow(row, index) }
    }

    /**
     * The inline CSS of one row: the selected row takes [selectedBackground], the others stay
     * transparent so the fill of the list itself shows through.
     *
     * A row is styled here instead of through the style layer, because that layer describes the box
     * of a component as a whole and cannot tell one row from another. The text colour still comes
     * from the `foreground` of the style, so a list is tinted from one place.
     */
    internal fun rowCss(selected: Boolean): String {
        val fill = if (selected) this.selectedBackgroundValue else Color.TRANSPARENT
        val text = this.styleSpec.foreground ?: DEFAULT_FOREGROUND
        return buildString {
            append("-fx-background-color: ").append(fill.cssFill()).append("; ")
            append("-fx-text-fill: ").append(cssColor(text)).append("; ")
            append("-fx-background-insets: 0; ")
            append("-fx-padding: ").append(ROW_PADDING).append(";")
        }
    }

    /** Paints one row with [rowCss]; identical CSS is not re-applied, so resizes stay cheap. */
    private fun styleRow(row: Label, index: Int) {
        val css = rowCss(index == this.selectedValue)
        if (row.style != css) row.style = css
    }

    /** Formats a fill, spelling a fully transparent one as `transparent` instead of `rgba(…, 0)`. */
    private fun Color.cssFill(): String =
        if (this.opacity == 0.0) "transparent" else cssColor(this)

    companion object {
        /** The fill of a list until a style overrides it. */
        val DEFAULT_BACKGROUND: Color = Color.web("#ffffff")

        /** The text colour of a list until a style overrides it. */
        val DEFAULT_FOREGROUND: Color = Color.web("#000000")

        /** The fill of the selected row: a light grey that reads as a highlight on white. */
        val DEFAULT_SELECTED_BACKGROUND: Color = Color.web("#eeeeee")

        /** The [selectedIndex] value of a list without a selection. */
        const val NO_SELECTION: Int = -1

        /** Style class every OvatoXin list carries; the scroll bar styling hangs off it. */
        const val STYLE_CLASS: String = "ovatoxin-list"

        /** The stylesheet of the scroll bar, next to this class in `resources`. */
        private const val STYLESHEET: String = "listview.css"

        /** Top, right, bottom and left padding of a row, in pixels. */
        private const val ROW_PADDING: String = "6 10 6 10"
    }
}
