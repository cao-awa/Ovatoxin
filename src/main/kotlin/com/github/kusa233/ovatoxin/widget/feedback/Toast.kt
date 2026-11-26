package com.github.kusa233.ovatoxin.widget.feedback

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.SurfaceScope
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.runtime.Fx
import com.github.kusa233.ovatoxin.style.cssColor
import com.github.kusa233.ovatoxin.widget.container.FlexBox
import javafx.animation.Animation
import javafx.animation.FadeTransition
import javafx.animation.Interpolator
import javafx.animation.PauseTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import javafx.util.Duration as FxDuration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A colour that identifies what a [Toast] is telling the user.
 *
 * The tone controls the card's fill and text colour; it does not change the message or how long the
 * toast stays on screen. The four documented tones cover the usual information/confirmation/warning/
 * error split, and the constructor is public so an application can add its own pair of colours:
 *
 * ```kotlin
 * val brandTone = ToastTone(Color.web("#4c8bf5"), Color.WHITE)
 * toast.show("已保存", tone = brandTone)
 * ```
 *
 * @property background the fill of the toast card.
 * @property foreground the colour of the message text.
 */
data class ToastTone(
    val background: Color,
    val foreground: Color,
) {
    companion object {
        /** Neutral information: a dark navy card with white text. */
        val INFO: ToastTone = ToastTone(Color.web("#101b33"), Color.WHITE)

        /** A completed action: green. */
        val SUCCESS: ToastTone = ToastTone(Color.web("#047857"), Color.WHITE)

        /** Something the user should notice: amber. */
        val WARNING: ToastTone = ToastTone(Color.web("#b45309"), Color.WHITE)

        /** A failed action: red. */
        val ERROR: ToastTone = ToastTone(Color.web("#be123c"), Color.WHITE)
    }
}

/**
 * Where a [Toast] card sits inside its overlay.
 *
 * The overlay itself always covers the whole surface it is drawn into, so the position only moves
 * the card within that surface. The default is [BOTTOM_CENTER], the conventional place for a
 * transient notification because it stays out of the way of the content above it.
 */
enum class ToastPosition(internal val alignment: Pos, internal val margin: Insets) {
    /** Top left corner. */
    TOP_START(Pos.TOP_LEFT, Insets(28.0, 28.0, 0.0, 28.0)),

    /** Top edge, horizontally centred. */
    TOP_CENTER(Pos.TOP_CENTER, Insets(28.0, 24.0, 0.0, 24.0)),

    /** Top right corner. */
    TOP_END(Pos.TOP_RIGHT, Insets(28.0, 28.0, 0.0, 28.0)),

    /** Bottom left corner. */
    BOTTOM_START(Pos.BOTTOM_LEFT, Insets(0.0, 28.0, 28.0, 28.0)),

    /** Bottom edge, horizontally centred. */
    BOTTOM_CENTER(Pos.BOTTOM_CENTER, Insets(0.0, 24.0, 28.0, 24.0)),

    /** Bottom right corner. */
    BOTTOM_END(Pos.BOTTOM_RIGHT, Insets(0.0, 28.0, 28.0, 28.0)),

    /** The exact centre of the surface, for messages that should interrupt the page. */
    CENTER(Pos.CENTER, Insets(0.0)),
}

/**
 * A transient message layer: call [show] and a small rounded card fades in, waits for the requested
 * duration, then fades out on its own.
 *
 * A toast is an **overlay**, not a widget that takes part in the page flow. Draw it as the last child
 * of the surface it should cover, normally the window itself:
 *
 * ```kotlin
 * val toast = Toast()
 *
 * frame.layout {
 *     draw(TabPane()) { ... }
 *
 *     // Last child: the toast floats above everything drawn before it.
 *     draw(toast) {
 *         layout {
 *             width = 100.percent
 *             height = 100.percent
 *         }
 *     }
 * }
 *
 * toast.show("已保存")
 * ```
 *
 * The `toast()` factory in a [SurfaceScope] does the same geometry for you. By default the overlay
 * is mouse-transparent, so it never blocks the controls underneath.
 *
 * Calling [show] again replaces the current card and restarts the timer. The message, tone and
 * duration can each be supplied per call, with the constructor values as defaults:
 *
 * ```kotlin
 * val toast = Toast(duration = 3.seconds, tone = ToastTone.INFO)
 * toast.show("普通消息")
 * toast.show("保存成功", tone = ToastTone.SUCCESS)
 * toast.show("网络错误", tone = ToastTone.ERROR, position = ToastPosition.CENTER)
 * ```
 *
 * Besides a plain [String] message, [show] accepts a drawing block. The block receives a
 * [FlexBox] surface, so any component can be drawn into the card:
 *
 * ```kotlin
 * toast.show(position = ToastPosition.CENTER, dimOutside = true) {
 *     draw(TextArea("确认删除？")) { ... }
 *     draw(Button("取消")) { it.onClick { toast.dismiss() } }
 * }
 * ```
 *
 * With [dimOutside] set, a translucent scrim covers everything outside the card and the overlay
 * stops forwarding clicks to the page below. The toast card itself stays interactive.
 *
 * @param duration how long a toast stays visible by default. It can be anything positive, including
 *   [Duration.INFINITE] for a card that stays until the next [show] or [dismiss].
 * @param tone the default card tone.
 * @param position where the card sits in the overlay.
 * @param dimOutside whether to dim (and block) the surface outside the card by default.
 */
class Toast(
    val duration: Duration = DEFAULT_DURATION,
    val tone: ToastTone = ToastTone.INFO,
    val position: ToastPosition = ToastPosition.BOTTOM_CENTER,
    val dimOutside: Boolean = false,
) : Component<Toast>() {

    private sealed interface Request {
        val duration: Duration
        val tone: ToastTone
        val position: ToastPosition
        val dimOutside: Boolean
        val message: String?
    }

    private data class MessageRequest(
        override val message: String,
        override val duration: Duration,
        override val tone: ToastTone,
        override val position: ToastPosition,
        override val dimOutside: Boolean,
    ) : Request

    private class ContentRequest(
        override val duration: Duration,
        override val tone: ToastTone,
        override val position: ToastPosition,
        override val dimOutside: Boolean,
        val content: SurfaceScope<FlexBox>.() -> Unit,
    ) : Request {
        override val message: String? get() = null
    }

    init {
        // The overlay is a transparent layer; only the individual cards paint themselves.
        this.styleSpec.background { color = null }
    }

    @Volatile
    private var currentRequest: Request? = null

    /** The request waiting for the overlay node to be created, if it does not exist yet. */
    private var pendingRequest: Request? = null

    private var card: Node? = null

    /** The dimming layer shown for a [ContentRequest] that asks for [Request.dimOutside]. */
    private var scrim: Region? = null

    private var timer: PauseTransition? = null

    private var animation: Animation? = null

    /** Whether a toast message is currently shown (or waiting to be shown). */
    val isShowing: Boolean
        get() = this.currentRequest != null

    /** The message of the current toast, or `null` when it draws custom content or nothing is shown. */
    val message: String?
        get() = this.currentRequest?.message

    /** The tone of the current toast, or `null` when nothing is shown. */
    val currentTone: ToastTone?
        get() = this.currentRequest?.tone

    /** The position of the current toast, or `null` when nothing is shown. */
    val currentPosition: ToastPosition?
        get() = this.currentRequest?.position

    /** Whether the current toast dims the surface outside its card, or `null` when nothing is shown. */
    val currentDimOutside: Boolean?
        get() = this.currentRequest?.dimOutside

    /**
     * Shows [message], replacing any card that is already visible.
     *
     * The call is safe from any thread: before the toolkit is running it only records the request,
     * and once the overlay is in a scene the card is created on the JavaFX application thread.
     */
    fun show(
        message: String,
        duration: Duration = this.duration,
        tone: ToastTone = this.tone,
        position: ToastPosition = this.position,
        dimOutside: Boolean = this.dimOutside,
    ) {
        submit(MessageRequest(message, duration, tone, position, dimOutside))
    }

    /**
     * Shows a card whose body is drawn by [content], replacing any card already visible.
     *
     * The block runs on the JavaFX application thread when the card is created, with a [FlexBox]
     * surface of a fixed content width. Every ordinary component can be drawn into it. The toast
     * card grows vertically to fit the content, so a short row of buttons and a taller form both
     * look right.
     *
     * ```kotlin
     * toast.show(position = ToastPosition.CENTER) {
     *     draw(TextArea("已同步")) { ... }
     *     draw(Button("好的")) { it.onClick { toast.dismiss() } }
     * }
     * ```
     */
    fun show(
        duration: Duration = this.duration,
        tone: ToastTone = this.tone,
        position: ToastPosition = this.position,
        dimOutside: Boolean = this.dimOutside,
        content: SurfaceScope<FlexBox>.() -> Unit,
    ) {
        submit(ContentRequest(duration, tone, position, dimOutside, content))
    }

    private fun submit(request: Request) {
        this.currentRequest = request
        Fx.dispatchToFxThread { present(request) }
    }

    /** Hides the current toast and stops its timer. Safe from any thread. */
    fun dismiss() {
        if (this.currentRequest == null) return
        this.currentRequest = null
        this.pendingRequest = null
        Fx.dispatchToFxThread { clearCard(animate = true) }
    }

    override fun buildNode(): Node {
        val layer = StackPane()
        layer.styleClass += STYLE_CLASS
        // The layer spans the whole surface but must not steal clicks from the page below.
        layer.isPickOnBounds = false
        layer.isMouseTransparent = true
        return layer
    }

    override fun onNodeCreated(node: Node) {
        super.onNodeCreated(node)
        val pending = this.pendingRequest
        if (pending != null) {
            this.pendingRequest = null
            present(pending)
        }
    }

    override fun reset() {
        super.reset()
        // A redraw restores the transparent layer; it deliberately leaves active messages alone.
        this.styleSpec.background { color = null }
    }

    private fun present(request: Request) {
        val host = this.builtNode as? StackPane
        if (host == null) {
            // The overlay has not been drawn yet: remember the newest request and show it when the
            // node is created. Replacing the pending request matches the "latest toast wins" rule.
            this.pendingRequest = request
            return
        }

        clearCard(animate = false)
        applyScrim(host, request.dimOutside, interactive = request is ContentRequest)

        val card = when (request) {
            is MessageRequest -> messageCard(request)
            is ContentRequest -> contentCard(request)
        }
        StackPane.setAlignment(card, request.position.alignment)
        StackPane.setMargin(card, request.position.margin)
        host.children += card
        this.card = card

        val fadeIn = FadeTransition(FxDuration.millis(FADE_IN_MILLIS), card)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.interpolator = Interpolator.EASE_OUT
        this.animation = fadeIn
        fadeIn.play()

        scheduleDismiss(request)
    }

    /** The ordinary text toast: a single wrapping [Label] with a compact, text-sized card. */
    private fun messageCard(request: MessageRequest): Label {
        val message = request.message
        val width = preferredCardWidth(message)
        return Label(message).apply {
            styleClass += CARD_STYLE_CLASS
            style = cardStyle(request.tone)
            isWrapText = true
            prefWidth = width
            maxWidth = width
            // Let the card be as tall as its text needs instead of filling the whole overlay.
            maxHeight = Region.USE_PREF_SIZE
            textAlignment = TextAlignment.CENTER
            alignment = Pos.CENTER
            isMouseTransparent = true
            opacity = 0.0
        }
    }

    /**
     * The custom-content toast: a card whose body is a real [FlexBox] surface, so the drawing block
     * receives the normal `draw(...)` DSL and can place any component.
     */
    private fun contentCard(request: ContentRequest): VBox {
        val body = FlexBox()
        body.gap = CONTENT_GAP.px
        // A FlexBox defaults to a white fill. Inside the toast that white sheet would cover the
        // tone-coloured card, so make the drawing surface transparent and let the card show through.
        body.applyStyle {
            background {
                color = null
            }
        }
        // Touch the lazily built node so the fixed content width is in place before children arrive.
        val bodyNode = body.node as Region
        bodyNode.minWidth = CONTENT_WIDTH
        bodyNode.prefWidth = CONTENT_WIDTH
        bodyNode.maxWidth = CONTENT_WIDTH
        body.layout { request.content(this) }
        // A FlexBox reports the height of a single line when asked for its preferred size, so VBox
        // would clip the wrapped rows. Measure each child against the fixed content width instead.
        val children = body.children
        val bodyHeight = if (children.isEmpty()) {
            0.0
        } else {
            children.sumOf { child ->
                child.layoutSpec.height?.resolve(CONTENT_WIDTH, child.resolvedFontSize)
                    ?: child.node.prefHeight(CONTENT_WIDTH)
            } + CONTENT_GAP * (children.size - 1)
        }
        bodyNode.minHeight = bodyHeight
        bodyNode.prefHeight = bodyHeight
        bodyNode.maxHeight = bodyHeight
        bodyNode.resize(CONTENT_WIDTH, bodyHeight)
        body.relayout()

        return VBox(bodyNode).apply {
            styleClass += CARD_STYLE_CLASS
            style = cardStyle(request.tone, CONTENT_CARD_PADDING, includeTextStyle = false)
            // Let StackPane use the computed size instead of stretching the card to the overlay.
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            alignment = Pos.TOP_LEFT
            isMouseTransparent = false
            opacity = 0.0
        }
    }

    /**
     * Adds or removes the dim layer and changes whether the overlay accepts input.
     *
     * A custom-content card can contain buttons, so the overlay must accept input for it even when
     * it does not dim the page. With [enabled] false and [interactive] true the card itself is
     * interactive while the empty surrounding area still lets clicks through.
     */
    private fun applyScrim(host: StackPane, enabled: Boolean, interactive: Boolean) {
        clearScrim()
        host.isMouseTransparent = !(enabled || interactive)
        host.isPickOnBounds = enabled
        if (!enabled) return

        val layer = Region().apply {
            styleClass += SCRIM_STYLE_CLASS
            style = SCRIM_STYLE
            isMouseTransparent = true
            minWidth = 0.0
            minHeight = 0.0
            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE
        }
        // First child, so the card drawn afterwards stays on top of the dim layer.
        host.children.add(0, layer)
        this.scrim = layer
    }

    private fun clearScrim() {
        val layer = this.scrim ?: return
        (layer.parent as? StackPane)?.children?.remove(layer)
        this.scrim = null
    }

    private fun scheduleDismiss(request: Request) {
        this.timer?.stop()
        this.timer = null
        if (request.duration <= Duration.ZERO || request.duration == Duration.INFINITE) return

        val pause = PauseTransition(FxDuration.millis(request.duration.inWholeMilliseconds.toDouble()))
        pause.setOnFinished {
            if (this.currentRequest === request) {
                this.currentRequest = null
                clearCard(animate = true)
            }
        }
        this.timer = pause
        pause.play()
    }

    private fun clearCard(animate: Boolean) {
        this.timer?.stop()
        this.timer = null
        this.animation?.stop()
        this.animation = null

        val host = this.builtNode as? StackPane
        val current = this.card
        if (!animate || current == null) {
            this.card = null
            if (current != null) {
                (current.parent as? StackPane)?.children?.remove(current)
            }
            clearScrim()
            host?.isMouseTransparent = true
            host?.isPickOnBounds = false
            return
        }

        // Keep [card] pointing at the node while it fades out. If a new message arrives in the
        // meantime, [present] calls [clearCard] without animation, which stops this fade and removes
        // the old node immediately instead of letting two cards overlap.
        val fadeOut = FadeTransition(FxDuration.millis(FADE_OUT_MILLIS), current)
        fadeOut.toValue = 0.0
        fadeOut.interpolator = Interpolator.EASE_IN
        fadeOut.setOnFinished {
            (current.parent as? StackPane)?.children?.remove(current)
            if (this.card === current) {
                this.card = null
                clearScrim()
                host?.isMouseTransparent = true
                host?.isPickOnBounds = false
            }
        }
        this.animation = fadeOut
        fadeOut.play()
    }

    /**
     * An estimate of the width the card should take, so a short toast is a compact pill and a long
     * one grows to [MAX_WIDTH] before wrapping. The estimate only decides the horizontal size; the
     * label itself still wraps and measures its real text.
     */
    private fun preferredCardWidth(message: String): Double {
        var width = CARD_HORIZONTAL_PADDING
        var index = 0
        while (index < message.length) {
            val codePoint = message.codePointAt(index)
            width += when {
                codePoint == '\n'.code || codePoint == '\r'.code -> 0.0
                Character.isWhitespace(codePoint) -> 4.5
                codePoint > 0x2E80 -> 14.5
                else -> 7.6
            }
            index += Character.charCount(codePoint)
        }
        return width.coerceIn(MIN_WIDTH, MAX_WIDTH)
    }

    /**
     * The chrome every toast card shares, plus the text styling a plain message card needs.
     *
     * A custom-content card passes [includeTextStyle] as `false`: its children are ordinary
     * components that bring their own foreground and typography, and a card-wide
     * `-fx-text-fill`/`-fx-font-weight` would otherwise decide their look for them.
     */
    private fun cardStyle(
        tone: ToastTone,
        padding: String = CARD_TEXT_PADDING,
        includeTextStyle: Boolean = true,
    ): String = buildString {
        append("-fx-background-color: ").append(cssColor(tone.background)).append(';')
        append("-fx-background-radius: 12;")
        append("-fx-background-insets: 0;")
        append("-fx-padding: ").append(padding).append(';')
        if (includeTextStyle) {
            append("-fx-text-fill: ").append(cssColor(tone.foreground)).append(';')
            append("-fx-font-size: 14px;")
            append("-fx-font-weight: bold;")
        }
        append("-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.28), 18, 0, 0, 6);")
    }

    companion object {
        /** The style class put on the full-surface overlay node. */
        const val STYLE_CLASS: String = "ovatoxin-toast"

        /** The style class put on every toast card. */
        const val CARD_STYLE_CLASS: String = "ovatoxin-toast-card"

        /** The style class put on the translucent layer behind a dimmed toast. */
        const val SCRIM_STYLE_CLASS: String = "ovatoxin-toast-scrim"

        /** The default time a toast stays visible. */
        val DEFAULT_DURATION: Duration = 2500.milliseconds

        /** The widest a text card may grow before its text wraps. */
        const val MAX_WIDTH: Double = 560.0

        /** The narrowest a text card may shrink to. */
        const val MIN_WIDTH: Double = 136.0

        /** Width of the drawing surface inside a custom-content card. */
        const val CONTENT_WIDTH: Double = 420.0

        /** Left and right padding of a text card plus a small safety margin, in pixels. */
        private const val CARD_HORIZONTAL_PADDING: Double = 52.0

        private const val CARD_TEXT_PADDING: String = "12 22 12 22"

        private const val CONTENT_CARD_PADDING: String = "16 20 16 20"

        private const val CONTENT_GAP: Double = 10.0

        private const val SCRIM_STYLE: String = "-fx-background-color: rgba(15, 23, 42, 0.42);"

        private const val FADE_IN_MILLIS: Double = 160.0

        private const val FADE_OUT_MILLIS: Double = 140.0
    }
}

/**
 * Draws a full-surface [Toast] overlay into this surface.
 *
 * It is a convenience over `draw(Toast(...)) { layout { width = 100.percent; height = 100.percent } }`.
 * Draw it **after** the content it should cover, so it is the topmost child:
 *
 * ```kotlin
 * frame.layout {
 *     draw(TabPane()) { ... }
 *     val toast = toast()
 *     toast.show("已保存")
 * }
 * ```
 *
 * @param duration the default visible time of a message.
 * @param tone the default tone of a message.
 * @param position where the card sits inside the overlay.
 * @param dimOutside whether the default card dims (and blocks) the surface outside it.
 */
fun SurfaceScope<*>.toast(
    duration: Duration = Toast.DEFAULT_DURATION,
    tone: ToastTone = ToastTone.INFO,
    position: ToastPosition = ToastPosition.BOTTOM_CENTER,
    dimOutside: Boolean = false,
): Toast = draw(Toast(duration, tone, position, dimOutside)) { toast ->
    layout {
        width = 100.percent
        height = 100.percent
    }
}
