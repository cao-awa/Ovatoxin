package com.github.kusa233.ovatoxin.dsl

/**
 * Marks the receiver types of the OvatoXin DSL.
 *
 * The DSL nests blocks with receivers: a
 * [com.github.kusa233.ovatoxin.component.SurfaceScope.draw] block hands over a
 * [com.github.kusa233.ovatoxin.component.DrawScope], and that scope opens a
 * [com.github.kusa233.ovatoxin.layout.Layout] block and a
 * [com.github.kusa233.ovatoxin.style.Style] block, which in turn opens a
 * [com.github.kusa233.ovatoxin.style.Background] block. Every one of them exposes properties of
 * the same shape — `x`, `corner`, `color` — so without a marker a name written one level too
 * deep silently lands on the *outer* block:
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     layout {
 *         corner = 10.px   // BUG without this marker: button.styleSpec.corner, not a corner at all
 *     }
 * }
 * ```
 *
 * Annotating the receivers with this marker makes that an error instead of a surprise: inside a
 * marked receiver an enclosing marked receiver is no longer available implicitly, so `corner`
 * simply does not resolve and the compiler points at the mistake. Reaching an outer block is
 * still possible, but it has to be said out loud, the way the DSL already asks for containers:
 *
 * ```kotlin
 * draw(Div()) { panel ->
 *     panel.layout {        // explicit: the children of the panel, not of the frame
 *         draw(TextArea()) { text -> ... }
 *     }
 * }
 * ```
 *
 * Only *implicit* availability is affected. Members of the innermost block, lambda parameters,
 * local variables, imports and top level functions all keep working as before.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class OvatoxinDsl
