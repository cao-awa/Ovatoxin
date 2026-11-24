package com.github.kusa233.ovatoxin.widget.container

/**
 * How a [FlexBox] shares out the room a line has left over, from left to right.
 *
 * A line takes children until the next one would not fit, so it usually ends a little short of the
 * right edge of the content box. [Justify] decides what happens to that leftover room: it can be
 * left where it is, split evenly on both ends, pushed all to the left, or handed to the gaps between
 * the children. The dots below stand for the room now under discussion, the children for themselves:
 *
 * ```
 * START          [a][b][c]...............
 * CENTER         .....[a][b][c]..........
 * END            ..............[a][b][c].
 * SPACE_BETWEEN  [a].....[b]......[c]....
 * SPACE_AROUND   ..[a]....[b].....[c]....
 * ```
 *
 * It is the main axis of the flow — the direction the children are laid in, i.e. horizontally — and
 * the counterpart of [Align], which works along the other one. The values are the ones CSS uses for
 * `justify-content`, and they never resize a child: only its position changes.
 *
 * A [FlexBox] starts at [START], i.e. the children are packed to the left and the room that is left
 * over stays on the right.
 */
enum class Justify {

    /**
     * Packs the children of a line against its start, leaving all the leftover room on the right.
     *
     * This is the default, and the only arrangement that does not depend on the width of the box: a
     * row that grows wider keeps its children exactly where they were.
     */
    START,

    /**
     * Centres the children of a line, splitting the leftover room equally between both ends.
     *
     * A line that fills the box completely is unaffected — there is no room to split.
     */
    CENTER,

    /**
     * Packs the children of a line against its end, leaving all the leftover room on the left.
     *
     * The mirror image of [START].
     */
    END,

    /**
     * Keeps the first child at the start, the last one at the end, and spreads the leftover room
     * evenly between the neighbours in between.
     *
     * The room shared out is added to the [gap][FlexBox.gap] of the box, so the visible space between
     * two children is the gap plus one share. A line of a single child has no neighbour to spread
     * room between and behaves like [START].
     */
    SPACE_BETWEEN,

    /**
     * Gives every child the same room on both of its sides, so the children look evenly spaced while
     * the outermost ones keep half a share against the edge of the line.
     *
     * Like [SPACE_BETWEEN] the shares are added to the [gap][FlexBox.gap] of the box. A line of a
     * single child puts it in the middle, since it carries a share on either side.
     */
    SPACE_AROUND,
}
