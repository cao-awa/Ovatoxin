package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl

/**
 * A container that is itself a drawing surface, so children can be declared inside its `layout`
 * block.
 *
 * Three types of the library extend this class, because only they define a region a child can be
 * placed into:
 *
 * * [com.github.kusa233.ovatoxin.window.Frame] — the window, the root of every tree;
 * * [com.github.kusa233.ovatoxin.widget.container.Div] — a box inside it, which rescopes percentages;
 * * [com.github.kusa233.ovatoxin.widget.container.FlexBox] — a box that flows its children in a line.
 *
 * Everything else is a plain [Component] and cannot draw at all: a button, a list or a text area
 * is *drawn*, it never draws. Even the surfaces have no `draw` of their own — the method lives on
 * [SurfaceScope], the receiver of [layout], and therefore exists only while that block runs:
 *
 * ```kotlin
 * val frame = Frame()
 * frame.layout {
 *     draw(Button()) { it.text("Ok") }    // compiles: this block declares children of the frame
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
 * ## Padding
 *
 * A surface keeps its children away from its own edges with `hPadding` and `vPadding`, which are
 * declared in the `layout` block of the surface — like every other value of a layout, right next
 * to the position and the size it is drawn with:
 *
 * ```kotlin
 * draw(Div()) { panel ->
 *     layout {
 *         x = 10.percent
 *         width = 80.percent
 *         hPadding = 24.px
 *         vPadding = 16.px
 *     }
 *     panel.layout {
 *         // children of the panel: 24px from its left, 16px from its top
 *         draw(Button()) { it.layout { x = 0.px; y = 0.px } }
 *     }
 * }
 * ```
 *
 * The padding is a *content inset*: the room left over shrinks by the padding on both sides, so a
 * child at `100.percent` still ends one padding short of the edge instead of running over it. The
 * inset box is what percentages resolve against, and its origin is what a child's `x` and `y` are
 * counted from — see [com.github.kusa233.ovatoxin.component.Container.contentBox].
 *
 * It is declared in the layout block of the *container*, so it insets the children of that
 * container and nothing else: padding on a panel does not move the panel, and a component that
 * holds no children is unaffected by either value.
 *
 * Padding is an inset and nothing more — it never pushes two children apart. A surface that flows
 * its children keeps the space between them in its own
 * [gap][com.github.kusa233.ovatoxin.widget.container.FlexBox.gap], which is a value of the surface rather
 * than one of its layout.
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
     * The padding of the surface is not part of this call: it is one of the values of the
     * [Layout][com.github.kusa233.ovatoxin.layout.Layout] the surface is drawn with — `hPadding`
     * and `vPadding` inside the `layout { }` block of the surrounding `draw`, or [applyLayout] on
     * a surface that already exists.
     *
     * @param block declares the children and returns nothing.
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
