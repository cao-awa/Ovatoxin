package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.style.Style

/**
 * The scope of one [Container.draw] call.
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
 */
class DrawScope<T : Component<*>>(internal val component: T) {

    /** Configures the position and the size of the drawn component. */
    fun layout(block: Layout.() -> Unit): DrawScope<T> {
        this.component.layoutSpec.configure(block)
        return this
    }

    /** Configures the appearance of the drawn component. */
    fun style(block: Style.() -> Unit): DrawScope<T> {
        this.component.styleSpec.configure(block)
        return this
    }
}
