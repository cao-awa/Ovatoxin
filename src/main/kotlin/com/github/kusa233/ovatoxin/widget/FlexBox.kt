package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.Size
import com.github.kusa233.ovatoxin.component.Slot
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

/**
 * A drawing surface that lays its children out in lines instead of at absolute positions.
 *
 * A [Div] places every child exactly where its own `x` and `y` say. A flex box instead flows them:
 * the first child goes to the top left corner of the content box, the next one is put to its right,
 * and so on until the line is full — then a new line starts underneath, as tall as the tallest child
 * of the line it follows. Like every surface it can draw, and the padding it declares in its layout
 * block is what separates its children:
 *
 * * `hPadding` is the inset on the left and right *and* the gap between two children of the same
 *   line;
 * * `vPadding` is the inset at the top and bottom *and* the gap between two lines.
 *
 * ```kotlin
 * frame.layout {
 *     draw(FlexBox()) { row ->
 *         layout {
 *             width = 100.percent
 *             hPadding = 8.px
 *             vPadding = 8.px
 *         }
 *         row.layout {
 *             draw(Button()) { it.text("One") }
 *             draw(Button()) { it.text("Two") }
 *             draw(Button()) { it.text("Three") }
 *         }
 *     }
 * }
 * ```
 *
 * The flow is decided by the widths the children resolve to, so a child still says how big it is in
 * its own `layout` block — `width = 30.percent` of the content box, or `width = 120.px`. A child
 * that says nothing is measured at its preferred size, which is what makes a row of buttons come
 * out right without any arithmetic.
 *
 * `x` and `y` do not lose their meaning: they are added onto the slot the flow computed, so
 * `y = 2.px` nudges a child two pixels down inside its line instead of pinning it near the top:
 *
 * ```kotlin
 * draw(Button()) {
 *     it.layout { x = 4.px; y = 2.px }   // 4px right of its slot, 2px below it
 * }
 * ```
 *
 * A child is never squeezed to fit: when it is wider than the room left on the current line it is
 * moved to the next one, and when it is wider than a whole line it simply gets a line of its own and
 * overflows to the right.
 *
 * Without a `width` or `height` of its own the box is as large as its content, so it can be dropped
 * into a [Div] without arithmetic. A percentage child cannot be measured before the box has a width
 * of its own, so such a child contributes its preferred width to that content size.
 */
class FlexBox : ComponentDrawable<FlexBox>() {

    /** The slot the flow handed out to each child in the last pass. */
    private var slots: Map<Component<*>, Slot> = emptyMap()

    override fun buildNode(): Node = FlowPane()

    /**
     * Flows [targets] through [box] before they are placed.
     *
     * The whole child list is handed in even for a partial pass — a single child cannot be placed
     * into a line without knowing where the lines before it ended.
     */
    override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        this.slots = arrange(box, targets)
    }

    /** The slot the last [beginPass] computed, so a child stays where the flow put it. */
    override fun slotFor(child: Component<*>, box: ContentBox): Slot =
        this.slots[child] ?: Slot(box.offsetX, box.offsetY)

    /**
     * Walks [children] through [box] and returns one slot per child.
     *
     * One pass is enough: sizes are measured up front, so a line break never changes the size of a
     * child that comes after it.
     */
    private fun arrange(box: ContentBox, children: List<Component<*>>): Map<Component<*>, Slot> {
        val gapX = this.layoutSpec.hPadding.resolve(box.width)
        val gapY = this.layoutSpec.vPadding.resolve(box.height)
        val right = box.offsetX + box.width

        val result = LinkedHashMap<Component<*>, Slot>(children.size)
        var lineX = box.offsetX
        var lineY = box.offsetY
        var lineHeight = 0.0

        for (child in children) {
            val size = measure(child, box)
            val fits = lineX == box.offsetX || lineX + size.width <= right
            if (!fits) {
                lineX = box.offsetX
                lineY += lineHeight + gapY
                lineHeight = 0.0
            }
            result[child] = Slot(lineX, lineY)
            lineX += size.width + gapX
            lineHeight = maxOf(lineHeight, size.height)
        }
        return result
    }

    /**
     * The space [child] takes up in the flow.
     *
     * A child that names both a `width` and a `height` is measured from its own layout block alone —
     * its node is never touched, so a box that was never rendered still knows its flow. When either
     * size is missing the node is asked for it: a wrapped label, for instance, is taller the
     * narrower it is, so the height is asked for at the width the child ended up with, exactly as a
     * plain container does it.
     */
    private fun measure(child: Component<*>, box: ContentBox): Size {
        val spec = child.layoutSpec
        val declaredWidth = spec.width
        val declaredHeight = spec.height
        if (declaredWidth != null && declaredHeight != null) {
            return Size(declaredWidth.resolve(box.width), declaredHeight.resolve(box.height))
        }
        val node = child.node
        val width = declaredWidth?.resolve(box.width) ?: preferredWidth(node)
        val height = declaredHeight?.resolve(box.height) ?: node.prefHeight(width)
        return Size(width, height)
    }

    /** The width [node] asks for when its slot names none. */
    private fun preferredWidth(node: Node): Double =
        if (node is Region) node.prefWidth(-1.0) else node.layoutBounds.width

    /**
     * What this box wants to be when nothing else decides: a single line of children.
     *
     * The insets and the gaps are part of it, so a padded row is as large as the room it keeps
     * clear. A child sized in pixels contributes that size; a child sized in percent cannot be
     * measured before the box has a width, so it contributes the preferred width of its node.
     */
    private fun contentWidth(): Double {
        val children = this.children
        val inset = this.layoutSpec.hPadding.resolve(0.0)
        if (children.isEmpty()) return inset
        var total = 0.0
        for ((index, child) in children.withIndex()) {
            total += child.layoutSpec.width?.resolve(0.0) ?: preferredWidth(child.node)
            if (index > 0) total += inset
        }
        return total + inset
    }

    /** What this box wants to be when nothing else decides: one line, as tall as its tallest child. */
    private fun contentHeight(): Double {
        val children = this.children
        val inset = this.layoutSpec.vPadding.resolve(0.0)
        if (children.isEmpty()) return inset
        var tallest = 0.0
        for (child in children) {
            val node = child.node
            val width = child.layoutSpec.width?.resolve(contentWidth()) ?: preferredWidth(node)
            val height = child.layoutSpec.height?.resolve(0.0) ?: node.prefHeight(width)
            tallest = maxOf(tallest, height)
        }
        return tallest + inset
    }

    /**
     * The pane of the box, reporting the size of one line as the size it prefers.
     *
     * A [Pane] sizes itself from its *managed* children, and the children of a surface are placed
     * by the library instead, so the preferred size is answered here — that is what lets a flex box
     * without a `width` and `height` of its own wrap around its content.
     */
    private inner class FlowPane : Pane() {

        init {
            isPickOnBounds = true
        }

        override fun computePrefWidth(height: Double): Double = contentWidth()

        override fun computePrefHeight(width: Double): Double = contentHeight()

        override fun computeMinWidth(height: Double): Double = computePrefWidth(height)

        override fun computeMinHeight(width: Double): Double = computePrefHeight(width)
    }
}
