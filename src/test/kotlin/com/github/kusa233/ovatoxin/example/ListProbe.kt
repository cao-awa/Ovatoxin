package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.widget.list.ListView
import com.github.kusa233.ovatoxin.window.Frame
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.control.Label
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/**
 * Manual rendering probe for the list — not a unit test, and never part of `build`.
 *
 * It shows a window for real, snapshots the rows the renderer produced and reads the pixels back, so
 * the checks below are about what is on screen rather than about the CSS the library emitted. Run it
 * with:
 *
 * ```
 * gradlew demo "-PdemoMain=com.github.kusa233.ovatoxin.example.ListProbeKt"
 * ```
 */
fun main() {
    val frame = Frame("Ovatoxin – list probe")
    lateinit var list: ListView<String>
    val selections = mutableListOf<Int>()
    val selectedItems = mutableListOf<String>()

    frame.layout {
        list = draw(ListView(listOf("Alpha", "Bravo", "Charlie", "Delta"))) { component ->
            layout {
                x = 10.percent
                y = 10.percent
                width = 40.percent
                height = 40.percent
            }
            component.onSelect { selections += it }
            component.onSelectItem { selectedItems += it }
            // Selected before the node exists; the rows must pick it up when they are built.
            component.select(1)
        }
    }

    frame.show()

    // Two pulses, as in the render probe: one to lay the scene out, one to read it back.
    Platform.runLater {
        Platform.runLater {
            val started = list.selectedItem == "Bravo"
            val painted = paintedRows(list)
            val clicked = clickRow(list, 3)
            val afterClick = paintedRows(list)
            val overridden = overriddenSelection(list)
            val inserted = insertRow(list)
            val removed = removeRow(list)
            val grown = appendRepeatedly(list)
            val afterEdit = paintedRows(list, "rows after editing")
            val scrolled = scrolling(list, frame)

            println()
            println("=== list probe ===")
            println("window   : ${frame.stage?.title}")
            println("selected : ${if (started) "ok  " else "FAIL"} the draw block's select(1) shows up as a painted row")
            println("reported : ${if (selections == listOf(3)) "ok  " else "FAIL"} $selections (the draw block's own select stays silent)")
            println("item     : ${if (selectedItems == listOf("Delta")) "ok  " else "FAIL"} $selectedItems (onSelectItem hands over the item, not the index)")
            println("click    : ${if (clicked) "ok  " else "FAIL"} a primary click on row 3 selects it")
            println("override : ${if (overridden) "ok  " else "FAIL"} a custom selected colour reaches the pixels")
            println("insert   : ${if (inserted) "ok  " else "FAIL"} a row added above the selection shifts it down and it stays selected")
            println("remove   : ${if (removed) "ok  " else "FAIL"} removing a neighbour keeps the selection, removing it drops it")
            println("grow     : ${if (grown) "ok  " else "FAIL"} repeated appends each add a row instead of replacing the last one")
            println("scroll   : ${if (scrolled) "ok  " else "FAIL"} a long list scrolls, and its bar is a flat grey with no frame")
            println()
            val allOk = painted && afterClick && started && clicked && overridden && inserted && removed && grown &&
                afterEdit && scrolled && selectedItems == listOf("Delta")
            println(if (allOk) "RESULT: ok" else "RESULT: MISMATCH")

            Platform.exit()
            if (!allOk) exitProcess(1)
        }
    }
}

/** The list colours, as the probe expects them. */
private val EXPECTED_FILL: Color = Color.web("#ffffff")
private val EXPECTED_SELECTED: Color = Color.web("#eeeeee")

/** The scroll bar of the list, as the probe expects it. */
private val SCROLLER_THUMB: Color = Color.web("#c0c0c0")
private const val SCROLLER_WIDTH: Double = 8.0

/**
 * Reads the rows back out of the rendered scene and checks the fill of each one.
 *
 * A row that is not selected paints nothing at all, so the white surface of the list itself shows
 * through; the selected row carries the light grey. Both are checked against the pixels, and the
 * text of every row must still be dark.
 */
private fun paintedRows(list: ListView<String>, label: String = "rows"): Boolean {
    val box = listBox(list)
    val rows = box.children.filterIsInstance<Label>()
    val expectedIndex = list.selectedIndex

    var allOk = rows.size == list.size
    println()
    println("$label (${rows.size} labels in a ${probeNum(box.layoutBounds.width)}x${probeNum(box.layoutBounds.height)} box)")
    println("row  y      h      selected  fill                text  ok")
    println("-".repeat(66))

    rows.forEachIndexed { index, row ->
        val painted = paint(row)
        val selected = index == expectedIndex
        val fillOk = if (selected) {
            painted.covered && near(painted.fill, EXPECTED_SELECTED)
        } else {
            !painted.covered
        }
        val textOk = painted.textPixels > 0
        val matched = fillOk && textOk
        allOk = allOk && matched

        println(
            "${probePad(index.toString(), 4)} ${probePad(probeNum(row.layoutY), 6)} " +
                "${probePad(probeNum(row.layoutBounds.height), 6)} " +
                "${probePad(selected.toString(), 9)} " +
                "${probePad(if (painted.covered) probeHex(painted.fill) else "none (list fill)", 19)} " +
                "${probePad(painted.textPixels.toString(), 5)} ${if (matched) "ok" else "FAIL"}",
        )
    }

    val cssOk = (list.node.style ?: "").contains("-fx-background-color: ${probeHex(EXPECTED_FILL)}")
    allOk = allOk && cssOk
    println("surface : ${if (cssOk) "ok  " else "FAIL"} list fill ${probeHex(EXPECTED_FILL)} declared in the node CSS")
    return allOk
}

/** The box the rows live in: the list's node is the scroll pane around it. */
private fun listBox(list: ListView<String>): VBox = (list.node as ScrollPane).content as VBox

/**
 * Clicks a row for real and checks that the selection follows the click.
 *
 * The event goes through the same handler the toolkit would call, so this covers the wiring between
 * a row, [ListView.select] and the [ListView.onSelect] handler.
 */
private fun clickRow(list: ListView<String>, index: Int): Boolean {
    val box = listBox(list)
    val row = box.children.filterIsInstance<Label>().getOrNull(index) ?: return false
    val bounds = row.layoutBounds

    row.fireEvent(
        MouseEvent(
            MouseEvent.MOUSE_CLICKED,
            bounds.width / 2.0,
            bounds.height / 2.0,
            0.0,
            0.0,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            false,
            false,
            true,
            null,
        ),
    )
    return list.selectedIndex == index
}

/** Checks that a custom selection colour really reaches the pixels. */
private fun overriddenSelection(list: ListView<String>): Boolean {
    val custom = Color.web("#cccccc")
    list.selectedBackground = custom
    val box = listBox(list)
    val row = box.children.filterIsInstance<Label>().getOrNull(list.selectedIndex) ?: return false
    return near(paint(row).fill, custom)
}

/**
 * Inserts a row above the selection and checks the list keeps the same item selected.
 *
 * The rows are recreated on a change, so this also proves the fresh rows are painted from scratch
 * rather than left as bare labels.
 */
private fun insertRow(list: ListView<String>): Boolean {
    val selected = list.selectedItem ?: return false
    val index = list.selectedIndex
    list.items.add(0, "Zero")

    val rows = listBox(list).children.filterIsInstance<Label>()
    val row = rows.getOrNull(list.selectedIndex) ?: return false
    // The selection colour may already have been overridden, so compare against what it is now.
    return list.selectedItem == selected &&
        list.selectedIndex == index + 1 &&
        rows.size == list.size &&
        rows[0].text == "Zero" &&
        near(paint(row).fill, list.selectedBackground)
}

/**
 * Removes rows above, below and the selected row itself, checking the selection at each step.
 *
 * Together these are the whole documented contract: a neighbour going away keeps the same item
 * selected, and only the selected row going away drops the selection.
 */
private fun removeRow(list: ListView<String>): Boolean {
    val rows = listBox(list).children
    val selected = list.selectedItem ?: return false
    val index = list.selectedIndex
    var ok = true

    // Above the selection: "Zero" was inserted at 0, so the selection shifts up with its item.
    val aboveCount = rows.size
    ok = ok && list.remove("Zero")
    ok = ok && rows.size == aboveCount - 1
    ok = ok && list.selectedItem == selected && list.selectedIndex == index - 1

    // Below the selection: a row appended at the end cannot disturb it.
    list.add("Echo")
    ok = ok && list.remove("Echo")
    ok = ok && list.selectedItem == selected && list.selectedIndex == index - 1

    // The selected row itself: the item it pointed at is gone, so the selection goes with it.
    val removedItem = list.selectedItem
    ok = ok && list.removeAt(list.selectedIndex) == removedItem
    ok = ok && list.selectedIndex == ListView.NO_SELECTION && list.selectedItem == null
    return ok
}

/**
 * Repeats the shape that made the list overwrite its last row: a redraw, then an append.
 *
 * A list bound to a state runs its draw block again on every change, so the block that appends a
 * row runs before the append it makes. A reset that restored the construction rows would put every
 * append back at the same index, and the row added last would be replaced instead of the list
 * growing. The rows on screen are checked as well, so this covers the rebuild too.
 */
private fun appendRepeatedly(list: ListView<String>): Boolean {
    val before = list.size
    var ok = true

    for (round in 1..3) {
        list.redraw()
        // The redraw must not have thrown away the rows added by the earlier rounds.
        ok = ok && list.size == before + round - 1
        list.add("Grow$round")
        ok = ok && list.size == before + round
        ok = ok && list.items.takeLast(round) == (1..round).map { "Grow$it" }
    }

    val rows = listBox(list).children.filterIsInstance<Label>()
    return ok &&
        rows.size == list.size &&
        rows.takeLast(3).map { it.text } == listOf("Grow1", "Grow2", "Grow3")
}

/**
 * Grows the list past its height and checks that it scrolls and that the bar is a bare grey thumb.
 *
 * The bar is read out of the live scene, not out of the stylesheet, so this covers the whole way: the
 * stylesheet reaching the pane, the bar being created at all, and the pixels it ends up painting. A
 * themed bar would paint a gradient track, a frame and two arrow buttons; a bare one paints nothing
 * but a grey thumb, which is what the transparency checks below prove pixel by pixel.
 *
 * Scrolling is measured on the rows themselves — they have to move up out of the viewport when the
 * bar is dragged to the end — because the thumb is only a skin detail.
 */
private fun scrolling(list: ListView<String>, frame: Frame): Boolean {
    val pane = list.node as ScrollPane
    val box = pane.content as VBox

    repeat(20) { list.add("Row $it") }
    // Lay the whole scene out again: the rows grew inside the pane, which its parent never saw.
    relayout(frame)

    val bar = pane.lookupAll(".scroll-bar")
        .filterIsInstance<ScrollBar>()
        .firstOrNull { it.orientation == Orientation.VERTICAL }
    val thumb = bar?.lookup(".thumb")

    println()
    println("--- scrolling ---")
    println(
        "list    : ${box.children.size} rows, ${probeNum(box.layoutBounds.height)} tall in a " +
            "${probeNum(pane.viewportBounds.height)} viewport (vmax ${probeNum(pane.vmax)})",
    )

    if (bar == null || thumb == null) {
        println("bar     : FAIL no vertical scroll bar came out of the pane")
        return false
    }

    val rowsTop = rowsTopInScene(box)

    // Drag the bar to the end: the rows have to move up and out of the viewport.
    pane.vvalue = 1.0
    relayout(frame)
    val rowsTopAtEnd = rowsTopInScene(box)

    val thumbPaint = paint(thumb)
    val thumbAtEnd = thumb.boundsInParent.minY
    // The pane's own fill does have to survive: the strip the bar sits in belongs to it.
    val surfacePaint = paint(
        pane,
        ProbeRegion(
            bar.layoutX,
            2.0,
            bar.layoutX + bar.layoutBounds.width,
            thumbAtEnd - 4.0,
        ),
    )

    val scrollsOk = pane.vmax > 0.0 && bar.isVisible
    val movedOk = rowsTopAtEnd < rowsTop - 1.0
    val widthOk = abs(bar.layoutBounds.width - SCROLLER_WIDTH) < 0.5
    val thumbOk = thumbPaint.covered && near(thumbPaint.fill, SCROLLER_THUMB)
    // Nothing but the thumb: every pixel of the bar outside it must be clear.
    val bareOk = outsideThumbIsClear(bar, thumb)
    val surfaceOk = surfacePaint.covered && near(surfacePaint.fill, EXPECTED_FILL)

    println("scrolls : ${if (scrollsOk) "ok  " else "FAIL"} vmax is above 0, so the rows do not all fit")
    println(
        "moves   : ${if (movedOk) "ok  " else "FAIL"} the rows went from y ${probeNum(rowsTop)} " +
            "to ${probeNum(rowsTopAtEnd)} when the bar was dragged to the end",
    )
    println("width   : ${if (widthOk) "ok  " else "FAIL"} the bar is ${probeNum(bar.layoutBounds.width)} wide")
    println(
        "thumb   : ${if (thumbOk) "ok  " else "FAIL"} " +
            "${if (thumbPaint.covered) probeHex(thumbPaint.fill) else "none"} thumb fill, " +
            "${probeNum(thumb.layoutBounds.height)} tall",
    )
    println("bare    : ${if (bareOk) "ok  " else "FAIL"} the bar paints nothing but its thumb: no frame, track or arrows")
    println(
        "surface : ${if (surfaceOk) "ok  " else "FAIL"} " +
            "${if (surfacePaint.covered) probeHex(surfacePaint.fill) else "none"} list fill in the bar's strip",
    )
    return scrollsOk && movedOk && widthOk && thumbOk && bareOk && surfaceOk
}

/** Runs the layout pass again, so a change made from a callback shows up in the scene. */
private fun relayout(frame: Frame) {
    frame.stage?.scene?.root?.let {
        it.applyCss()
        it.layout()
    }
}

/** Where the first row sits on screen, in scene coordinates. */
private fun rowsTopInScene(box: VBox): Double = box.localToScene(0.0, 0.0).y

/**
 * Checks that a scroll bar paints nothing but its thumb.
 *
 * The thumb's own rectangle is taken from its bounds in the bar, so this holds wherever the bar has
 * scrolled to. Every pixel around it belongs to the track or the arrow buttons, which a bare bar
 * leaves clear — that is what "no frame, no track, no arrows" looks like from the outside.
 */
private fun outsideThumbIsClear(bar: ScrollBar, thumb: Node): Boolean {
    val parameters = SnapshotParameters().apply { fill = Color.TRANSPARENT }
    val snapshot = bar.snapshot(parameters, null)
    val reader = snapshot.pixelReader
    val thumbBox = thumb.boundsInParent
    var outside = 0

    for (y in 0 until snapshot.height.toInt()) {
        for (x in 0 until snapshot.width.toInt()) {
            // A one pixel margin, because the thumb's own edges are antialiased.
            val inside = x >= thumbBox.minX - 1 && x <= thumbBox.maxX + 1 &&
                y >= thumbBox.minY - 1 && y <= thumbBox.maxY + 1
            if (inside) continue
            outside++
            val alpha = (reader.getArgb(x, y) ushr 24) and 0xff
            if (alpha > 0x10) return false
        }
    }
    // A bar with nothing around the thumb at all would mean the thumb filled it, which it cannot.
    return outside > 0
}

/** The fill a row painted, and how many dark pixels its text left behind. */
private class RowPaint(val covered: Boolean, val fill: Color, val textPixels: Int)

/** A box to sample inside a snapshot, in the node's own pixels. */
private class ProbeRegion(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

/**
 * Snapshots one node — or one stretch of it — and reads it back.
 *
 * The snapshot is taken on a transparent backdrop, so a node with no fill of its own comes back
 * wholly transparent — which is exactly the proof that a fill behind it shows through. The fill is
 * sampled to the right of the text by default, where a row is flat and no glyph dilutes the average;
 * [region] overrides that and samples exactly the given box instead.
 */
private fun paint(node: Node, region: ProbeRegion? = null): RowPaint {
    val parameters = SnapshotParameters().apply { fill = Color.TRANSPARENT }
    val snapshot = node.snapshot(parameters, null)
    val reader = snapshot.pixelReader
    val width = snapshot.width.toInt()
    val height = snapshot.height.toInt()

    var fillAlpha = 0L
    var fillRed = 0L
    var fillGreen = 0L
    var fillBlue = 0L
    var fillSamples = 0
    var opaqueSamples = 0
    var text = 0

    val fromX = (region?.minX ?: 0.0).roundToInt().coerceIn(0, width)
    val toX = (region?.maxX ?: width.toDouble()).roundToInt().coerceIn(0, width)
    val fromY = (region?.minY ?: 0.0).roundToInt().coerceIn(0, height)
    val toY = (region?.maxY ?: height.toDouble()).roundToInt().coerceIn(0, height)
    val fillFrom = if (region != null) fromX else (width * 0.55).roundToInt()

    for (y in fromY until toY) {
        for (x in fromX until toX) {
            val argb = reader.getArgb(x, y)
            val alpha = (argb ushr 24) and 0xff
            if (alpha > 0x80) {
                val r = (argb shr 16) and 0xff
                val g = (argb shr 8) and 0xff
                val b = argb and 0xff
                if (r < 0x60 && g < 0x60 && b < 0x60) text++
            }
            if (x >= fillFrom) {
                fillSamples++
                fillAlpha += alpha
                if (alpha > 0x80) {
                    opaqueSamples++
                    fillRed += (argb shr 16) and 0xff
                    fillGreen += (argb shr 8) and 0xff
                    fillBlue += argb and 0xff
                }
            }
        }
    }

    val averageAlpha = if (fillSamples == 0) 0.0 else fillAlpha.toDouble() / fillSamples / 0xff
    val fill = if (opaqueSamples > 0) {
        Color.rgb(
            (fillRed.toDouble() / opaqueSamples).roundToInt(),
            (fillGreen.toDouble() / opaqueSamples).roundToInt(),
            (fillBlue.toDouble() / opaqueSamples).roundToInt(),
        )
    } else {
        Color.TRANSPARENT
    }
    // A node with a fill covers its whole box; a transparent one leaves every pixel clear.
    return RowPaint(covered = averageAlpha > 0.98, fill = fill, textPixels = text)
}

/** Compares two colours per channel, tolerating antialiasing. */
private fun near(actual: Color, expected: Color): Boolean =
    abs(actual.red - expected.red) < 0.02 &&
        abs(actual.green - expected.green) < 0.02 &&
        abs(actual.blue - expected.blue) < 0.02

/** Formats a colour the same way the style layer does. */
private fun probeHex(color: Color): String =
    "#%02x%02x%02x".format(
        (color.red * 255.0).roundToInt(),
        (color.green * 255.0).roundToInt(),
        (color.blue * 255.0).roundToInt(),
    )

private fun probeNum(value: Double): String = "%.1f".format(value)

private fun probePad(value: String, width: Int): String = value.padEnd(width)
