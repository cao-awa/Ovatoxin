package com.github.kusa233.ovatoxin.style

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.px
import javafx.scene.paint.Color

/**
 * The outline of a component, configured with a `border { … }` block:
 *
 * ```kotlin
 * style {
 *     border {
 *         size = 1.px
 *         color = Color.BLACK
 *     }
 * }
 * ```
 *
 * The border is drawn on the inside edge of the box and follows the corner radius of the enclosing
 * [Style], so it stays attached to a rounded fill. It is disabled until a non-zero [size] is set.
 *
 * A `border { }` block is a marked DSL scope: inside it only the members of the border are reachable
 * by name, so `corner` or `foreground` cannot be set by accident from here.
 */
@OvatoxinDsl
class Border {

    /**
     * The thickness of the border. Defaults to [DEFAULT_SIZE], i.e. no border.
     *
     * Percentages resolve against the shorter side of the box, pixels are used as they are, and an
     * `em` resolves against the font size of the component. A negative value is treated as zero.
     */
    var size: Length = DEFAULT_SIZE

    /**
     * The colour of the border, or `null` for no outline.
     *
     * Defaults to [DEFAULT_COLOR]. Ignored while [size] is zero or negative.
     */
    var color: Color? = DEFAULT_COLOR

    /** Applies [block] to this border and returns it. */
    fun configure(block: Border.() -> Unit): Border {
        block()
        return this
    }

    /** Restores every property to the documented default: no border. */
    internal fun reset() {
        this.size = DEFAULT_SIZE
        this.color = DEFAULT_COLOR
    }

    /** Copies every property of [other] into this border. */
    fun copyFrom(other: Border) {
        this.size = other.size
        this.color = other.color
    }

    override fun toString(): String =
        "Border(size=${this.size}, color=${this.color?.let { cssColor(it) } ?: "none"})"

    companion object {
        /** The thickness of a border nobody configured: none at all. */
        val DEFAULT_SIZE: Length = 0.px

        /** The colour a border uses when only its [size] is configured. */
        val DEFAULT_COLOR: Color = Color.BLACK
    }
}
