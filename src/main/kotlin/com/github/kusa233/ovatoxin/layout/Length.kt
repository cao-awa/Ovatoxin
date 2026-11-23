package com.github.kusa233.ovatoxin.layout

/**
 * A length: a size or an offset, measured either against the size of the parent container or in
 * absolute device independent pixels.
 *
 * The two ways of measuring are two types — [Percent] and [Pixel] — so a relative length can never
 * be mistaken for an absolute one. The DSL turns a number into one of them:
 *
 * ```kotlin
 * x = 10.percent   // a tenth of the parent width, a Percent
 * y = 10.px        // ten device independent pixels, a Pixel
 * ```
 *
 * A percentage cannot become a pixel count before the parent size is known, which is what
 * [resolve] and [toPixels] are for: they hand in that size and get pixels back.
 *
 * Arithmetic belongs to the units and not to this type: [Percent] and [Pixel] each define their own
 * operators, and each of them accepts its own kind only. A length is therefore only ever added,
 * subtracted, scaled or compared inside the unit it was written in, and the compiler is what keeps
 * it that way:
 *
 * ```kotlin
 * var done = Percent.ZERO
 * while (done < Length.FULL) done += 1.percent
 *
 * 10.px * 2          // 20.px
 * 100.percent / 25.percent   // 4.0
 * 10.percent + 4.px  // does not compile
 * ```
 *
 * Mixing the two is a mistake rather than something to guess at, because only the parent size could
 * settle it. When a combination really is needed, freeze the relative side first —
 * `10.percent.toPixels(width)` — so both are pixel lengths.
 *
 * This is the type the slots of a [Layout] and of a
 * [style][com.github.kusa233.ovatoxin.style.Style] store, so one slot can accept either unit.
 */
sealed class Length {

    /** The raw amount: a percentage of the parent size, or a count of pixels. */
    abstract val value: Double

    /** `true` when this length is exactly zero, i.e. when it leaves a slot at its default. */
    val isZero: Boolean get() = this.value == 0.0

    /**
     * Resolves this length against the [available] space of the parent container, in pixels.
     *
     * A [Pixel] is already a pixel count, so [available] is ignored; a [Percent] is turned into
     * pixels with it.
     */
    fun resolve(available: Double): Double = when (this) {
        is Percent -> available * this.value / 100.0
        is Pixel -> this.value
    }

    /** Freezes this length into pixels, resolving a percentage against [available]. */
    fun toPixels(available: Double): Pixel = Pixel(resolve(available))

    companion object {
        /**
         * A zero offset — the same value as [Pixel.ZERO], which is what every position starts at.
         *
         * Zero is not a unit of its own, it is only a unit that happens to be empty: this one is a
         * pixel length and [Percent.ZERO] is a percentage one. They describe the same amount of
         * nothing, but they are not the same type, so neither can be handed to the other's
         * operators.
         */
        val ZERO: Length = Pixel.ZERO

        /** A length that fills the parent completely: `100.percent`. */
        val FULL: Percent = Percent(100.0)

        /** Creates a percentage length: `Length.percent(12.5)` is the same as `12.5.percent`. */
        fun percent(value: Double): Percent = Percent(value)

        /** Creates an absolute pixel length: `Length.pixels(4.0)` is the same as `4.px`. */
        fun pixels(value: Double): Pixel = Pixel(value)
    }
}

/**
 * A length relative to the size of the parent container: `10.percent` is a tenth of it.
 *
 * A percentage survives a resize — the same layout describes the same proportion of a window of any
 * size — which is why a layout is usually written in percentages. [Pixel] is kept for the sizes that
 * must not grow: a hairline, an icon, a fixed sidebar.
 *
 * A percentage only ever does arithmetic with another percentage. That is what makes a value safe to
 * advance in a loop, and what makes a comparison mean something:
 *
 * ```kotlin
 * 40.percent + 5.percent        // 45.percent
 * 10.percent * 2                // 20.percent
 * 100.percent / 25.percent      // 4.0
 * 50.percent >= 100.percent     // false
 * ```
 */
data class Percent internal constructor(override val value: Double) : Length() {

    /** Adds [other] percentages to this one: `40.percent + 5.percent` is `45.percent`. */
    operator fun plus(other: Percent): Percent = Percent(this.value + other.value)

    /** Subtracts [other] percentages from this one: `45.percent - 5.percent` is `40.percent`. */
    operator fun minus(other: Percent): Percent = Percent(this.value - other.value)

    /** Scales this percentage by [factor], staying a percentage. */
    operator fun times(factor: Double): Percent = Percent(this.value * factor)

    /** Scales this percentage by [factor], staying a percentage. */
    operator fun times(factor: Int): Percent = times(factor.toDouble())

    /** Splits this percentage into [divisor] equal parts, staying a percentage. */
    operator fun div(divisor: Double): Percent = Percent(this.value / divisor)

    /** Splits this percentage into [divisor] equal parts, staying a percentage. */
    operator fun div(divisor: Int): Percent = div(divisor.toDouble())

    /**
     * How many times [other] fits into this percentage: `100.percent / 25.percent` is `4.0`.
     *
     * Dividing by `Percent.ZERO` follows the usual [Double] rules and gives an infinity.
     */
    operator fun div(other: Percent): Double = this.value / other.value

    /** Flips the sign of this percentage. */
    operator fun unaryMinus(): Percent = Percent(-this.value)

    /** Orders two percentages, which is what makes `<`, `<=`, `>` and `>=` work. */
    operator fun compareTo(other: Percent): Int = this.value.compareTo(other.value)

    override fun toString(): String = "${this.value.trimmed()}%"

    companion object {
        /** A percentage of nothing: `0.percent`. */
        val ZERO = Percent(0.0)
    }
}

/**
 * A length in device independent pixels: `10.px` is ten of them, whatever the window size is.
 *
 * Pixels are the unit everything is drawn in — a percentage is resolved into pixels by
 * [Length.resolve] before anything reaches the screen — so a size that has to stay the same at every
 * window size is written in pixels.
 *
 * A pixel length only ever does arithmetic with another pixel length, under the same rules
 * [Percent] follows:
 *
 * ```kotlin
 * 10.px + 4.px        // 14.px
 * 10.px * 2           // 20.px
 * 10.px / 4.px        // 2.5
 * 10.px > 4.px        // true
 * ```
 */
data class Pixel internal constructor(override val value: Double) : Length() {

    /** Adds [other] pixel lengths to this one: `10.px + 4.px` is `14.px`. */
    operator fun plus(other: Pixel): Pixel = Pixel(this.value + other.value)

    /** Subtracts [other] pixel lengths from this one: `10.px - 4.px` is `6.px`. */
    operator fun minus(other: Pixel): Pixel = Pixel(this.value - other.value)

    /** Scales this pixel length by [factor], in pixels. */
    operator fun times(factor: Double): Pixel = Pixel(this.value * factor)

    /** Scales this pixel length by [factor], in pixels. */
    operator fun times(factor: Int): Pixel = times(factor.toDouble())

    /** Splits this pixel length into [divisor] equal parts, in pixels. */
    operator fun div(divisor: Double): Pixel = Pixel(this.value / divisor)

    /** Splits this pixel length into [divisor] equal parts, in pixels. */
    operator fun div(divisor: Int): Pixel = div(divisor.toDouble())

    /**
     * How many times [other] fits into this pixel length: `10.px / 4.px` is `2.5`.
     *
     * Dividing by `Pixel.ZERO` follows the usual [Double] rules and gives an infinity.
     */
    operator fun div(other: Pixel): Double = this.value / other.value

    /** Flips the sign of this pixel length. */
    operator fun unaryMinus(): Pixel = Pixel(-this.value)

    /** Orders two pixel lengths, which is what makes `<`, `<=`, `>` and `>=` work. */
    operator fun compareTo(other: Pixel): Int = this.value.compareTo(other.value)

    override fun toString(): String = "${this.value.trimmed()}px"

    companion object {
        /** A pixel length of nothing: `0.px`. */
        val ZERO = Pixel(0.0)
    }
}

/** `10.percent` — 10% of the parent size. */
val Number.percent: Percent get() = Percent(toDouble())

/** `10.p` — shorthand for [percent]. */
val Number.p: Percent get() = Percent(toDouble())

/** `10.px` — 10 device independent pixels. */
val Number.px: Pixel get() = Pixel(toDouble())

/** `10.fp` — device independent pixels, kept as an alias for [px]. */
val Number.fp: Pixel get() = Pixel(toDouble())

/** `0.5 * 10.percent` — the reverse of [Percent.times]. */
operator fun Double.times(length: Percent): Percent = length * this

/** `2 * 10.percent` — the reverse of [Percent.times]. */
operator fun Int.times(length: Percent): Percent = length * this.toDouble()

/** `0.5 * 10.px` — the reverse of [Pixel.times]. */
operator fun Double.times(length: Pixel): Pixel = length * this

/** `2 * 10.px` — the reverse of [Pixel.times]. */
operator fun Int.times(length: Pixel): Pixel = length * this.toDouble()

private fun Double.trimmed(): String {
    val asLong = toLong()
    return if (this == asLong.toDouble()) asLong.toString() else toString()
}
