package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import javafx.scene.paint.Color

/**
 * The appearance painted on the selected tab of a [TabBar] or [TabPane].
 *
 * Set with `selectColor { ... }` on a [TabBar] or [TabPane]:
 *
 * ```kotlin
 * pane.selectColor {
 *     background = Color.WHITE
 *     foreground = Color.BLACK
 * }
 * ```
 *
 * A `null` value leaves that part of the selected tab as it was given: an unset [background] keeps
 * the button's own fill, and an unset [foreground] keeps the colour the bar inherited from the
 * pane. Unselected tabs are always restored to their base style.
 */
@OvatoxinDsl
class TabSelectionStyle internal constructor() {

    /** The fill painted on the selected tab, or `null` to keep the button's own fill. */
    var background: Color? = null
        set(value) {
            if (field == value) return
            field = value
            this.onChange?.invoke()
        }

    /** The text colour painted on the selected tab, or `null` to keep the inherited colour. */
    var foreground: Color? = null
        set(value) {
            if (field == value) return
            field = value
            this.onChange?.invoke()
        }

    /** Called whenever one of the properties changes, so the owning bar can repaint its tabs. */
    internal var onChange: (() -> Unit)? = null

    /**
     * Runs [block] as one change and notifies the owner once.
     *
     * The notification is suppressed while the block runs, so setting both properties repaints the
     * tabs once instead of once per property.
     */
    internal fun configure(block: TabSelectionStyle.() -> Unit) {
        val listener = this.onChange
        this.onChange = null
        try {
            block()
        } finally {
            this.onChange = listener
        }
        listener?.invoke()
    }

    /** Restores the defaults: no selected-tab override at all. */
    internal fun reset() {
        this.background = null
        this.foreground = null
    }
}
