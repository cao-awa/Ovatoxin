package com.github.kusa233.ovatoxin.dsl

import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.style.Style

/**
 * Builds a reusable [Layout] preset outside any component.
 *
 * A `layout { }` block written directly inside a
 * [DrawScope][com.github.kusa233.ovatoxin.component.DrawScope] configures one component and nothing
 * else. The same block written here returns a plain [Layout] value instead, so one set of positions
 * and sizes can be named once and then applied wherever it is needed:
 *
 * ```kotlin
 * val halfWidth = layout {
 *     width = 50.percent
 *     height = 100.percent
 * }
 * frame.layout {
 *     draw(Div()) { left -> layout(halfWidth) }
 *     draw(Div()) { right -> layout(halfWidth) { x = 50.percent } }
 * }
 * ```
 *
 * The preset is a snapshot: applying it with
 * [DrawScope.layout][com.github.kusa233.ovatoxin.component.DrawScope.layout] copies its values into
 * the component, so changing the template afterwards does not move a component that already used
 * it, and two components that share one template stay independent.
 *
 * @param block configures the returned layout. Every slot left untouched keeps its documented
 *   default, so a template only states what differs from a plain component.
 * @return a new [Layout] holding exactly the values configured by [block].
 */
fun layout(block: Layout.() -> Unit = {}): Layout = Layout().configure(block)

/**
 * Builds a reusable [Style] preset outside any component.
 *
 * The counterpart of [layout] for appearance. A `style { }` inside a
 * [DrawScope][com.github.kusa233.ovatoxin.component.DrawScope] styles one component; written here it
 * returns a plain [Style] value that can be handed to many components:
 *
 * ```kotlin
 * val card = style {
 *     background { color = Color.web("#4c8bf5") }
 *     corner = 8.px
 * }
 * frame.layout {
 *     draw(Div()) { first -> style(card) }
 *     draw(Div()) { second -> style(card) { corner = 50.percent } }
 * }
 * ```
 *
 * Like a layout template it is a snapshot: the values — including the [background][Style.background]
 * — are copied into each component that applies it, so later edits to the template do not touch
 * components already built from it.
 *
 * @param block configures the returned style. Every slot left untouched keeps its documented
 *   default, so a template only states what differs from a plain component.
 * @return a new [Style] holding exactly the values configured by [block].
 */
fun style(block: Style.() -> Unit = {}): Style = Style().configure(block)
