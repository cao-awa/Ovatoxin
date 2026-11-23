package com.github.kusa233.ovatoxin.widget

/**
 * A [MutableList] over [backing] that reports what every change did.
 *
 * The list is the storage itself rather than a copy of it, so a caller holds one object and writes
 * to it; [onChange] is called after each mutation with the index it started at, how many elements it
 * removed there and how many it inserted, which is enough for the owner to keep a selection pointing
 * at the same element through the change.
 *
 * The reporting methods are the ones a `MutableList` contract adds to [List]: everything that
 * changes the size or the contents goes through one of them, so wrapping them is enough — the
 * methods the JDK builds on top of them (an iterator's `remove`, `removeAll` and `retainAll` with a
 * predicate, a `subList` view) all end up in one of the overrides below.
 *
 * Bulk removals ([removeAll], [retainAll]) are reported as one change per element removed, so the
 * owner sees each removal instead of having to diff the list afterwards.
 *
 * This is the live collection behind [ListView.items] and [ComboBox.items]: both widgets expose the
 * same "mutate the list and the widget follows" behaviour, so the reporting is implemented once.
 */
internal class LiveItems<T>(
    private val backing: MutableList<T>,
    private val onChange: (start: Int, removed: Int, inserted: Int) -> Unit,
) : AbstractMutableList<T>() {

    override val size: Int
        get() = this.backing.size

    override fun get(index: Int): T = this.backing[index]

    override fun add(index: Int, element: T) {
        this.backing.add(index, element)
        onChange(index, 0, 1)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        val added = this.backing.addAll(index, elements)
        onChange(index, 0, elements.size)
        return added
    }

    override fun addAll(elements: Collection<T>): Boolean = addAll(this.backing.size, elements)

    override fun removeAt(index: Int): T {
        val removed = this.backing.removeAt(index)
        onChange(index, 1, 0)
        return removed
    }

    override fun set(index: Int, element: T): T {
        val previous = this.backing.set(index, element)
        // A replacement leaves every index where it was, so the selection stays on its row and
        // simply points at the new item. Reporting it as a removal would drop the selection for no
        // reason.
        onChange(index, 0, 0)
        return previous
    }

    /** Clears the list, reporting the whole run of rows at once. */
    override fun clear() {
        val removed = this.backing.size
        this.backing.clear()
        onChange(0, removed, 0)
    }

    /** Removes every element of [elements] that is present, reporting one change per element. */
    override fun removeAll(elements: Collection<T>): Boolean {
        val targets = elements.toSet()
        return removeMatching { it in targets }
    }

    /** Removes every element not in [elements], reporting one change per element. */
    override fun retainAll(elements: Collection<T>): Boolean {
        val keep = elements.toSet()
        return removeMatching { it !in keep }
    }

    /**
     * Removes every element [shouldRemove] accepts, one reported change each.
     *
     * Removing from the back keeps the index of what is left untouched, so the changes are reported
     * in a way the owner can apply one after another without re-reading the list in between.
     */
    private fun removeMatching(shouldRemove: (T) -> Boolean): Boolean {
        var changed = false
        for (index in this.backing.lastIndex downTo 0) {
            if (!shouldRemove(this.backing[index])) continue
            this.backing.removeAt(index)
            onChange(index, 1, 0)
            changed = true
        }
        return changed
    }
}
