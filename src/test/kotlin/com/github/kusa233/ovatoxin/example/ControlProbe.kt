package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.widget.CheckBox
import com.github.kusa233.ovatoxin.widget.ComboBox
import com.github.kusa233.ovatoxin.widget.ProgressBar
import com.github.kusa233.ovatoxin.window.Frame
import javafx.application.Platform
import javafx.scene.control.CheckBox as FxCheckBox
import javafx.scene.control.ComboBox as FxComboBox
import javafx.scene.control.ProgressBar as FxProgressBar
import kotlin.math.abs
import kotlin.system.exitProcess

/**
 * Manual rendering probe for the form controls — not a unit test, and never part of `build`.
 *
 * The three controls are real JavaFX nodes, so they cannot be built without a toolkit and their
 * behaviour cannot be checked from a plain unit test. This probe shows them for real and then works
 * them the way a user would: it fires the action of the checkbox, picks a row in the dropdown and
 * moves the progress bar through a state, reading the resulting nodes each time.
 *
 * Run it with:
 *
 * ```
 * gradlew demo "-PdemoMain=com.github.kusa233.ovatoxin.example.ControlProbeKt"
 * ```
 */
fun main() {
    val frame = Frame("Ovatoxin – control probe")
    lateinit var bar: ProgressBar
    lateinit var box: CheckBox
    lateinit var combo: ComboBox<String>
    val reported = mutableListOf<Boolean>()
    val picked = mutableListOf<String>()

    frame.layout {
        bar = draw(ProgressBar(0.25)) { component ->
            layout {
                x = 10.percent
                y = 10.percent
                width = 80.percent
                height = 8.percent
            }
            component.progress = 0.25
        }

        box = draw(CheckBox("Remember me")) { component ->
            layout {
                x = 10.percent
                y = 30.percent
                width = 80.percent
                height = 10.percent
            }
            component.onChange { reported += it }
        }

        combo = draw(ComboBox(listOf("Alpha", "Bravo", "Charlie"))) { component ->
            layout {
                x = 10.percent
                y = 50.percent
                width = 80.percent
                height = 10.percent
            }
            component.prompt = "Pick a name"
            component.onSelectItem { picked += it }
            // Selected before the node exists; the control must show it once it is built.
            component.select(0)
        }
    }

    frame.show()

    // Two pulses, as in the other probes: one to lay the scene out, one to read it back.
    Platform.runLater {
        Platform.runLater {
            val barOk = progressBar(bar)
            val boxOk = checkBox(box, reported)
            val comboOk = comboBox(combo, picked)

            println()
            println("=== control probe ===")
            println("window : ${frame.stage?.title}")
            println("bar    : ${if (barOk) "ok  " else "FAIL"} the bar shows the fraction it was given")
            println("box    : ${if (boxOk) "ok  " else "FAIL"} a click ticks the box and reports it once")
            println("combo  : ${if (comboOk) "ok  " else "FAIL"} the dropdown shows its items and reports the picked one")
            println()
            val allOk = barOk && boxOk && comboOk
            println(if (allOk) "RESULT: ok" else "RESULT: MISMATCH")

            Platform.exit()
            if (!allOk) exitProcess(1)
        }
    }
}

/**
 * Moves the bar and reads the node back.
 *
 * The value goes through a state, so this also proves the whole binding path: a state change, the
 * draw block re-running, and the new value reaching the JavaFX control.
 */
private fun progressBar(bar: ProgressBar): Boolean {
    val node = bar.node as FxProgressBar

    val created = near(node.progress, 0.25)
    bar.progress = 0.75
    val written = near(node.progress, 0.75)

    // A style corner has to reach the node as a radius, or the bar cannot be rounded.
    bar.applyStyle { corner = 4.percent }
    val rounded = (node.style ?: "").contains("-fx-background-radius")

    bar.progress = 1.4
    val clamped = near(node.progress, 1.0) && near(bar.progress, 1.0)

    bar.reset()
    val restored = near(node.progress, 0.25)

    println()
    println("progress bar (node ${probeNum(node.layoutBounds.width)}x${probeNum(node.layoutBounds.height)})")
    println("  created : ${mark(created)} starts at 0.25")
    println("  write   : ${mark(written)} progress = 0.75 reaches the control")
    println("  corner  : ${mark(rounded)} a corner in the style block becomes a radius")
    println("  clamp   : ${mark(clamped)} 1.4 shows as a full bar instead of throwing")
    println("  reset   : ${mark(restored)} the construction value is back")
    return created && written && rounded && clamped && restored
}

/**
 * Clicks the checkbox for real and checks what the handler saw.
 *
 * `fire()` is what a click ends up calling, and the control flips its own state before it fires the
 * action — so firing is enough, and setting `isSelected` first would undo the click instead of
 * making one. That ordering is exactly what [CheckBox.performClick] relies on when it reads the
 * tick back off the node.
 */
private fun checkBox(box: CheckBox, reported: MutableList<Boolean>): Boolean {
    val node = box.node as FxCheckBox

    val caption = node.text == "Remember me"
    val unticked = !node.isSelected

    node.fire()
    val ticked = node.isSelected && box.checked
    val once = reported == listOf(true)

    node.fire()
    val untickedAgain = !node.isSelected && reported == listOf(true, false)

    box.enabled = false
    val disabled = node.isDisable
    node.fire()
    val ignored = reported == listOf(true, false)

    box.enabled = true

    println()
    println("check box (node ${probeNum(node.layoutBounds.width)}x${probeNum(node.layoutBounds.height)})")
    println("  caption : ${mark(caption)} \"${node.text}\"")
    println("  start   : ${mark(unticked)} unticked")
    println("  click   : ${mark(ticked && once)} one click reports $reported")
    println("  again   : ${mark(untickedAgain)} a second click reports the new state")
    println("  disable : ${mark(disabled && ignored)} a disabled box reports nothing")
    return caption && unticked && ticked && once && untickedAgain && disabled && ignored
}

/**
 * Picks a row in the dropdown the way a user does: by moving the selection model.
 *
 * The items are pushed through the live list afterwards, so this also covers the guard that keeps a
 * mutation from looking like a pick.
 */
private fun comboBox(combo: ComboBox<String>, picked: MutableList<String>): Boolean {
    // A star projection is enough: the probe only reads the control back, never writes a typed item
    // through the node — that would be a second write path next to the live list.
    val node = combo.node as FxComboBox<*>

    val items = node.items == listOf("Alpha", "Bravo", "Charlie")
    val shown = node.selectionModel.selectedIndex == 0 && node.value == "Alpha"
    val prompt = node.promptText == "Pick a name"
    val silent = picked.isEmpty()

    node.selectionModel.select(1)
    val reportedIndex = combo.selectedIndex == 1 && combo.selectedItem == "Bravo"
    val reportedItem = picked == listOf("Bravo")

    // A mutation is not a pick: the guard has to keep the handler quiet here.
    combo.items.add("Delta")
    val grew = node.items == listOf("Alpha", "Bravo", "Charlie", "Delta")
    val stillQuiet = picked == listOf("Bravo")

    combo.items.add(0, "Zero")
    val shifted = combo.selectedIndex == 2 && combo.selectedItem == "Bravo"
    val shiftedNode = node.selectionModel.selectedIndex == 2

    combo.items.removeAt(0)
    val back = combo.selectedIndex == 1 && node.selectionModel.selectedIndex == 1

    combo.items.removeAt(1)
    val dropped = combo.selectedIndex == ComboBox.NO_SELECTION && node.selectionModel.selectedIndex == -1
    val droppedQuietly = picked == listOf("Bravo")

    // A programmatic selection must not look like a pick either.
    combo.select(0)
    val programmatic = node.selectionModel.selectedIndex == 0 && picked == listOf("Bravo")

    println()
    println("combo box (node ${probeNum(node.layoutBounds.width)}x${probeNum(node.layoutBounds.height)})")
    println("  items   : ${mark(items)} the live list reached the control")
    println("  start   : ${mark(shown)} select(0) from the draw block is what the field shows")
    println("  prompt  : ${mark(prompt)} a prompt is set on an empty field")
    println("  pick    : ${mark(reportedIndex && reportedItem)} a row pick reports \"${picked.firstOrNull()}\"")
    println("  grow    : ${mark(grew && stillQuiet)} appending an item is not a pick")
    println("  shift   : ${mark(shifted && shiftedNode)} inserting above moves the selection down")
    println("  remove  : ${mark(back)} removing above moves it back up")
    println("  drop    : ${mark(dropped && droppedQuietly)} removing the selected item drops the selection")
    println("  silent  : ${mark(programmatic)} select(0) reports nothing")
    return items && shown && prompt && silent && reportedIndex && reportedItem && grew && stillQuiet &&
        shifted && shiftedNode && back && dropped && droppedQuietly && programmatic
}

/** Renders a check column. */
private fun mark(ok: Boolean): String = if (ok) "ok  " else "FAIL"

private fun near(actual: Double, expected: Double): Boolean = abs(actual - expected) < 0.001

private fun probeNum(value: Double): String = "%.1f".format(value)
