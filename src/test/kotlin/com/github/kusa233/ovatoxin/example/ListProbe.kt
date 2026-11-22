package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.widget.ListView
import com.github.kusa233.ovatoxin.window.Frame
import javafx.application.Platform
import javafx.scene.SnapshotParameters
import javafx.scene.control.Label
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
            println()
            val allOk = painted && afterClick && started && clicked && overridden && inserted && removed && grown &&
                afterEdit && selectedItems == listOf("Delta")
            println(if (allOk) "RESULT: ok" else "RESULT: MISMATCH")

            Platform.exit()
            if (!allOk) exitProcess(1)
        }
    }
}

/** The list colours, as the probe expects them. */
private val EXPECTED_FILL: Color = Color.web("#ffffff")
private val EXPECTED_SELECTED: Color = Color.web("#eeeeee")

/**
 * Reads the rows back out of the rendered scene and checks the fill of each one.
 *
 * A row that is not selected paints nothing at all, so the white surface of the list itself shows
 * through; the selected row carries the light grey. Both are checked against the pixels, and the
 * text of every row must still be dark.
 */
private fun paintedRows(list: ListView<String>, label: String = "rows"): Boolean {
    val box = list.node as VBox
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

/**
 * Clicks a row for real and checks that the selection follows the click.
 *
 * The event goes through the same handler the toolkit would call, so this covers the wiring between
 * a row, [ListView.select] and the [ListView.onSelect] handler.
 */
private fun clickRow(list: ListView<String>, index: Int): Boolean {
    val box = list.node as VBox
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
    val box = list.node as VBox
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

    val rows = (list.node as VBox).children.filterIsInstance<Label>()
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
    val boxes = list.node as VBox
    val selected = list.selectedItem ?: return false
    val index = list.selectedIndex
    var ok = true

    // Above the selection: "Zero" was inserted at 0, so the selection shifts up with its item.
    val aboveCount = boxes.children.size
    ok = ok && list.remove("Zero")
    ok = ok && boxes.children.size == aboveCount - 1
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

    val rows = (list.node as VBox).children.filterIsInstance<Label>()
    return ok &&
        rows.size == list.size &&
        rows.takeLast(3).map { it.text } == listOf("Grow1", "Grow2", "Grow3")
}

/** The fill a row painted, and how many dark pixels its text left behind. */
private class RowPaint(val covered: Boolean, val fill: Color, val textPixels: Int)

/**
 * Snapshots one row and reads it back.
 *
 * The snapshot is taken on a transparent backdrop, so a row with no fill of its own comes back
 * wholly transparent — which is exactly the proof that the white list surface shows through. The
 * fill is sampled to the right of the text, where the row is flat; the glyphs are counted on the
 * left, where the text sits.
 */
private fun paint(row: Label): RowPaint {
    val parameters = SnapshotParameters().apply { fill = Color.TRANSPARENT }
    val snapshot = row.snapshot(parameters, null)
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

    val flatFrom = (width * 0.55).roundToInt()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = reader.getArgb(x, y)
            val alpha = (argb ushr 24) and 0xff
            if (alpha > 0x80) {
                val r = (argb shr 16) and 0xff
                val g = (argb shr 8) and 0xff
                val b = argb and 0xff
                if (r < 0x60 && g < 0x60 && b < 0x60) text++
            }
            if (x >= flatFrom) {
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
    // A row with a fill covers its whole width; a transparent one leaves every pixel clear.
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
