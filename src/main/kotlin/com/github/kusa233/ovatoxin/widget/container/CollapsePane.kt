package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.component.ContentBox
import com.github.kusa233.ovatoxin.component.Slot
import com.github.kusa233.ovatoxin.component.SurfaceScope
import com.github.kusa233.ovatoxin.dsl.OvatoxinDsl
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

/**
 * A panel with a clickable header that collapses or expands its body.
 *
 * Collapsed, only the header is visible and it takes [collapsedHeight] (by default the height of the
 * header). Expanded, the body is shown and the whole panel takes [expandedHeight]. The body scrolls
 * up and down on its own when its content is taller than the room left below the header.
 *
 * The body is declared with [content], which opens a normal [FlexBox] surface:
 *
 * ```kotlin
 * frame.layout {
 *     collapsePane("示例代码", expandedHeight = 260.px) {
 *         bareTextArea(code) {
 *             layout { width = 100.percent; height = 400.px }
 *         }
 *     }
 * }
 * ```
 *
 * The header itself is part of the panel and should not be replaced; use [title] to change its
 * caption and [expanded] to drive the state. Every constructor value survives a [reset], like the
 * values of a [Card].
 *
 * @param title the caption shown in the clickable header.
 * @param expandedHeight the height of the whole panel when expanded.
 * @param collapsedHeight the height of the whole panel when collapsed; the header height by default.
 * @param headerHeight the height of the clickable header row.
 * @param initiallyExpanded whether the body starts visible.
 * @param fill the background colour behind header and body.
 * @param borderColor the colour of the one-pixel outline, or `null` for no outline.
 * @param corner the corner radius, `12.px` by default.
 * @param headerPadding the horizontal inset of the header caption, in pixels.
 * @param bodyPadding the inset around the body content, in pixels.
 * @param bodyGap the vertical gap between two body children, in pixels.
 * @param headerForeground the colour of the header caption and its arrow.
 */
@OvatoxinDsl
class CollapsePane(
    title: String,
    expandedHeight: Length,
    collapsedHeight: Length? = null,
    headerHeight: Length = DEFAULT_HEADER_HEIGHT,
    initiallyExpanded: Boolean = false,
    fill: Color = DEFAULT_FILL,
    borderColor: Color? = DEFAULT_BORDER,
    corner: Length = DEFAULT_CORNER,
    headerPadding: Double = DEFAULT_HEADER_PADDING,
    bodyPadding: Double = DEFAULT_BODY_PADDING,
    bodyGap: Double = DEFAULT_BODY_GAP,
    headerForeground: Color = DEFAULT_HEADER_FOREGROUND,
) : ComponentDrawable<CollapsePane>() {

    private var titleValue: String = title
    private val defaultTitle: String = title
    private val panelWidth: Length = 100.percent
    private val panelExpandedHeight: Length = expandedHeight
    private val panelCollapsedHeight: Length = collapsedHeight ?: headerHeight
    private val panelHeaderHeight: Length = headerHeight
    private val panelInitiallyExpanded: Boolean = initiallyExpanded
    private val panelFill: Color = fill
    private val panelBorder: Color? = borderColor
    private val panelCorner: Length = corner
    private val panelHeaderPadding: Double = headerPadding
    private val panelBodyPadding: Double = bodyPadding
    private val panelBodyGap: Double = bodyGap
    private val panelHeaderForeground: Color = headerForeground

    private var expandedValue: Boolean = initiallyExpanded

    /** The clickable header row. It is a normal flex row holding the caption. */
    private val header: FlexBox = FlexBox()

    /** The caption and arrow of [header]. */
    private val headerLabel: TextArea = TextArea()

    /** The scrollable body below the header. */
    private val bodyPane: FlexBox = FlexBox()

    /** The resolved header height of the last layout pass, used to position the body. */
    private var resolvedHeaderHeight: Double = 0.0

    init {
        seedGeometry()
        seedStyle()

        this.header.gap = Length.ZERO
        this.header.justify = Justify.START
        this.header.align = Align.CENTER
        this.header.layout {
            draw(this@CollapsePane.headerLabel) { label ->
                layout {
                    width = 100.percent
                    height = 100.percent
                }
                style {
                    background { color = Color.TRANSPARENT }
                    foreground = this@CollapsePane.panelHeaderForeground
                    fontSize = 13.px
                }
                label.bold = true
            }
        }

        this.bodyPane.gap = this.panelBodyGap.px
        this.bodyPane.scrollY = true

        this.layout {
            draw(this@CollapsePane.header) {
                layout {
                    width = 100.percent
                    hPadding = this@CollapsePane.panelHeaderPadding.px
                    vPadding = Length.ZERO
                    height = this@CollapsePane.panelHeaderHeight
                }
                style {
                    // The header is transparent so the whole panel keeps one continuous fill.
                    background { color = Color.TRANSPARENT }
                }
                this@CollapsePane.header.onClick { this@CollapsePane.toggle() }
            }
            draw(this@CollapsePane.bodyPane) {
                layout {
                    width = 100.percent
                    hPadding = this@CollapsePane.panelBodyPadding.px
                    vPadding = this@CollapsePane.panelBodyPadding.px
                    height = Length.ZERO
                }
                style {
                    // The body surface is transparent so the CollapsePane fill shows through.
                    background { color = Color.TRANSPARENT }
                }
            }
        }

        refreshHeaderText()
        this.bodyPane.visible = this.expandedValue
    }

    /** The scrollable body, exposed for inspection and for tests. */
    val contentArea: FlexBox
        get() = this.bodyPane

    /** Whether the body is currently shown. */
    var expanded: Boolean
        get() = this.expandedValue
        set(value) {
            if (this.expandedValue == value) return
            this.expandedValue = value
            applyExpandedState()
        }

    /** The caption shown in the header. */
    var title: String
        get() = this.titleValue
        set(value) {
            if (this.titleValue == value) return
            this.titleValue = value
            refreshHeaderText()
        }

    /**
     * Draws the body of this panel.
     *
     * The block runs on the body's own [FlexBox] surface, so `draw(...)` and the usual layout blocks
     * work exactly as in any other container. Calling it again replaces the body content on the
     * next replay.
     */
    fun content(block: SurfaceScope<FlexBox>.() -> Unit): CollapsePane {
        this.bodyPane.layout(block)
        return this
    }

    /** Shows the body if it was hidden, or hides it if it was shown. */
    fun toggle() {
        this.expanded = !this.expandedValue
    }

    override fun buildNode(): Node {
        val pane = Pane()
        pane.isPickOnBounds = true
        return pane
    }

    override fun reset() {
        super.reset()
        this.titleValue = this.defaultTitle
        this.expandedValue = this.panelInitiallyExpanded
        seedGeometry()
        seedStyle()
        this.bodyPane.visible = this.expandedValue
        refreshHeaderText()
    }

    /**
     * The body is placed directly below the header; every other child, if any, falls back to the
     * top-left corner like a plain container.
     */
    internal override fun slotFor(child: Component<*>, box: ContentBox): Slot = when (child) {
        this.header -> Slot(box.offsetX, box.offsetY)
        this.bodyPane -> Slot(box.offsetX, box.offsetY + this.resolvedHeaderHeight)
        else -> Slot(box.offsetX, box.offsetY)
    }

    /**
     * Resolves the header and body heights for this pass.
     *
     * The outer panel already has the collapsed or expanded height as its layout height, so the
     * body simply receives whatever remains below the header when expanded. When collapsed it gets
     * no height and is also hidden.
     */
    internal override fun beginPass(box: ContentBox, targets: List<Component<*>>) {
        val headerHeight = this.panelHeaderHeight
            .resolve(box.height, this.resolvedFontSize)
            .coerceAtLeast(0.0)
        this.resolvedHeaderHeight = headerHeight
        this.header.layoutSpec.height = headerHeight.px
        val bodyHeight = if (this.expandedValue) {
            (box.height - headerHeight).coerceAtLeast(0.0)
        } else {
            0.0
        }
        this.bodyPane.layoutSpec.height = bodyHeight.px
    }

    private fun applyExpandedState() {
        this.layoutSpec.height = if (this.expandedValue) this.panelExpandedHeight else this.panelCollapsedHeight
        this.bodyPane.visible = this.expandedValue
        refreshHeaderText()
        // The panel changed its own height, so both its parent (which gives it that height) and its
        // own children have to be laid out again.
        this.parent?.relayout()
        this.relayout()
    }

    private fun refreshHeaderText() {
        this.headerLabel.text(if (this.expandedValue) "▾ $titleValue" else "▸ $titleValue")
    }

    private fun seedGeometry() {
        this.layoutSpec.width = this.panelWidth
        this.layoutSpec.height = if (this.expandedValue) this.panelExpandedHeight else this.panelCollapsedHeight
    }

    private fun seedStyle() {
        this.styleSpec.background { color = this@CollapsePane.panelFill }
        this.styleSpec.corner = this.panelCorner
        this.styleSpec.fontSize = 14.px
        if (this.panelBorder != null) {
            this.styleSpec.border {
                size = 1.px
                color = this@CollapsePane.panelBorder
            }
        }
    }

    companion object {
        /** The default fill behind a collapse pane. */
        val DEFAULT_FILL: Color = Color.WHITE

        /** The default outline colour. */
        val DEFAULT_BORDER: Color? = Color.web("#e4e9f2")

        /** The default corner radius. */
        val DEFAULT_CORNER: Length = 12.px

        /** The height of the header row, and the default collapsed height. */
        val DEFAULT_HEADER_HEIGHT: Length = 40.px

        /** The distance from the header caption to the panel edge. */
        const val DEFAULT_HEADER_PADDING: Double = 12.0

        /** The inset around the body content. */
        const val DEFAULT_BODY_PADDING: Double = 12.0

        /** The vertical gap between two body children. */
        const val DEFAULT_BODY_GAP: Double = 8.0

        /** The default caption and arrow colour. */
        val DEFAULT_HEADER_FOREGROUND: Color = Color.web("#162033")
    }
}

/**
 * Draws a [CollapsePane] and declares its body in one call.
 *
 * ```kotlin
 * frame.layout {
 *     collapsePane("示例代码", expandedHeight = 260.px) {
 *         bareTextArea("val answer = 42")
 *     }
 * }
 * ```
 */
fun SurfaceScope<*>.collapsePane(
    title: String,
    expandedHeight: Length,
    collapsedHeight: Length? = null,
    headerHeight: Length = CollapsePane.DEFAULT_HEADER_HEIGHT,
    initiallyExpanded: Boolean = false,
    fill: Color = CollapsePane.DEFAULT_FILL,
    borderColor: Color? = CollapsePane.DEFAULT_BORDER,
    corner: Length = CollapsePane.DEFAULT_CORNER,
    headerPadding: Double = CollapsePane.DEFAULT_HEADER_PADDING,
    bodyPadding: Double = CollapsePane.DEFAULT_BODY_PADDING,
    bodyGap: Double = CollapsePane.DEFAULT_BODY_GAP,
    headerForeground: Color = CollapsePane.DEFAULT_HEADER_FOREGROUND,
    body: SurfaceScope<FlexBox>.() -> Unit = {},
): CollapsePane = draw(
    CollapsePane(
        title = title,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        headerHeight = headerHeight,
        initiallyExpanded = initiallyExpanded,
        fill = fill,
        borderColor = borderColor,
        corner = corner,
        headerPadding = headerPadding,
        bodyPadding = bodyPadding,
        bodyGap = bodyGap,
        headerForeground = headerForeground,
    ),
) { pane ->
    pane.content(body)
}
