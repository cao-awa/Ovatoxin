package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl

/**
 * A container that is itself a drawing surface, so children can be declared inside its `layout`
 * block.
 *
 * Only two types of the library extend this class, because only they define a region a child can
 * be placed into:
 *
 * * [com.github.kusa233.ovatoxin.window.Frame] — the window, the root of every tree;
 * * [com.github.kusa233.ovatoxin.widget.Div] — a box inside it, which rescopes percentages.
 *
 * Everything else is a plain [Component] and cannot draw at all: a button, a list or a text area
 * is *drawn*, it never draws. Even the two surfaces have no `draw` of their own — the method lives
 * on [SurfaceScope], the receiver of [layout], and therefore exists only while that block runs:
 *
 * ```kotlin
 * val frame = Frame()
 * frame.layout {
 *     draw(Button()) { it.text = "Ok" }   // compiles: this block declares children of the frame
 * }
 * frame.draw(Button())                    // does not compile: a frame has no draw
 * draw(Button())                          // does not compile: no surface in scope
 * ```
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     button.draw(TextArea())             // does not compile: a button is not a drawing surface
 * }
 * ```
 *
 * Nesting works by naming the inner surface in the block that draws it, so the inner `layout`
 * block declares children for *that* surface:
 *
 * ```kotlin
 * frame.layout {
 *     draw(Div()) { panel ->
 *         panel.layout { draw(TextArea()) { it.text("inside") } }
 *     }
 * }
 * ```
 *
 * @param T the concrete subclass, so [layout] returns the exact type and the block it opens is
 *   scoped to this surface.
 */
@OvatoxinDsl
abstract class ComponentDrawable<T : ComponentDrawable<T>> : Container<T>() {

    /**
     * Opens the block that declares the children of this surface.
     *
     * ```kotlin
     * frame.layout {
     *     draw(Button()) { ... }
     * }
     * ```
     *
     * The block runs with a [SurfaceScope] as receiver, which is where [SurfaceScope.draw] comes
     * from. Nothing else in the DSL offers `draw`, so a child cannot be attached outside a
     * `layout` block of the surface that should hold it.
     *
     * @return this surface, so the call chains.
     */
    fun layout(block: SurfaceScope<T>.() -> Unit): T {
        // A replayed block draws its children again, so register here too: a surface whose new
        // block draws fewer children than before still has to be trimmed.
        declareChildren { SurfaceScope(this.self).block() }
        return this.self
    }

    /** Re-runs the render function of this surface, reconciling the children it draws. */
    internal override fun replayDrawBlock() {
        val previous = ReplayScope.current
        val scope = ReplayScope()
        ReplayScope.current = scope
        scope.begin(this)
        try {
            super.replayDrawBlock()
        } finally {
            ReplayScope.current = previous
            scope.finish()
        }
        relayout()
    }
}
