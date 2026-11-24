package com.github.kusa233.ovatoxin.widget

/**
 * How a [FlexBox] places a child inside the line it was given, from top to bottom.
 *
 * A line is as tall as its tallest child, so every shorter child of that line has room above or
 * below it. [Align] decides where that room goes: a child can sit at the top of its line, in the
 * middle of it, or at its bottom. The box below is one line, the filled block is one child of it:
 *
 * ```
 * START        CENTER       END
 * ┌─────┐      ┌─────┐      ┌─────┐
 * │ ██  │      │     │      │     │
 * │     │      │ ██  │      │     │
 * │     │      │     │      │ ██  │
 * └─────┘      └─────┘      └─────┘
 * ```
 *
 * It works along the cross axis — vertically, across the direction the children flow in — and is
 * therefore the counterpart of [Justify]. The values are the ones CSS uses for `align-items`: every
 * child is aligned inside its own line, and the lines themselves follow each other downwards
 * regardless of the value.
 *
 * A [FlexBox] starts at [START], i.e. every child sits at the top of its line, which is also what a
 * line of equally tall children looks like.
 */
enum class Align {

    /**
     * Puts every child at the top of its line, leaving the room below it.
     *
     * This is the default, and the only placement that does not depend on the height of the line: a
     * child does not move when a taller sibling joins its line.
     */
    START,

    /**
     * Centres every child in its line, splitting the room above and below it equally.
     *
     * A child as tall as its line is unaffected — there is no room to split.
     */
    CENTER,

    /**
     * Puts every child at the bottom of its line, leaving the room above it.
     *
     * The mirror image of [START].
     */
    END,
}
