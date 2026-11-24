package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl

/**
 * The scope a [ComponentDrawable.layout] block runs in — the only place a child can be declared.
 *
 * The library has three drawing surfaces — [com.github.kusa233.ovatoxin.window.Frame],
 * [com.github.kusa233.ovatoxin.widget.Div] and [com.github.kusa233.ovatoxin.widget.FlexBox] — and
 * all of them are [ComponentDrawable]s. None of them carries a `draw` of its own: the method lives
 * here, on the scope the `layout` block receives, so it is reachable only *inside* that block and
 * nowhere else:
 *
 * ```kotlin
 * val frame = Frame()
 * frame.layout {
 *     draw(Button()) { it.text = "Ok" }   // compiles: this block declares children of the frame
 * }
 * frame.draw(Button())                    // does not compile: the frame has no draw
 * draw(Button())                          // does not compile: no surface in scope
 * ```
 *
 * ```kotlin
 * draw(Button()) { button ->
 *     button.draw(TextArea())             // does not compile: a button is not a surface
 * }
 * ```
 *
 * A surface can be handed to another block explicitly, which is how a nested surface is filled —
 * `div.layout { ... }` inside the `draw` block of that `div`.
 *
 * The scope is marked with [OvatoxinDsl]: inside a nested `layout` block the enclosing surface of
 * a `draw` block is hidden, so children are never declared by accident for the wrong surface.
 *
 * An instance is created by [ComponentDrawable.layout] and by nothing else, which is why the
 * constructor is [internal].
 *
 * @param T the concrete surface, so [draw] attaches to the exact type — the surface the block of
 *   [ComponentDrawable.layout] was opened for.
 */
@OvatoxinDsl
class SurfaceScope<T : ComponentDrawable<T>> internal constructor(
    /** The surface these children belong to. */
    val surface: T,
) {

    /**
     * Draws [component] into [surface] and configures it with [config].
     *
     * The component is the lambda parameter; the receiver is a [DrawScope] that accepts a
     * `layout { ... }` block. Drawing a container nests it, so its own `layout { ... }` blocks
     * are scoped to that container.
     *
     * The block is remembered as the render function of the component, so a redraw — see
     * [Component.withState] — can run it again and pick up the new value of a state.
     *
     * The component is [reset][Component.reset] right before [config] runs, so the block always
     * starts from the defaults instead of inheriting what an earlier draw set.
     *
     * @return the drawn component, so a reference can be kept for later wiring.
     * @throws IllegalArgumentException if [component] already belongs to another container.
     */
    fun <C : Component<*>> draw(component: C, config: DrawScope<C>.(C) -> Unit = {}): C {
        val replayed = replay(component, config)
        if (replayed != null) return replayed

        require(component.parent == null) {
            "${component::class.simpleName} is already attached to a container"
        }
        // Start from the documented defaults, so nothing a previous draw left behind leaks into
        // this one. The component is reset before the block runs, never after.
        component.reset()
        component.parent = this.surface
        this.surface.attach(component)
        val scope = DrawScope(component)
        component.recordDrawBlock { scope.config(component) }
        scope.config(component)
        this.surface.relayout()
        return component
    }

    /**
     * Fills a slot of a replay with an existing child instead of a new one.
     *
     * Returns `null` when no replay is running or the slot does not hold a component of the
     * same kind, in which case [draw] attaches [component] as usual.
     */
    private fun <C : Component<*>> replay(component: C, config: DrawScope<C>.(C) -> Unit): C? {
        val scope = ReplayScope.current ?: return null
        val existing = this.surface.childAt(scope.index(this.surface))
        if (existing == null || existing::class != component::class) return null
        scope.advance(this.surface)

        @Suppress("UNCHECKED_CAST")
        val target = existing as C
        // A redraw re-runs the block, so the replayed child starts from its defaults as well;
        // that is what makes a value dropped from the block disappear instead of lingering.
        target.reset()
        DrawScope(target).config(target)
        return target
    }
}
