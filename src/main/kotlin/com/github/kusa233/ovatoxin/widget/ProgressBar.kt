package com.github.kusa233.ovatoxin.widget

import com.github.kusa233.ovatoxin.component.Component
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.scene.control.ProgressBar as FxProgressBar

/**
 * A horizontal bar that fills up to show how much of a task is done.
 *
 * The value is a **fraction**, not a percentage: `0.0` is an empty bar, `1.0` is a full one, and
 * `0.4` fills two fifths of it. Write `0.4`, not `40`.
 *
 * ```kotlin
 * draw(ProgressBar(0.4)) { bar ->
 *     layout {
 *         x = 10.percent
 *         y = 20.percent
 *         width = 80.percent
 *         height = 6.px
 *     }
 *     style { corner = 3.px }
 * }
 * ```
 *
 * The bar is a plain single-track indicator: it never shows a percentage label of its own, and it
 * does not animate. Drive it from a
 * [State][com.github.kusa233.ovatoxin.state.State] to have it follow real progress — a redraw re-runs
 * the draw block, so the block has to be the thing that reads the state:
 *
 * ```kotlin
 * val progress = State(0.0)
 * val frame = Frame()
 * frame.layout {
 *     draw(ProgressBar()) { bar ->
 *         layout { width = 80.percent; height = 6.px }
 *         bar.progress = progress.value
 *     }.withState(progress)
 * }
 * progress.value = 0.5   // the bar refills itself
 * ```
 *
 * A `corner` in a [style][com.github.kusa233.ovatoxin.component.DrawScope.style] block rounds the
 * track, which is how the bar is meant to be dressed — the default fill is the same white a
 * component starts with, so a style is what makes it readable.
 */
class ProgressBar(progress: Double = 0.0) : Component<ProgressBar>() {

    /** The value this bar was created with, restored by [reset]. */
    private val defaultProgress: Double = clampProgress(progress)

    private var progressValue: Double = this.defaultProgress

    /**
     * Reads how full the bar is, as a fraction from `0.0` to `1.0`.
     *
     * Use the method or the [progress] property — they are the same value.
     */
    fun progress(): Double = this.progressValue

    /**
     * Fills the bar to [value], a fraction from `0.0` to `1.0`.
     *
     * A value outside the range is clamped rather than rejected: a bar is an indicator, and
     * over-reporting progress — `1.2` from a rounding error in a progress loop — should show a full
     * bar instead of throwing in the middle of a render.
     *
     * ```kotlin
     * bar.progress(0.5)   // halfway
     * bar.progress(2.0)   // full, not an error
     * bar.progress(-1.0)  // empty, not an error
     * ```
     *
     * A [Double.NaN] is treated as `0.0`: there is no way to render it, and an empty bar reads as
     * "not started" rather than as a broken one.
     */
    fun progress(value: Double) {
        val clamped = clampProgress(value)
        this.progressValue = clamped
        (this.builtNode as? FxProgressBar)?.progress = clamped
    }

    /** How full the bar is, as a fraction from `0.0` to `1.0`. */
    var progress: Double
        get() = this.progressValue
        set(value) = progress(value)

    /**
     * Restores the progress this bar was created with.
     *
     * The value is assigned directly instead of through [progress], so no listener sees a change
     * nobody made.
     */
    override fun reset() {
        super.reset()
        this.progressValue = this.defaultProgress
        (this.builtNode as? FxProgressBar)?.progress = this.defaultProgress
    }

    override fun buildNode(): Node {
        val bar = FxProgressBar(clampProgress(this.progressValue))
        bar.styleClass += STYLE_CLASS
        ProgressBar::class.java.getResource(STYLESHEET)?.let { bar.stylesheets += it.toExternalForm() }
        return bar
    }

    /**
     * Clips the bar to its rounded outline.
     *
     * The filled band is a region of the control's skin, drawn edge to edge whatever its own radius
     * says, so at a partial or full value it would paint square corners over a rounded bar. Clipping
     * the control to the outline [styleSpec] resolves is what keeps the band inside the corners; it
     * also mirrors JavaFX's own advice to round a progress bar this way instead of relying on the
     * skin. A square bar — the default — needs no clip at all.
     */
    override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        val bar = this.builtNode as? Region ?: return
        val radius = this.styleSpec.cornerPixels(width, height)
        if (radius <= 0.0 || width <= 0.0 || height <= 0.0) {
            bar.clip = null
            return
        }
        val outline = (bar.clip as? Rectangle) ?: Rectangle().also { bar.clip = it }
        outline.width = width
        outline.height = height
        outline.arcWidth = radius * 2.0
        outline.arcHeight = radius * 2.0
    }

    companion object {
        /** Style class every OvatoXin progress bar carries. */
        const val STYLE_CLASS: String = "ovatoxin-progress"

        /** The progress of a bar nobody configured: empty. */
        const val DEFAULT_PROGRESS: Double = 0.0

        /** Stylesheet that flattens the gradient Modena paints into the track and the band. */
        private const val STYLESHEET: String = "progressbar.css"

        /** Brings [value] into `0.0..1.0`, turning `NaN` into [DEFAULT_PROGRESS]. */
        private fun clampProgress(value: Double): Double =
            if (value.isNaN()) DEFAULT_PROGRESS else value.coerceIn(0.0, 1.0)
    }
}
