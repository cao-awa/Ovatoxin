package com.github.kusa233.ovatoxin.component

/**
 * Bookkeeping for one redraw of a component subtree.
 *
 * A redraw re-runs the block a component was drawn with, and that block draws its children
 * again. Reusing the children that already exist keeps the tree intact: each container that is
 * drawn into during the replay hands out its children in order, so the second run of a
 * `layout { }` block lands on the same components instead of building a duplicate set.
 *
 * A container whose block draws fewer children than before is trimmed when the replay ends, so
 * a block that reacts to a state by dropping a child behaves as expected.
 */
internal class ReplayScope {

    private val cursors = LinkedHashMap<Container<*>, Int>()

    /**
     * Registers [container] as being replayed.
     *
     * A container that draws no children at all never passes through [draw], so it has to be
     * registered up front for [finish] to be able to trim it.
     */
    fun begin(container: Container<*>) {
        this.cursors.putIfAbsent(container, 0)
    }

    /** The next child slot of [container] that a replay should fill. */
    fun index(container: Container<*>): Int = this.cursors[container] ?: 0

    /** Records that [container] filled one more slot. */
    fun advance(container: Container<*>) {
        this.cursors[container] = index(container) + 1
    }

    /** Drops the children every touched container no longer drew. */
    fun finish() {
        for ((container, count) in this.cursors) {
            container.trimTo(count)
        }
    }

    companion object {
        /** The replay currently running, or `null` when components are being drawn normally. */
        var current: ReplayScope? = null
    }
}
