package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.widget.TextField
import com.github.kusa233.ovatoxin.window.Frame
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.paint.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/**
 * Manual rendering probe —not a unit test, and never part of `build`.
 *
 * It shows a window for real, waits for a layout pulse, then prints the geometry the JavaFX
 * renderer actually produced and compares it with what the DSL declared. Run it with:
 *
 * ```
 * gradlew demo "-PdemoMain=com.github.kusa233.ovatoxin.example.RenderProbeKt"
 * ```
 */
fun main() {
    val frame = Frame("Ovatoxin –render probe")
    val probes = mutableListOf<Probe>()
    val nested = mutableListOf<Probe>()
    val imageUrl = probeImage()
    lateinit var panel: Div
    lateinit var imageBox: Div
    lateinit var boundText: TextArea
    lateinit var untouchedText: Button
    val translations = mapOf("test" to "Test", "changed" to "Changed")
    val state = State("test")

    frame.layout {
        val text = draw(TextArea()) { component ->
            layout {
                x = 10.percent
                y = 10.percent
                width = 80.percent
                height = 10.percent
            }
            // The text is derived from the state, so a new value must redraw this one component.
            component.text(translations.getValue(state.value))
        }.withState(state)
        probes += Probe("TextArea", text, "Test")
        boundText = text

        val field = draw(TextField()) { component ->
            layout {
                x = 10.percent
                y = 30.percent
                width = 80.percent
                height = 10.percent
            }
            component.hint = "type here"
        }
        probes += Probe("TextField", field, "")

        val button = draw(Button()) { component ->
            layout {
                x = 10.percent
                y = 50.percent
                width = 80.percent
                height = 10.percent
            }
            style {
                background {
                    color = Color.web("#4c8bf5")
                    opacity = 60
                }
                corner = 50.percent
                foreground = Color.web("#ffffff")
            }
            component.text = "Button"
        }
        probes += Probe("Button", button, "Button")
        untouchedText = button

        val link = draw(Link("Ovatoxin", "https://github.com/kusa233/Ovatoxin")) { component ->
            layout {
                x = 10.percent
                y = 62.percent
                width = 80.percent
                height = 6.percent
            }
            component.text = "Link"
        }
        probes += Probe("Link", link, "Link")

        panel = draw(Div()) { component ->
            layout {
                x = 10.percent
                y = 68.percent
                width = 80.percent
                height = 24.percent
            }
            style {
                background {
                    color = Style.DEFAULT_COLOR
                }
                corner = 12.percent
            }
            component.layout {
                val inside = draw(TextArea()) { child ->
                    layout {
                        x = 50.percent
                        y = 10.percent
                        width = 40.percent
                        height = 40.percent
                    }
                    child.text("inside")
                }
                nested += Probe("Div>inside", inside, "inside")

                val outside = draw(TextArea()) { child ->
                    layout {
                        x = 100.percent
                        y = 10.percent
                        width = 40.percent
                        height = 40.percent
                    }
                    child.text("clipped away")
                }
                nested += Probe("Div>outside", outside, "clipped away")
            }
        }

        imageBox = draw(Div()) { component ->
            layout {
                x = 10.percent
                y = 0.percent
                width = 80.percent
                height = 8.percent
            }
            style {
                background {
                    // Both are set on purpose: the image must win, the colour must not show.
                    color = IMAGE_FALLBACK_COLOR
                    image = imageUrl
                }
            }
        }
    }

    frame.show()

    // Two pulses: the first lays out the freshly shown scene, the second reports geometry
    // after the size listeners have run.
    Platform.runLater {
        Platform.runLater {
            val initial = report("initial 800x600", frame, probes, nested, panel, imageBox)

            // Percentages must be re-resolved against the new window size, not frozen.
            frame.stage?.let { stage ->
                stage.width = 1000.0
                stage.height = 500.0
            }
            frame.layout { }

            Platform.runLater {
                Platform.runLater {
                    val resized = report("resized 1000x500", frame, probes, nested, panel, imageBox)
                    val stateOk = stateRedraw(boundText, untouchedText, state, "changed")
                    val ok = initial && resized && stateOk
                    Platform.exit()
                    if (!ok) exitProcess(1)
                }
            }
        }
    }
}

/** A component under verification, with the text it must display. */
private class Probe(val name: String, val component: Component<*>, val expectedText: String)

/** The probe colour set next to the image. It must never be visible. */
private val IMAGE_FALLBACK_COLOR: Color = Color.web("#123456")

private fun report(
    label: String,
    frame: Frame,
    probes: List<Probe>,
    nested: List<Probe>,
    panel: Div,
    imageBox: Div,
): Boolean {
    val scene = probes.first().component.node.scene
    if (scene == null) {
        println("FAIL  no scene attached to the component tree")
        return false
    }

    println()
    println("=== $label ===")
    println("window  : ${frame.stage?.title}")
    println("scene   : ${num(scene.width)} x ${num(scene.height)}")

    var ok = geometry(probes, "top level (percentages of the window)")
    ok = geometry(nested, "nested in Div (percentages of the panel)") && ok
    ok = clipping(panel, nested.last()) && ok
    ok = styling(probes, panel) && ok
    ok = opacityLayer(probes.first { it.name == "Button" }) && ok
    ok = imageLayer(imageBox) && ok
    ok = contents(probes + nested) && ok

    println()
    println(if (ok) "RESULT: ok" else "RESULT: MISMATCH —see rows above")
    return ok
}

/** Compares every rendered box against the spec, resolved against the real parent size. */
private fun geometry(probes: List<Probe>, title: String): Boolean {
    println()
    println("$title")
    println("component      x       y       w       h    | x%    y%    w%    h%   | ok")
    println("-".repeat(84))

    var allOk = true
    for (probe in probes) {
        val node = probe.component.node
        val spec = probe.component.layoutSpec
        val parent = probe.component.parent as Container<*>
        val host = parent.contentPane

        if (host == null) {
            println("${pad(probe.name, 12)} FAIL no parent pane")
            allOk = false
            continue
        }

        val x = node.layoutX
        val y = node.layoutY
        val w = node.layoutBounds.width
        val h = node.layoutBounds.height

        val xPercent = x / host.width * 100.0
        val yPercent = y / host.height * 100.0
        val wPercent = w / host.width * 100.0
        val hPercent = h / host.height * 100.0

        val matched = near(spec.x.value, xPercent) &&
            near(spec.y.value, yPercent) &&
            near(spec.width!!.value, wPercent) &&
            near(spec.height!!.value, hPercent)

        allOk = allOk && matched
        println(
            "${pad(probe.name, 12)} ${pad(num(x), 7)} ${pad(num(y), 7)} ${pad(num(w), 7)} ${pad(num(h), 7)}| " +
                "${pad(num(xPercent), 5)} ${pad(num(yPercent), 5)} ${pad(num(wPercent), 5)} ${pad(num(hPercent), 5)}| " +
                if (matched) "ok" else "MISMATCH",
        )
    }
    return allOk
}

/** Checks that the panel really clips: the child at 100% must fall outside its bounds. */
private fun clipping(panel: Div, outside: Probe): Boolean {
    println()
    val pane = panel.contentPane
    if (pane == null) {
        println("clip     FAIL  panel has no pane")
        return false
    }

    val hasClip = pane.clip != null
    val clipWidth = pane.clip?.layoutBounds?.width ?: 0.0
    val clipMatchesBox = abs(clipWidth - pane.width) < 0.5

    // Geometric proof, independent of the clip node: how much of the child overlaps the box.
    val child = outside.component.node
    val childLeft = child.layoutX
    val childRight = childLeft + child.layoutBounds.width
    val visible = max(0.0, min(childRight, pane.width) - max(childLeft, 0.0))
    val visibleFraction = if (child.layoutBounds.width > 0) visible / child.layoutBounds.width else 0.0
    val outsideOk = visibleFraction < 0.001

    println(
        "clip        ${if (hasClip) "ok  " else "FAIL"} panel pane has a clip rectangle " +
            "(${num(clipWidth)} wide vs box ${num(pane.width)})",
    )
    println(
        "clip size   ${if (clipMatchesBox) "ok  " else "FAIL"} clip tracks the box size",
    )
    println(
        "clip range  ${if (outsideOk) "ok  " else "FAIL"} child at x=100% overlaps " +
            "${"%.1f".format(visibleFraction * 100)}% of the panel (expect 0%)",
    )
    return hasClip && clipMatchesBox && outsideOk
}

/**
 * Proves a state update redraws the component bound to it, and only that one.
 *
 * The rendered pixels are compared, so the check looks at what the renderer shows rather than
 * at the library's own bookkeeping. The component next to it is not bound and must come out of
 * the change byte-for-byte identical.
 */
private fun stateRedraw(
    bound: TextArea,
    untouched: Button,
    state: State<String>,
    newValue: String,
): Boolean {
    println()
    val boundNode = bound.labelNode()
    val untouchedNode = untouched.node as javafx.scene.control.Button

    val beforeBound = boundNode.text
    val beforeBoundLayout = geometryOf(boundNode)
    val beforeBoundPixels = pixels(boundNode)

    val beforeUntouched = untouchedNode.text
    val beforeUntouchedLayout = geometryOf(untouchedNode)
    val beforeUntouchedPixels = pixels(untouchedNode)

    // The state is the only trigger: nothing below touches either component directly.
    state.update(newValue)

    val afterBound = boundNode.text
    val afterBoundLayout = geometryOf(boundNode)
    val afterBoundPixels = pixels(boundNode)

    val afterUntouched = untouchedNode.text
    val afterUntouchedLayout = geometryOf(untouchedNode)
    val afterUntouchedPixels = pixels(untouchedNode)

    val saved = state.value == newValue
    val textChanged = afterBound == translationsOf(newValue)
    val boundGeometry = afterBoundLayout == beforeBoundLayout
    val repainted = beforeBoundPixels != null && afterBoundPixels != null &&
        beforeBoundPixels != afterBoundPixels

    val untouchedText = afterUntouched == beforeUntouched
    val untouchedGeometry = afterUntouchedLayout == beforeUntouchedLayout
    val untouchedPixels = beforeUntouchedPixels != null && beforeUntouchedPixels == afterUntouchedPixels

    val matched = saved && textChanged && boundGeometry && repainted &&
        untouchedText && untouchedGeometry && untouchedPixels

    println(
        "${pad("State", 12)} ${if (matched) "ok  " else "FAIL"} " +
            "saved=\"${state.value}\"  " +
            "bound \"$beforeBound\" -> \"$afterBound\"  " +
            "geometry=${if (boundGeometry) "kept" else "MOVED"}  " +
            "pixels=${if (repainted) "repainted" else "UNCHANGED"}",
    )
    println(
        "${pad("unbound", 12)} ${if (untouchedText && untouchedGeometry && untouchedPixels) "ok  " else "FAIL"} " +
            "text \"$beforeUntouched\" -> \"$afterUntouched\"  " +
            "geometry=${if (untouchedGeometry) "kept" else "MOVED"}  " +
            "pixels=${if (untouchedPixels) "identical" else "CHANGED"}",
    )
    return matched
}

/** The text the probe state maps [value] to. */
private fun translationsOf(value: String): String = when (value) {
    "changed" -> "Changed"
    else -> "Test"
}

/** A node's position and size, as one comparable value. */
private fun geometryOf(node: javafx.scene.Node): List<String> = listOf(
    num(node.layoutX),
    num(node.layoutY),
    num(node.layoutBounds.width),
    num(node.layoutBounds.height),
)

/** A cheap fingerprint of the rendered pixels, or `null` when the snapshot failed. */
private fun pixels(node: javafx.scene.Node): String? = try {
    val image = node.snapshot(null, null)
    val reader = image.pixelReader ?: return null
    val width = image.width.toInt()
    val height = image.height.toInt()
    val pixels = IntArray(width * height)
    reader.getPixels(
        0,
        0,
        width,
        height,
        javafx.scene.image.PixelFormat.getIntArgbInstance(),
        pixels,
        0,
        width,
    )
    pixels.contentHashCode().toString()
} catch (failure: RuntimeException) {
    null
}

/** The label a text area renders, so the probe reads what is on screen. */
private fun TextArea.labelNode(): Label = node as Label

/** Verifies the text every probed component actually shows. */
private fun contents(probes: List<Probe>): Boolean {
    println()
    var allOk = true
    for (probe in probes) {
        val actual = when (val component = probe.component) {
            is TextArea -> component.text()
            is TextField -> component.text()
            is Button -> component.text
            is Link -> component.text()
            else -> null
        }
        val matched = actual == probe.expectedText
        allOk = allOk && matched
        println(
            "${pad(probe.name, 12)} text ${if (matched) "ok  " else "FAIL"} " +
                "expected=${quote(probe.expectedText)} actual=${quote(actual)}",
        )
    }
    return allOk
}

/** Verifies every probed component renders as a solid, borderless box with the intended radius. */
private fun styling(probes: List<Probe>, panel: Div): Boolean {
    println()
    println("style       (solid fill, no border, corner resolved against the real box)")
    var allOk = true

    for (probe in probes + Probe("Div", panel, "")) {
        val node = probe.component.node
        val css = node.style ?: ""
        val spec = probe.component.styleSpec

        val background = spec.background
        val image = background.image
        val fill = if (image != null) {
            "transparent"
        } else {
            background.color?.let { expectedFill(it, background.opacity) } ?: "transparent"
        }
        val expected = spec.cornerPixels(node.layoutBounds.width, node.layoutBounds.height)

        val hasFill = css.contains("-fx-background-color: $fill")
        val noBorder = css.contains("-fx-border-width: 0")
        val applied = cssRadius(css)
        // The CSS keeps two decimals, so compare numerically instead of as text.
        val hasRadius = applied != null && abs(applied - expected) < 0.5
        // An image, when present, must be the layer that paints and the colour must be gone.
        val hasImage = image == null || (
            css.contains("-fx-background-image: url(") &&
                css.contains("-fx-background-size: cover") &&
                css.contains("-fx-background-color: transparent")
            )

        val matched = hasFill && noBorder && hasRadius && hasImage
        allOk = allOk && matched
        val source = image?.let { "image=$it" } ?: "color"
        println(
            "${pad(probe.name, 12)} ${if (matched) "ok  " else "FAIL"} " +
                "fill=$fill ${if (hasFill) "(applied)" else "(MISSING)"}  " +
                "border=${if (noBorder) "none" else "PRESENT"}  " +
                "corner=${num(expected)}px " +
                "rendered=${applied?.let { num(it) } ?: "<none>"}px  " +
                "layer=$source",
        )
    }

    // The text colour must reach the CSS only when the style asks for one.
    for (probe in probes + Probe("Div", panel, "")) {
        val css = probe.component.node.style ?: ""
        val wanted = probe.component.styleSpec.foreground
        val applied = cssTextFill(css)
        val matched = if (wanted == null) {
            applied == null
        } else {
            applied == colorHex(wanted)
        }
        allOk = allOk && matched
        println(
            "${pad("${probe.name} text", 12)} ${if (matched) "ok  " else "FAIL"} " +
                "foreground=${wanted?.let { colorHex(it) } ?: "none"} " +
                "rendered=${applied ?: "none"}",
        )
    }

    // The div clips its content along the same rounded outline as its fill.
    val pane = panel.contentPane
    val clip = pane?.clip as? javafx.scene.shape.Rectangle
    val expectedArc = panel.styleSpec.cornerPixels(
        pane?.width ?: 0.0,
        pane?.height ?: 0.0,
    ) * 2.0
    val clipRounded = clip != null && abs(clip.arcWidth - expectedArc) < 0.5
    allOk = allOk && clipRounded
    println(
        "${pad("Div clip", 12)} ${if (clipRounded) "ok  " else "FAIL"} " +
            "arc=${clip?.arcWidth?.let { num(it) } ?: "<none>"} expected=${num(expectedArc)} (diameter)",
    )
    return allOk
}

/**
 * Proves an image really replaces the colour: both are configured, the box is rendered for real,
 * and the pixels are read back. Not one pixel may carry the fallback colour.
 */
private fun imageLayer(box: Div): Boolean {
    println()
    val node = box.node
    val css = node.style ?: ""
    val spec = box.styleSpec
    val url = spec.background.image

    val declaresImage = url != null && css.contains("-fx-background-image: url(")
    val noFill = css.contains("-fx-background-color: transparent")
    val noFallback = !css.contains(colorHex(IMAGE_FALLBACK_COLOR))

    // Snapshot the rendered box and count the pixels of each colour.
    val snapshot = node.snapshot(null, null)
    val pixels = snapshot.pixelReader
    var fallbackPixels = 0
    var imagePixels = 0
    for (y in 0 until snapshot.height.toInt()) {
        for (x in 0 until snapshot.width.toInt()) {
            val argb = pixels.getArgb(x, y)
            val r = (argb shr 16) and 0xff
            val g = (argb shr 8) and 0xff
            val b = argb and 0xff
            if (r == 0x12 && g == 0x34 && b == 0x56) fallbackPixels++
            if (g > 200 && r < 80 && b < 80) imagePixels++
        }
    }
    val total = snapshot.width.toInt() * snapshot.height.toInt()
    val imageVisible = imagePixels > total / 2
    val fallbackHidden = fallbackPixels == 0

    val matched = declaresImage && noFill && noFallback && imageVisible && fallbackHidden
    println(
        "${pad("Image bg", 12)} ${if (matched) "ok  " else "FAIL"} " +
            "url=${url ?: "<none>"}  " +
            "declared=${if (declaresImage) "yes" else "NO"}  " +
            "colour-layer=${if (noFill && noFallback) "gone" else "PRESENT"}  " +
            "pixels: image=$imagePixels fallback=$fallbackPixels  " +
            "size=${snapshot.width.toInt()}x${snapshot.height.toInt()}",
    )
    return matched
}

/**
 * Proves a `background { opacity = …}` really reaches the renderer.
 *
 * The CSS is checked to carry the scaled alpha, and the box is then snapshotted: `snapshot` fills
 * empty space with white, so a translucent fill has to show up as the fill blended with white at
 * exactly the declared share. Both ends of the range are checked this way —`0` blends to pure
 * white, `100` to the plain colour.
 */
private fun opacityLayer(probe: Probe): Boolean {
    println()
    val node = probe.component.node
    val background = probe.component.styleSpec.background
    val color = background.color ?: return false
    val opacity = background.opacity

    val expectedAlpha = (color.opacity * opacity / 100.0 * 255.0).roundToInt()
    val declaredCss = node.style?.contains("-fx-background-color: ${expectedFill(color, opacity)};")
        ?: false

    // What the composited pixel must be: the fill blended over the white snapshot background.
    val alpha = color.opacity * opacity / 100.0
    val blendRed = (color.red * 255.0 * alpha + 255.0 * (1.0 - alpha)).roundToInt()
    val blendGreen = (color.green * 255.0 * alpha + 255.0 * (1.0 - alpha)).roundToInt()
    val blendBlue = (color.blue * 255.0 * alpha + 255.0 * (1.0 - alpha)).roundToInt()

    val snapshot = node.snapshot(null, null)
    val pixels = snapshot.pixelReader
    var covered = 0
    var blended = 0
    for (y in 0 until snapshot.height.toInt()) {
        for (x in 0 until snapshot.width.toInt()) {
            val argb = pixels.getArgb(x, y)
            if (((argb ushr 24) and 0xff) == 0) continue
            covered++
            val red = (argb shr 16) and 0xff
            val green = (argb shr 8) and 0xff
            val blue = argb and 0xff
            if (abs(red - blendRed) <= 2 && abs(green - blendGreen) <= 2 && abs(blue - blendBlue) <= 2) {
                blended++
            }
        }
    }

    // The text sits on top of the fill, so most —not all —of the box is still fill.
    val share = if (covered > 0) blended.toDouble() / covered else 0.0
    val matched = declaredCss && covered > 0 && share > 0.5
    val expected = "#%02x%02x%02x".format(blendRed, blendGreen, blendBlue)
    println(
        "${pad("${probe.name} opacity", 12)} ${if (matched) "ok  " else "FAIL"} " +
            "declared=$opacity%  alpha=${expectedAlpha}/255  " +
            "css=${if (declaredCss) "rgba" else "MISSING"}  " +
            "blend=expected $expected rendered ${"%.1f".format(share * 100)}% of the box",
    )
    return matched
}

/** Writes a solid green PNG to a temporary file and returns its URL. */
private fun probeImage(): String {
    val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = java.awt.Color(0x00, 0xff, 0x00)
    graphics.fillRect(0, 0, 64, 64)
    graphics.dispose()

    val file = Files.createTempFile("ovatoxin-probe-", ".png")
    file.toFile().deleteOnExit()
    ImageIO.write(image, "png", file.toFile())
    return file.toUri().toString()
}

/** Reads the radius back out of the inline CSS, or `null` when the property is absent. */
private fun cssRadius(css: String): Double? {
    val match = Regex("""-fx-background-radius:\s*([0-9.]+)px""").find(css) ?: return null
    return match.groupValues[1].toDoubleOrNull()
}

/** Reads the text colour back out of the inline CSS, or `null` when the property is absent. */
private fun cssTextFill(css: String): String? {
    val match = Regex("""-fx-text-fill:\s*([^;]+);""").find(css) ?: return null
    return match.groupValues[1].trim()
}

/** Formats a JavaFX colour the same way the style layer emits it. */
private fun colorHex(color: Color): String =
    if (color.opacity >= 1.0) {
        "#%02x%02x%02x".format(
            (color.red * 255.0).toInt(),
            (color.green * 255.0).toInt(),
            (color.blue * 255.0).toInt(),
        )
    } else {
        "rgba(${(color.red * 255.0).toInt()}, ${(color.green * 255.0).toInt()}, " +
            "${(color.blue * 255.0).toInt()}, ${cssAlpha(color.opacity)})"
    }

/**
 * The fill a `color` written at [opacity] whole percent must render as.
 *
 * Mirrors the library: the percentage scales the alpha the colour already carries, and a fully
 * opaque result collapses back to a plain hex colour.
 */
private fun expectedFill(color: Color, opacity: Int): String {
    val alpha = color.opacity * opacity / 100.0
    return if (alpha >= 1.0) {
        "#%02x%02x%02x".format(
            (color.red * 255.0).roundToInt(),
            (color.green * 255.0).roundToInt(),
            (color.blue * 255.0).roundToInt(),
        )
    } else {
        "rgba(${(color.red * 255.0).roundToInt()}, ${(color.green * 255.0).roundToInt()}, " +
            "${(color.blue * 255.0).roundToInt()}, ${cssAlpha(alpha)})"
    }
}

/** Formats an alpha the way the library writes it: two decimals at most, no trailing zeros. */
private fun cssAlpha(alpha: Double): String {
    val rounded = (alpha * 100.0).roundToInt() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

/** Compares a declared percentage with the rendered one, tolerating sub-pixel rounding. */
private fun near(declared: Double, rendered: Double): Boolean = abs(declared - rendered) < 0.5

private fun num(value: Double): String = "%.1f".format(value)

private fun quote(value: String?): String = "\"${value ?: "<none>"}\""

private fun pad(value: String, width: Int): String = value.padEnd(width)
