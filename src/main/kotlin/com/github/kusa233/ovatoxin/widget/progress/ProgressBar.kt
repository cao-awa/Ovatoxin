package com.github.kusa233.ovatoxin.widget.progress

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.percent
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.scene.control.ProgressBar as FxProgressBar

/**
 * A horizontal bar that fills up to show how much of a task is done.
 *
 * The bar is filled by a fraction of itself, which is a percentage: `0.percent` is empty,
 * `100.percent` is full, and `40.percent` fills two fifths of it. That is the same unit every other
 * length in the library is written in, and the same [Percent] arithmetic — a progress counter
 * advances with `done += 1.percent` and stops at `done >= 100.percent`.
 *
 * ```kotlin
 * draw(ProgressBar(40.percent)) { bar ->
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
 * A [Pixel][com.github.kusa233.ovatoxin.layout.Pixel] is not a fraction of anything, so it is never
 * a progress: `bar.progress = 40.px` does not compile. Where a plain number is what is at hand the
 * fraction form still works — `ProgressBar(0.4)`, `bar.progress(0.4)`, `bar.progress()` — because a
 * fraction and a percentage describe the same fullness; it is only that one of the two is a length.
 *
 * The bar is a plain single-track indicator: it never shows a percentage label of its own, and it
 * does not animate. Drive it from a
 * [State][com.github.kusa233.ovatoxin.state.State] to have it follow real progress — a redraw re-runs
 * the draw block, so the block has to be the thing that reads the state:
 *
 * ```kotlin
 * val progress = State(Percent.ZERO)
 * val frame = Frame()
 * frame.layout {
 *     draw(ProgressBar()) { bar ->
 *         layout { width = 80.percent; height = 6.px }
 *         bar.progress = progress.value
 *     }.withState(progress)
 * }
 * progress.value = 50.percent   // the bar refills itself
 * ```
 *
 * A `corner` in a [style][com.github.kusa233.ovatoxin.component.DrawScope.style] block rounds the
 * track, which is how the bar is meant to be dressed — the default fill is the same white a
 * component starts with, so a style is what makes it readable.
 */
class ProgressBar(progress: Double = 0.0) : Component<ProgressBar>() {

    /**
     * Creates a bar that starts [percent] full: `ProgressBar(40.percent)` is `ProgressBar(0.4)`.
     *
     * An out of range percentage is clamped exactly as the fraction form clamps it.
     */
    constructor(percent: Percent) : this(percent.value / 100.0)

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

    /**
     * Fills the bar to [percent], a percentage from `0.percent` to `100.percent`.
     *
     * A bar is a fraction of itself, not a size, so only a [Percent] can be a progress: `40.percent`
     * is the same as `progress(0.4)`. A [Pixel] has nothing to be a fraction of, and the two units
     * never do arithmetic together, so the mistake is caught by the compiler instead of by a runtime
     * check.
     *
     * ```kotlin
     * bar.progress(50.percent)   // halfway
     * bar.progress(40.px)        // does not compile: a pixel is not a fraction
     * ```
     */
    fun progress(percent: Percent) {
        progress(percent.value / 100.0)
    }

    /**
     * How full the bar is, as a [Percent] from `0.percent` to `100.percent`.
     *
     * The property speaks percentages even though the underlying value is a fraction, so that a bar
     * follows the same unit every other length in the library uses — `bar.progress = 40.percent` sits
     * next to `width = 40.percent` without a unit change in between. A value out of range is clamped
     * on the way in, exactly as [progress] clamps it.
     *
     * ```kotlin
     * bar.progress = 40.percent
     * if (bar.progress >= 100.percent) bar.progress = 0.percent
     * ```
     */
    var progress: Percent
        get() = (this.progressValue * 100).percent
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
        val radius = this.styleSpec.cornerPixels(width, height, this.resolvedFontSize)
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
