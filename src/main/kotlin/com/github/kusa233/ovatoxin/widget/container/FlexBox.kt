package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.Size
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.layout.Length
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

/**
 * A drawing surface that lays its children out in lines instead of at absolute positions.
 *
 * A [Div] places every child exactly where its own `x` and `y` say. A flex box instead flows them:
 * the first child goes to the top left corner of the content box, the next one is put to its right,
 * and so on until the line is full — then a new line starts underneath, as tall as the tallest child
 * of the line it follows.
 *
 * Three properties of the box steer that flow, and they are set on the box itself rather than inside
 * a layout block:
 *
 * * [gap] — the space between two children of a line *and* between two lines;
 * * [justify] — how the room a line leaves over is shared out, from left to right;
 * * [align] — where each child sits inside its own line, from top to bottom.
 *
 * ```kotlin
 * frame.layout {
 *     draw(FlexBox()) { row ->
 *         layout {
 *             width = 100.percent
 *             hPadding = 8.px
 *             vPadding = 8.px
 *         }
 *         row.gap = 6.px
 *         row.justify = Justify.SPACE_BETWEEN
 *         row.align = Align.CENTER
 *         row.layout {
 *             draw(Button()) { it.text("One") }
 *             draw(Button()) { it.text("Two") }
 *             draw(Button()) { it.text("Three") }
 *         }
 *     }
 * }
 * ```
 *
 * `hPadding` and `vPadding` keep their own meaning here: they are the inset that pushes the children
 * away from the edges of the box, and they do not space the children apart — that is what [gap] is
 * for. A padded row is still as large as the room it keeps clear, and a panel that draws a
 * background around its children is written the same way.
 *
 * The flow is decided by the widths the children resolve to, so a child still says how big it is in
 * its own `layout` block — `width = 30.percent` of the content box, or `width = 120.px`. A child
 * that says nothing is measured at its preferred size, which is what makes a row of buttons come
 * out right without any arithmetic.
 *
 * Neither [gap] nor [justify] resizes a child: the space they talk about is only ever *between*
 * children, never inside one. A line that overflows the content box therefore leaves nothing to
 * share out, and packs its children from the start exactly as if [justify] were [Justify.START].
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
open class FlexBox : ComponentDrawable<FlexBox>() {

    /** The slot the flow handed out to each child in the last pass. */
    private var slots: Map<Component<*>, Slot> = emptyMap()

    private var gapValue: Length = Length.ZERO
    private var justifyValue: Justify = Justify.START
    private var alignValue: Align = Align.START

    /**
     * Reads the space this box keeps between its children.
     *
     * It is used twice: once between two children of the same line, and once between two lines. Like
     * the padding it resolves against the side it separates — the width for the space within a line,
     * the height for the space between two of them — so `gap = 2.percent` is a space that stays
     * proportional in both directions.
     */
    fun gap(): Length = this.gapValue

    /** Replaces the space this box keeps between its children. */
    fun gap(value: Length) {
        this.gapValue = value
    }

    /** The space this box keeps between two children of a line, and between two lines. Defaults to `0.px`. */
    var gap: Length
        get() = this.gapValue
        set(value) = gap(value)

    /** Reads how this box shares out the room a line leaves over. */
    fun justify(): Justify = this.justifyValue

    /** Replaces how this box shares out the room a line leaves over. */
    fun justify(value: Justify) {
        this.justifyValue = value
    }

    /** How this box shares out the room a line leaves over. Defaults to [Justify.START]. */
    var justify: Justify
        get() = this.justifyValue
        set(value) = justify(value)

    /** Reads where this box puts a child inside the line it was given. */
    fun align(): Align = this.alignValue

    /** Replaces where this box puts a child inside the line it was given. */
    fun align(value: Align) {
        this.alignValue = value
    }

    /** Where this box puts a child inside the line it was given. Defaults to [Align.START]. */
    var align: Align
        get() = this.alignValue
        set(value) = align(value)

    override fun buildNode(): Node = FlowPane()

    /**
     * Restores the flow of a fresh box: no gap, children packed at the start of their line, and every
     * child sitting at the top of it.
     *
     * The three values belong to the box itself rather than to its layout block, so [Component.reset]
     * does not know about them and they have to be put back here — otherwise a redraw would keep the
     * gap and the distribution of the pass before it.
     */
    override fun reset() {
        super.reset()
        this.gapValue = Length.ZERO
        this.justifyValue = Justify.START
        this.alignValue = Align.START
    }

    /**
     * Flows [targets] through [box] before they are placed.
     *
     * The whole child list is handed in even for a partial pass — a single child cannot be placed
     * into a line without knowing where the lines before it ended.
     */
    override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        // A flow is a property of the whole child list: even a pass that repaints one child has
        // to know where the lines before it end. Measuring only that child would put it at the
        // top of the box, which is exactly the jump a state-bound child used to take.
        this.slots = arrange(box, this.children)
    }

    /** The slot the last [beginPass] computed, so a child stays where the flow put it. */
    override fun slotFor(child: Component<*>, box: ContentBox): Slot =
        this.slots[child] ?: Slot(box.offsetX, box.offsetY)

    /**
     * Walks [children] through [box] and returns one slot per child.
     *
     * Sizes are measured up front, so a line break never changes the size of a child that comes after
     * it; what does need the whole line is [justify], which shares out the room only a finished line
     * knows it has left over, and [align], which needs the line's height. The children are therefore
     * gathered into a line first and only then given their slots.
     */
    private fun arrange(box: ContentBox, children: List<Component<*>>): Map<Component<*>, Slot> {
        val gapX = this.gapValue.resolve(box.width, this.resolvedFontSize)
        val gapY = this.gapValue.resolve(box.height, this.resolvedFontSize)
        val right = box.offsetX + box.width

        val result = LinkedHashMap<Component<*>, Slot>(children.size)
        val line = mutableListOf<Placed>()
        var cursor = box.offsetX
        var lineY = box.offsetY
        var wrapped = false

        for (child in children) {
            val size = measure(child, box)
            // A child that is on a line of its own is never moved on: it may overflow, it may not wrap.
            val fits = line.isEmpty() || cursor + size.width <= right
            if (!fits) {
                wrapped = true
                lineY += placeLine(line, box, gapX, lineY, result, onlyLine = false) + gapY
                line.clear()
                cursor = box.offsetX
            }
            line.add(Placed(child, size))
            cursor += size.width + gapX
        }
        placeLine(line, box, gapX, lineY, result, onlyLine = !wrapped)
        return result
    }

    /**
     * Gives every child of one finished [line] its slot and answers how tall the line turned out.
     *
     * The line is as tall as its tallest child, and [align] decides what a shorter child does with the
     * room that leaves above and below it. [justify] moves the whole line along the main axis and
     * widens the space between the children; neither ever changes a child's own size.
     *
     * A padded leaf is the exception to [align]: it is given the whole band as its slot so it can
     * centre itself in it and keep the space above and below equal, giving height up only when it
     * does not fit — see [com.github.kusa233.ovatoxin.layout.insetBox]. A flow of one line hands the
     * whole content height down as that band (as in a flex row that stretches to its container);
     * once it wraps, each line is its own band, as tall as the tallest child on it.
     */
    private fun placeLine(
        line: List<Placed>,
        box: ContentBox,
        gap: Double,
        lineY: Double,
        result: MutableMap<Component<*>, Slot>,
        onlyLine: Boolean,
    ): Double {
        if (line.isEmpty()) return 0.0
        val lineHeight = line.maxOf { it.size.height }
        val used = line.sumOf { it.size.width } + gap * (line.size - 1)
        val free = (box.width - used).coerceAtLeast(0.0)
        var x = box.offsetX + leadingRoom(free, line.size)
        val between = gap + sharedRoom(free, line.size)
        val bandTop = if (onlyLine) box.offsetY else lineY
        val bandHeight = if (onlyLine) box.height else lineHeight

        for (placed in line) {
            val child = placed.child
            val paddedLeaf = child !is Container<*> && child.layoutSpec.vPadding != Length.ZERO
            result[child] = if (paddedLeaf) {
                Slot(x, bandTop, bandHeight)
            } else {
                Slot(x, lineY + crossRoom(lineHeight - placed.size.height))
            }
            x += placed.size.width + between
        }
        return lineHeight
    }

    /** The room [justify] keeps clear before the first child of a line. */
    private fun leadingRoom(free: Double, count: Int): Double = when (this.justifyValue) {
        Justify.START, Justify.SPACE_BETWEEN -> 0.0
        Justify.CENTER -> free / 2.0
        Justify.END -> free
        // Half a share per side: one share per child, split evenly.
        Justify.SPACE_AROUND -> free / (2.0 * count)
    }

    /** The room [justify] adds to [gap] between two children of a line. */
    private fun sharedRoom(free: Double, count: Int): Double = when (this.justifyValue) {
        Justify.START, Justify.CENTER, Justify.END -> 0.0
        Justify.SPACE_BETWEEN -> if (count > 1) free / (count - 1) else 0.0
        // Two half shares meet between two children, so the whole share goes there.
        Justify.SPACE_AROUND -> free / count
    }

    /** The room [align] keeps clear above a child, out of the [room] its line leaves over it. */
    private fun crossRoom(room: Double): Double = when (this.alignValue) {
        Align.START -> 0.0
        Align.CENTER -> room / 2.0
        Align.END -> room
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
        // A child measures its own em against its own font, inherited from this box.
        val fontSize = child.resolvedFontSize
        if (declaredWidth != null && declaredHeight != null) {
            return Size(
                declaredWidth.resolve(box.width, fontSize),
                declaredHeight.resolve(box.height, fontSize),
            )
        }
        val node = child.node
        val width = declaredWidth?.resolve(box.width, fontSize) ?: preferredWidth(node)
        val height = declaredHeight?.resolve(box.height, fontSize) ?: node.prefHeight(width)
        return Size(width, height)
    }

    /** The width [node] asks for when its slot names none. */
    private fun preferredWidth(node: Node): Double =
        if (node is Region) node.prefWidth(-1.0) else node.layoutBounds.width

    /**
     * What this box wants to be when nothing else decides: a single line of children.
     *
     * The insets and the gaps are part of it, so a padded row is as large as the room it keeps clear
     * and a row with a [gap] is as wide as the children plus the space it puts between them. A child
     * sized in pixels or in `em` contributes that size; a child sized in percent cannot be measured
     * before the box has a width, so it contributes the preferred width of its node — and a
     * percentage [gap] contributes nothing, for the same reason.
     */
    private fun contentWidth(): Double {
        val children = this.children
        val inset = this.layoutSpec.hPadding.resolve(0.0, this.resolvedFontSize)
        if (children.isEmpty()) return inset * 2.0
        val gap = this.gapValue.resolve(0.0, this.resolvedFontSize)
        var total = inset * 2.0 + gap * (children.size - 1)
        for (child in children) {
            val width = child.layoutSpec.width?.resolve(0.0, child.resolvedFontSize)
            total += width ?: preferredWidth(child.node)
        }
        return total
    }

    /** What this box wants to be when nothing else decides: one line, as tall as its tallest child. */
    private fun contentHeight(): Double {
        val children = this.children
        val inset = this.layoutSpec.vPadding.resolve(0.0, this.resolvedFontSize)
        if (children.isEmpty()) return inset * 2.0
        var tallest = 0.0
        for (child in children) {
            val node = child.node
            val fontSize = child.resolvedFontSize
            val width = child.layoutSpec.width?.resolve(contentWidth(), fontSize) ?: preferredWidth(node)
            val height = child.layoutSpec.height?.resolve(0.0, fontSize) ?: node.prefHeight(width)
            tallest = maxOf(tallest, height)
        }
        return tallest + inset * 2.0
    }

    /** One child of a line, with the size it was measured at. */
    private data class Placed(val child: Component<*>, val size: Size)

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
