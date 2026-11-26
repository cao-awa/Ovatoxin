package com.github.kusa233.ovatoxin.state

import com.github.kusa233.ovatoxin.component.Component

/**
 * A value that components are bound to.
 *
 * A state both **stores** a value and **drives** the components that read it, so one object is
 * all the plumbing a binding needs:
 *
 * ```kotlin
 * val state = State("zh-cn")
 *
 * draw(TextArea()) { text ->
 *     text.text(languages[state.value])
 * }.withState(state)
 * ```
 *
 * A component is hooked up with [Component.withState], and from then on this state owns it:
 *
 * - [update] hands a value in, which is what a text box, a clock or a network reply calls;
 * - [value] saves it, so the state always remembers the newest value;
 * - every component bound to this state is redrawn on its own, while the rest of the tree is
 *   left exactly as it was.
 *
 * A redraw re-runs the block the component was drawn with, so the block reads [value] again and
 * everything it derives from it on the way — a translation, a formatted number, a colour — is
 * recomputed.
 *
 * ```kotlin
 * state.update("en-us") // the language changed: redraw whatever reads the state
 * ```
 *
 * Writing [value] directly does the same work:
 *
 * ```kotlin
 * state.value = "en-us"
 * ```
 *
 * A value equal to the current one is ignored, so a component that feeds the state it is bound
 * to cannot loop back into itself.
 *
 * @param T the type of the value held by this state.
 */
class State<T>(initialValue: T) {

    private var current: T = initialValue

    /**
     * The newest value of this state.
     *
     * Assigning a different value saves it and redraws every component bound to this state.
     * Assigning an equal value does nothing, which keeps a state that is bound to a component
     * feeding the same state from looping back into itself.
     */
    var value: T
        get() = this.current
        set(newValue) {
            if (this.current == newValue) return
            this.current = newValue
            for (component in components.toList()) {
                component.redraw()
            }
        }

    /** Every component bound to this state, in the order they were bound. */
    val components: List<Component<*>>
        field = mutableListOf<Component<*>>()

    /**
     * Hands [newValue] to this state.
     *
     * This is the way a producer — a text box reporting what the user typed, a clock, a network
     * reply — announces a new value: it is saved, and the components bound to this state are
     * redrawn. Components bound to any other state stay exactly as they are.
     *
     * Handing in the value that is already stored redraws nothing.
     */
    fun update(newValue: T) {
        this.value = newValue
    }

    /** Binds [component] to this state. Called by [Component.withState]. */
    internal fun bind(component: Component<*>) {
        if (!components.contains(component)) components += component
    }

    override fun toString(): String =
        "State(value=${this.value}, components=${components.size})"
}
