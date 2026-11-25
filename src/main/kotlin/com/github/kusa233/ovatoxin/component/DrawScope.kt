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

    fun layout(
        layoutX: Length = Length.ZERO,
        layoutY: Length = Length.ZERO,
        layoutWidth: Length? = null,
        layoutHeight: Length? = null,
        tuning: Layout.() -> Unit = { }
    ): DrawScope<T> {
        layout {
            x = layoutX
            y = layoutY
            width = layoutWidth
            height = layoutHeight
        }
        layout(tuning)
        return this
    }

    /**
     * Applies a [Layout] preset, then tunes it with [block].
     *
     * The preset — built with
     * [layout][com.github.kusa233.ovatoxin.dsl.layout] outside any component — is copied into this
     * component first, so [block] starts from exactly the values of the template rather than from
     * whatever the component held before. That makes the block the natural place for the one or two
     * slots that differ per component:
     *
     * ```kotlin
     * val halfWidth = layout { width = 50.percent; height = 100.percent }
     * draw(Div()) { panel ->
     *     layout(halfWidth) { x = 50.percent }
     * }
     * ```
     *
     * @param preset the template to copy in; every slot of this component is replaced by it.
     * @param block per-component overrides applied on top of the preset.
     */
    fun layout(preset: Layout, block: Layout.() -> Unit = {}): DrawScope<T> {
        this.component.layoutSpec.copyFrom(preset)
        this.component.layoutSpec.configure(block)
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

    /**
     * Applies a [Style] preset, then tunes it with [block].
     *
     * The preset — built with
     * [style][com.github.kusa233.ovatoxin.dsl.style] outside any component — is copied into this
     * component first, so [block] starts from exactly the appearance of the template. The copy
     * reaches into the [Background][com.github.kusa233.ovatoxin.style.Background], so two components
     * that share one template do not share a fill:
     *
     * ```kotlin
     * val card = style { background { color = Color.web("#4c8bf5") }; corner = 8.px }
     * draw(Div()) { panel -> style(card) { corner = 50.percent } }
     * ```
     *
     * @param preset the template to copy in; every slot of this component is replaced by it.
     * @param block per-component overrides applied on top of the preset.
     */
    fun style(preset: Style, block: Style.() -> Unit = {}): DrawScope<T> {
        val previous = this.component.styleSpec.fontSize
        this.component.styleSpec.copyFrom(preset)
        this.component.styleSpec.configure(block)
        if (this.component.styleSpec.fontSize != previous) {
            this.component.relayoutAfterStyleChange()
        }
        return this
    }
}
