package com.github.kusa233.ovatoxin.layout

/**
 * A length: a size or an offset, measured against the size of the parent container, against the font
 * size of the component, or in absolute device independent pixels.
 *
 * The three ways of measuring are three types — [Percent], [Em] and [Pixel] — so a relative length
 * can never be mistaken for an absolute one. The DSL turns a number into one of them:
 *
 * ```kotlin
 * x = 10.percent   // a tenth of the parent width, a Percent
 * y = 10.px        // ten device independent pixels, a Pixel
 * hPadding = 1.em  // one line of text of this component, an Em
 * ```
 *
 * Neither relative unit can become a pixel count before something is known: a percentage needs the
 * size of the parent, an `em` needs the font size of the component. That is what [resolve] and
 * [toPixels] are for — they hand those references in and get pixels back.
 *
 * Arithmetic belongs to the units and not to this type: [Percent], [Em] and [Pixel] each define
 * their own operators, and each of them accepts its own kind only. A length is therefore only ever
 * added, subtracted, scaled or compared inside the unit it was written in, and the compiler is what
 * keeps it that way:
 *
 * ```kotlin
 * var done = Percent.ZERO
 * while (done < Length.FULL) done += 1.percent
 *
 * 10.px * 2          // 20.px
 * 1.5.em + 0.5.em    // 2.em
 * 100.percent / 25.percent   // 4.0
 * 10.percent + 4.px  // does not compile
 * ```
 *
 * Mixing them is a mistake rather than something to guess at, because only the parent size or the
 * font size could settle it. When a combination really is needed, freeze the relative side first —
 * `10.percent.toPixels(width)` — so both are pixel lengths.
 *
 * This is the type the slots of a [Layout] and of a
 * [style][com.github.kusa233.ovatoxin.style.Style] store, so one slot can accept any of the units.
 */
sealed class Length {

    /** The raw amount: a percentage of the parent size, a multiple of the font size, or a pixel count. */
    abstract val value: Double

    /** `true` when this length is exactly zero, i.e. when it leaves a slot at its default. */
    val isZero: Boolean get() = this.value == 0.0

    /**
     * Resolves this length into pixels, against the [available] space of the parent container and the
     * [fontSize] of the component the length belongs to.
     *
     * A [Pixel] is already a pixel count, so it ignores both references; a [Percent] is turned into
     * pixels with [available], an [Em] with [fontSize]. A length that names no [fontSize] — a frozen
     * percentage, a value inspected outside a component — resolves an `em` against
     * [DEFAULT_FONT_SIZE].
     */
    fun resolve(available: Double, fontSize: Double = DEFAULT_FONT_SIZE): Double = when (this) {
        is Percent -> available * this.value / 100.0
        is Em -> this.value * fontSize
        is Pixel -> this.value
    }

    /**
     * Freezes this length into pixels, resolving the relative units against [available] and
     * [fontSize].
     */
    fun toPixels(available: Double, fontSize: Double = DEFAULT_FONT_SIZE): Pixel =
        Pixel(resolve(available, fontSize))

    /**
     * Resolves a length used as a *font size* against the height of the component's own box.
     *
     * A font size is a size like any other, but it is not a length like any other: it is the number
     * the whole text is measured in, so a percentage of it is a percentage of the surface the
     * component occupies and not of the surrounding text. Only the two units that need no font of
     * their own are allowed here:
     *
     * * a [Pixel] is used as it is: `16.px` is sixteen device independent pixels of text;
     * * a [Percent] is that fraction of the component's own box [height]: a `50.percent` font on a
     *   component `40.px` tall is `20.0`, so the text scales with the box it is drawn in;
     * * an [Em] is refused, because an `em` is a multiple of the very font size it would be setting.
     *
     * A percentage needs the box, which is not known before the component is laid out. When
     * [height] is `null` — the box is still unknown, or its height is `auto` or itself written in
     * `em` and so would grow with the font being resolved — the result is `null` and the caller
     * falls back to the inherited size instead of chasing a size that depends on itself.
     *
     * @param height the height of the component's own box in pixels, or `null` when it is not known
     *   in a way that does not depend on the font size itself.
     * @return the size in pixels, or `null` for a percentage whose box height is unknown.
     * @throws IllegalArgumentException when this is an [Em].
     */
    internal fun resolveFontSize(height: Double?): Double? = when (this) {
        is Pixel -> this.value
        is Percent -> height?.let { it * this.value / 100.0 }
        is Em -> throw IllegalArgumentException(
            "fontSize is a font size and cannot be measured in em: an em is a multiple of the " +
                "font size, so it would measure the font against itself. Use px or percent.",
        )
    }

    companion object {
        /**
         * A zero offset — the same value as [Pixel.ZERO], which is what every position starts at.
         *
         * Zero is not a unit of its own, it is only a unit that happens to be empty: this one is a
         * pixel length and [Percent.ZERO] is a percentage one. They describe the same amount of
         * nothing, but they are not the same type, so neither can be handed to the other's
         * operators.
         *
         * It is read off [Pixel.ZERO] instead of being a constant of its own, and it is read
         * through a getter rather than into a field, because the two classes would otherwise
         * initialise each other: a subclass is initialised through its superclass, so the very
         * first `Pixel` use would enter this class first and read `Pixel.ZERO` while the companion
         * of `Pixel` was still empty.
         */
        val ZERO: Length get() = Pixel.ZERO

        /** A length that fills the parent completely: `100.percent`. */
        val FULL: Percent = Percent(100.0)

        /** Creates a percentage length: `Length.percent(12.5)` is the same as `12.5.percent`. */
        fun percent(value: Double): Percent = Percent(value)

        /** Creates an absolute pixel length: `Length.pixels(4.0)` is the same as `4.px`. */
        fun pixels(value: Double): Pixel = Pixel(value)

        /** Creates a font relative length: `Length.ems(1.5)` is the same as `1.5.em`. */
        fun ems(value: Double): Em = Em(value)

        /**
         * The font size every component resolves an [Em] against until a component sets one of its
         * own: 14 device independent pixels.
         *
         * A component that draws text — a
         * [TextArea][com.github.kusa233.ovatoxin.widget.text.TextArea] — carries its own size, and
         * every component without text of its own inherits the size of its parent. A tree therefore
         * always has a size to resolve against, and a tree that never names one lands here.
         */
        const val DEFAULT_FONT_SIZE: Double = 14.0
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
@ConsistentCopyVisibility
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
 * A length relative to the font size of the component: `1.5.em` is one and a half lines of its text.
 *
 * This is the unit a layout of text is written in. A padding of `1.em` stays exactly one line tall
 * whatever the font ends up being, a gap of `2.em` stays two lines, and both follow along when the
 * font grows — which is the whole point of writing them in `em` rather than in pixels:
 *
 * ```kotlin
 * layout {
 *     hPadding = 0.75.em
 *     vPadding = 0.5.em
 * }
 * ```
 *
 * The reference is the font size of the component the length belongs to, not of the window:
 * [Length.resolve] is handed that size by the component itself, so a child of a large-text area
 * measures its own `em` against the large size only if it inherits it. A component with no text of
 * its own — a container — inherits the size of its parent, and a tree that never sets one resolves
 * against [Length.DEFAULT_FONT_SIZE]. An [Em] sits between the two other units: it survives a resize
 * of the window the way [Percent] does, and it keeps a fixed count of pixels the way [Pixel] does,
 * as long as the font size does not change.
 *
 * An `em` only ever does arithmetic with another `em`, under the same rules [Percent] follows:
 *
 * ```kotlin
 * 1.5.em + 0.5.em     // 2.em
 * 2.em * 3            // 6.em
 * 3.em / 1.5.em       // 2.0
 * 2.em > 1.em         // true
 * ```
 */
@ConsistentCopyVisibility
data class Em internal constructor(override val value: Double) : Length() {

    /** Adds [other] font relative lengths to this one: `1.5.em + 0.5.em` is `2.em`. */
    operator fun plus(other: Em): Em = Em(this.value + other.value)

    /** Subtracts [other] font relative lengths from this one: `2.em - 0.5.em` is `1.5.em`. */
    operator fun minus(other: Em): Em = Em(this.value - other.value)

    /** Scales this font relative length by [factor], staying relative to the font size. */
    operator fun times(factor: Double): Em = Em(this.value * factor)

    /** Scales this font relative length by [factor], staying relative to the font size. */
    operator fun times(factor: Int): Em = times(factor.toDouble())

    /** Splits this font relative length into [divisor] equal parts. */
    operator fun div(divisor: Double): Em = Em(this.value / divisor)

    /** Splits this font relative length into [divisor] equal parts. */
    operator fun div(divisor: Int): Em = div(divisor.toDouble())

    /**
     * How many times [other] fits into this font relative length: `3.em / 1.5.em` is `2.0`.
     *
     * Dividing by `Em.ZERO` follows the usual [Double] rules and gives an infinity.
     */
    operator fun div(other: Em): Double = this.value / other.value

    /** Flips the sign of this font relative length. */
    operator fun unaryMinus(): Em = Em(-this.value)

    /** Orders two font relative lengths, which is what makes `<`, `<=`, `>` and `>=` work. */
    operator fun compareTo(other: Em): Int = this.value.compareTo(other.value)

    override fun toString(): String = "${this.value.trimmed()}em"

    companion object {
        /** A font relative length of nothing: `0.em`. */
        val ZERO = Em(0.0)
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
@ConsistentCopyVisibility
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

/** `1.5.em` — one and a half lines of the font of the component. */
val Number.em: Em get() = Em(toDouble())

/** `0.5 * 10.percent` — the reverse of [Percent.times]. */
operator fun Double.times(length: Percent): Percent = length * this

/** `2 * 10.percent` — the reverse of [Percent.times]. */
operator fun Int.times(length: Percent): Percent = length * this.toDouble()

/** `0.5 * 1.em` — the reverse of [Em.times]. */
operator fun Double.times(length: Em): Em = length * this

/** `2 * 1.em` — the reverse of [Em.times]. */
operator fun Int.times(length: Em): Em = length * this.toDouble()

/** `0.5 * 10.px` — the reverse of [Pixel.times]. */
operator fun Double.times(length: Pixel): Pixel = length * this

/** `2 * 10.px` — the reverse of [Pixel.times]. */
operator fun Int.times(length: Pixel): Pixel = length * this.toDouble()

private fun Double.trimmed(): String {
    val asLong = toLong()
    return if (this == asLong.toDouble()) asLong.toString() else toString()
}
