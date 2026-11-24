package com.github.kusa233.ovatoxin.component

/**
 * The area a container places its children into, in pixels.
 *
 * It is the box of the container itself, minus whatever its padding takes away: [offsetX] and
 * [offsetY] are where the content starts, and [width] and [height] are how much room is left after
 * the inset was cut off on both sides. The padding is the `hPadding` and `vPadding` of the
 * container's [layout][com.github.kusa233.ovatoxin.layout.Layout], so the inset is declared where
 * every other value of the layout is; a container without padding reports its own size with a zero
 * origin, which is what the default implementation of
 * [Container.contentBox][com.github.kusa233.ovatoxin.component.Container] returns.
 *
 * Children are positioned from the offset, and their percentages resolve against the width and
 * height — so a padded box shrinks its content by the padding twice over (left and right, top and
 * bottom) instead of merely shifting it, and a `100.percent` child stays inside the padding
 * instead of running over the edge it was supposed to keep clear of.
 *
 * ```text
 * ┌──────────────────────────────┐ ← the box of the container
 * │        ↑ vPadding            │
 * │  ┌────────────────────────┐  │
 * │  │  (offsetX, offsetY)    │  │ ← where content starts
 * │ h│    width x height      │h │
 * │ P│    a child at 100%     │P │
 * │ a└────────────────────────┘a │
 * │ d        ↓ vPadding        d │
 * └──────────────────────────────┘
 * ```
 *
 * The two bars are the `hPadding` of the same `Layout`, cut off on the left and right.
 *
 * @property offsetX the x of the content box inside the container, in pixels.
 * @property offsetY the y of the content box inside the container, in pixels.
 * @property width the usable width, in pixels.
 * @property height the usable height, in pixels.
 */
internal data class ContentBox(
    val offsetX: Double,
    val offsetY: Double,
    val width: Double,
    val height: Double,
)

/**
 * Where the hook of a container sits: the point its child's own `x` and `y` are counted from.
 *
 * For a plain container every slot is the origin of the content box, so a child lands exactly where
 * its [Layout][com.github.kusa233.ovatoxin.layout.Layout] says. A flowing container hands out the
 * slot it computed instead, and a child's `x`/`y` then add to it — the "nudge" semantics of the
 * flow.
 *
 * @property x the x of the slot, in pixels.
 * @property y the y of the slot, in pixels.
 */
internal data class Slot(val x: Double, val y: Double)

/**
 * The size a flow has to work with for one child, in pixels.
 *
 * A size is either the one the child asked for in its own `layout` block or the one it wants when
 * it says nothing — its preferred size, measured on the JavaFX node.
 *
 * @property width the occupied width, in pixels.
 * @property height the occupied height, in pixels.
 */
internal data class Size(val width: Double, val height: Double)
