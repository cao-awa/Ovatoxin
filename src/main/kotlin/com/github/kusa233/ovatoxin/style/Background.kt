package com.github.kusa233.ovatoxin.style

import javafx.scene.paint.Color

/**
 * The fill of a component, configured with a `background { … }` block:
 *
 * ```kotlin
 * style {
 *     background {
 *         color = Style.DEFAULT_COLOR
 *         opacity = 40
 *     }
 * }
 * ```
 *
 * A background has exactly two sources, and they are not mixed: as soon as an [image] is set it
 * is the only thing painted, and [color] is ignored. The colour is therefore a fallback that is
 * used only while no image is set.
 *
 * Neither source draws a border: the fill alone defines the shape, and the corner radius of the
 * enclosing [Style] rounds it.
 */
class Background {

    /**
     * A solid colour filling the box, or `null` for nothing at all.
     *
     * Defaults to [Style.DEFAULT_COLOR]. Ignored whenever [image] is set.
     */
    var color: Color? = Style.DEFAULT_COLOR

    /**
     * The URL of an image painted across the box, or `null` for none.
     *
     * Defaults to `null`. When set, it replaces [color] completely.
     *
     * The value is a URL the JavaFX CSS engine can load: a `file:` / `https:` URL, a classpath
     * path such as `/images/bg.png`, or a plain relative path. A JavaFX
     * [javafx.scene.image.Image] can be used through its `url`, which is exactly what this
     * property is rendered as. The image is scaled to cover the box, centred, and not repeated.
     */
    var image: String? = null

    /**
     * How opaque the fill is, as a **whole percentage**: `100` is fully opaque, `50` is halfway
     * to transparent, `0` paints nothing.
     *
     * This is a plain `Int`, not a `Length` — write `opacity = 40`, **not** `40.percent`.
     * Anything outside `0..100` is rejected on assignment with an [IllegalArgumentException]:
     *
     * ```kotlin
     * background { opacity = 101 }   // IllegalArgumentException
     * background { opacity = -1 }    // IllegalArgumentException
     * ```
     *
     * The opacity scales the alpha of [color], so a colour that is already translucent — see
     * `Color.web("#4c8bf5", 0.5)` — is made even more transparent rather than reset.
     *
     * An [image] is painted as it is: what a box shows is decided by [color] **or** by [image],
     * never by both, so transparency an image needs belongs in the image itself.
     */
    var opacity: Int = DEFAULT_OPACITY
        set(value) {
            require(value in MIN_OPACITY..MAX_OPACITY) {
                "opacity is a whole percentage from $MIN_OPACITY to $MAX_OPACITY, was $value"
            }
            field = value
        }

    /** Applies [block] to this background and returns it. */
    fun configure(block: Background.() -> Unit): Background {
        block()
        return this
    }

    /** Copies every property of [other] into this background. */
    fun copyFrom(other: Background) {
        this.color = other.color
        this.image = other.image
        this.opacity = other.opacity
    }

    override fun toString(): String =
        "Background(color=${this.color?.let { cssColor(it, this.opacity) } ?: "none"}, " +
            "image=${this.image ?: "none"}, opacity=${this.opacity})"

    companion object {
        /** The opacity of a fill nobody configured: fully opaque. */
        const val DEFAULT_OPACITY: Int = 100

        /** The most transparent an opacity may be: nothing is painted. */
        const val MIN_OPACITY: Int = 0

        /** The most opaque an opacity may be. */
        const val MAX_OPACITY: Int = 100
    }
}
