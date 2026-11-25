package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.style.Style

/**
 * The scope of one [SurfaceScope.draw] call.
 *
 * It offers the blocks that belong to a single component — [layout] and [style] — while the
 * drawn component itself is available as the lambda parameter:
 *
 * ```kotlin
 * draw(TextArea()) { text ->
 *     layout {
 *         x = 10.percent
 *     }
 *     style {
 *         corner = 10.px
 *     }
 *     text.text("Hello")
 * }
 * ```
 *
 * The scope is marked with [OvatoxinDsl], so the blocks around it are hidden from it: inside one
 * `draw` block another component's blocks cannot be reached implicitly. Drawing into a nested
 * container always goes through that container, as in `panel.layout { draw(...) }`.
 */
@OvatoxinDsl
class DrawScope<T : Component<*>>(internal val component: T) {

    /**
     * Configures the position and the size of the drawn component.
     *
     * This is also where a component that holds children declares its `hPadding` and `vPadding`,
     * so the inset sits next to the box it applies to and travels with it when the component is
     * redrawn:
     *
     * ```kotlin
     * draw(Div()) { panel ->
     *     layout {
     *         width = 80.percent
     *         hPadding = 12.px
     *     }
     *     panel.layout { draw(TextArea()) { it.text("12px in") } }
     * }
     * ```
     */
    fun layout(block: Layout.() -> Unit): DrawScope<T> {
        this.component.layoutSpec.configure(block)
        return this
    }

    fun layout(layoutX: Length = Length.ZERO, layoutY: Length = Length.ZERO, layoutWidth: Length? = null, layoutHeight: Length? = null): DrawScope<T> {
        layout {
            x = layoutX
            y = layoutY
            width = layoutWidth
            height = layoutHeight
        }
        return this
    }

    /**
     * Configures the appearance of the drawn component.
     *
     * This is also where a component states the font size its `em`s are measured against — see
     * [Style.fontSize]. Changing it moves the pixels of every `em` below, so the component is laid
     * out again as soon as the value really changed:
     *
     * ```kotlin
     * draw(Div()) { panel ->
     *     layout { hPadding = 1.em }   // 14px, the size in force when this line runs
     *     style { fontSize = 20.px }   // now the same padding is 20px, and so is the aside below
     *     panel.layout {
     *         draw(TextArea("inherits 20")) { }   // its own ems would be 20px each
     *         draw(TextArea("says 32")) { style { fontSize = 32.px } }   // this one resolves against 32px
     *     }
     * }
     * ```
     *
     * The size is a [Length] but only `px` and `percent` are accepted — `percent` of the component's
     * own box height, so the text scales with the box — because an `em` would be measuring the font
     * against itself.
     */
    fun style(block: Style.() -> Unit): DrawScope<T> {
        val previous = this.component.styleSpec.fontSize
        this.component.styleSpec.configure(block)
        if (this.component.styleSpec.fontSize != previous) {
            this.component.relayoutAfterStyleChange()
        }
        return this
    }
}
