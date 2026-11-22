package com.github.kusa233.ovatoxin.layout

/**
 * A length used by [Layout].
 *
 * A length is either relative to the size of the parent container ([Kind.PERCENT])
 * or absolute in device independent pixels ([Kind.PIXELS]).
 *
 * The DSL exposes friendly aliases, so all of the following are equivalent:
 *
 * ```kotlin
 * x = 10.percent
 * x = 10.p
 * ```
 */
@ConsistentCopyVisibility
data class Length private constructor(
    /** The raw value, interpreted according to [kind]. */
    val value: Double,
    /** How [value] has to be interpreted. */
    val kind: Kind,
) {

    enum class Kind {
        /** [value] is a percentage (`0.0` .. `100.0`) of the parent size. */
        PERCENT,

        /** [value] is an absolute amount of pixels. */
        PIXELS,
    }

    /** Resolves this length against the [available] space of the parent container. */
    fun resolve(available: Double): Double = when (this.kind) {
        Kind.PERCENT -> available * this.value / 100.0
        Kind.PIXELS -> this.value
    }

    /** Returns a pixel copy of this length, resolving percentages against [available]. */
    fun toPixels(available: Double): Length = pixels(resolve(available))

    override fun toString(): String = when (this.kind) {
        Kind.PERCENT -> "${this.value.trimmed()}%"
        Kind.PIXELS -> "${this.value.trimmed()}px"
    }

    companion object {
        /** A zero pixel length. */
        val ZERO: Length = Length(0.0, Kind.PIXELS)

        /** A length that fills the parent completely. */
        val FULL: Length = Length(100.0, Kind.PERCENT)

        /** Creates a percentage length. */
        fun percent(value: Double): Length = Length(value, Kind.PERCENT)

        /** Creates an absolute pixel length. */
        fun pixels(value: Double): Length = Length(value, Kind.PIXELS)
    }
}

/** `10.percent` — 10% of the parent size. */
val Number.percent: Length get() = Length.percent(toDouble())

/** `10.p` — shorthand for [percent]. */
val Number.p: Length get() = Length.percent(toDouble())

/** `10.px` — 10 device independent pixels. */
val Number.px: Length get() = Length.pixels(toDouble())

/** `10.fp` — device independent pixels, kept as an alias for [px]. */
val Number.fp: Length get() = Length.pixels(toDouble())

private fun Double.trimmed(): String {
    val asLong = toLong()
    return if (this == asLong.toDouble()) asLong.toString() else toString()
}
