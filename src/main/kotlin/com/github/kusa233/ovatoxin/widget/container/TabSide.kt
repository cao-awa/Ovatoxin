package com.github.kusa233.ovatoxin.widget.container

/**
 * The side of a [TabPane] on which its [TabBar] is placed.
 *
 * The names describe the edge the bar sits on, not the direction the tabs run in: [UP] and [BOTTOM]
 * make a horizontal bar above or below the content, while [LEFT] and [RIGHT] make a vertical bar
 * along one of the two sides.
 */
enum class TabSide {
    /** The bar is above the content and runs horizontally. */
    UP,

    /** The bar is below the content and runs horizontally. */
    BOTTOM,

    /** The bar is to the left of the content and runs vertically. */
    LEFT,

    /** The bar is to the right of the content and runs vertically. */
    RIGHT;

    /** Whether the bar is on one of the two vertical sides. */
    val isVertical: Boolean get() = this == LEFT || this == RIGHT
}

/** The [TabSide.UP] entry, available unqualified for `tab.side = UP`. */
val UP: TabSide get() = TabSide.UP

/** The [TabSide.BOTTOM] entry, available unqualified for `tab.side = BOTTOM`. */
val BOTTOM: TabSide get() = TabSide.BOTTOM

/** The [TabSide.LEFT] entry, available unqualified for `tab.side = LEFT`. */
val LEFT: TabSide get() = TabSide.LEFT

/** The [TabSide.RIGHT] entry, available unqualified for `tab.side = RIGHT`. */
val RIGHT: TabSide get() = TabSide.RIGHT
