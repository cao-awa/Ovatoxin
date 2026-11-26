package com.github.kusa233.ovatoxin.widget.container

import com.github.kusa233.ovatoxin.component.Container
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane

/**
 * The pane that sits inside the [ScrollPane] of a scrolling [Div] or [FlexBox].
 *
 * A surface places its children itself, with `isManaged = false`, so its inner pane cannot rely on
 * JavaFX to size it from them. It therefore reports the larger of the viewport it is given and the
 * extent its content reaches; that is what makes a short content fill the visible area while a long
 * one can be scrolled. The optional providers let a [FlexBox] keep answering with its own
 * single-line preferred size.
 */
internal class ScrollContentPane(
    private val preferredWidth: (Double) -> Double = { 0.0 },
    private val preferredHeight: (Double) -> Double = { 0.0 },
) : Pane() {

    init {
        isPickOnBounds = true
    }

    /** The visible height of the scroll viewport. */
    var viewportHeight: Double = 0.0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** How far the placed children reach below the top of the content pane. */
    var extentHeight: Double = 0.0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override fun computePrefWidth(height: Double): Double = maxOf(preferredWidth(height), 0.0)

    override fun computePrefHeight(width: Double): Double =
        maxOf(this.viewportHeight, this.extentHeight, preferredHeight(width))

    override fun computeMinWidth(height: Double): Double = computePrefWidth(height)

    override fun computeMinHeight(width: Double): Double = computePrefHeight(width)
}

/**
 * Builds and tracks the vertical [ScrollPane] around a [ScrollContentPane].
 *
 * Scroll bars are horizontal-never / vertical-as-needed and the content is fitted to the viewport
 * width, so only the up-down direction scrolls. The stylesheet attached here strips the Modena frame
 * and track, leaving the normal component background to show through.
 */
internal class ScrollSurface(
    private val owner: Container<*>,
    val content: ScrollContentPane,
) {

    private var scrollPane: ScrollPane? = null

    /** The visible height of the viewport, or `0` before the first layout. */
    val viewportHeight: Double get() = this.content.viewportHeight

    /** Creates the outer pane, or answers the one already created. */
    fun build(): ScrollPane {
        this.scrollPane?.let { return it }
        val scroll = ScrollPane(this.content)
        scroll.styleClass += "ovatoxin-scroll"
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        scroll.isFitToWidth = true
        scroll.isFitToHeight = false
        scroll.isPannable = true
        ScrollSurface::class.java.getResource("scroll.css")?.let { resource ->
            scroll.stylesheets += resource.toExternalForm()
        }
        scroll.viewportBoundsProperty().addListener { _, _, bounds ->
            this.content.viewportHeight = bounds.height
            // Percentages and child placement depend on the visible height, so the surface has to
            // lay itself out again whenever the viewport changes.
            this.owner.relayout()
        }
        this.scrollPane = scroll
        return scroll
    }

    /** Records how tall the placed content is; the inner pane grows to that, or the viewport. */
    fun setExtent(value: Double) {
        this.content.extentHeight = value.coerceAtLeast(0.0)
    }

    /** The actual node produced by [build]. */
    val node: Node? get() = this.scrollPane
}
