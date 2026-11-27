package com.github.kusa233.ovatoxin.widget.media

import com.github.kusa233.ovatoxin.component.Component
import com.github.kusa233.ovatoxin.component.SurfaceScope
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.runtime.Fx
import com.github.kusa233.ovatoxin.style.StyleVisual
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.util.Duration as FxDuration
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A black panel that plays a video from a local file, a classpath resource or a URL.
 *
 * ```kotlin
 * draw(VideoPane("media/intro.mp4")) { video ->
 *     layout {
 *         width = 80.percent
 *         height = 45.percent
 *     }
 *     style { corner = 12.px }
 *     video.autoPlay = true
 *     video.loop = true
 * }
 * ```
 *
 * [source] may be anything JavaFX's [Media] accepts: an `http`/`https` URL, a `file` URI, a
 * `jar` URI, a plain filesystem path, or a `classpath:` path. The convenience constructors for
 * [File], [URI] and [URL] do the obvious conversions, and `classpath:/media/intro.mp4` is resolved
 * with [Class.getResource].
 *
 * The video is drawn over a black fill, so a frame whose aspect ratio differs from the box shows
 * black bars instead of the page behind it. The fill can be changed through the ordinary
 * [style][com.github.kusa233.ovatoxin.component.DrawScope.style] block, and a non-zero
 * [corner][com.github.kusa233.ovatoxin.style.Style.corner] clips the picture to the same rounded
 * outline as the fill.
 *
 * ## Playback
 *
 * The player is created lazily, together with the JavaFX node, and only when a source is set. The
 * common playback controls are available as methods and properties:
 *
 * ```kotlin
 * video.play()
 * video.pause()
 * video.stop()
 * video.seekTo(12.seconds)
 * video.volume = 0.6          // 0.0 to 1.0, clamped
 * video.muted = true
 * video.loop = true
 * ```
 *
 * Calls made before the component is rendered are remembered and applied when the node is built:
 * `play()` on a component that has no node yet starts the video once it appears, and `seekTo(...)`
 * is applied as soon as the media is ready. [status], [isPlaying], [position] and [duration] read
 * the current state and return neutral values before a player exists.
 *
 * [onReady], [onEnd] and [onError] report the lifecycle of the media. All of them run on the
 * JavaFX application thread, like every other playback callback; [onStatusChanged] additionally
 * reports every transition of [MediaPlayer.Status].
 *
 * ## Lifecycle
 *
 * `javafx-media` is required at runtime. An application that only draws components which do not
 * play media does not need to load it, but a [VideoPane] will fail to initialize without the
 * module on the classpath.
 *
 * The player is released when the component is removed from its container, and [dispose] releases
 * it immediately. A redraw keeps the player: [reset] restores the properties the pane was created
 * with, and the draw block then writes what it wants again. When the resulting [source] is still
 * the same file the video keeps playing where it was; a different source reloads from the start.
 *
 * @param source the media URI, filesystem path or `classpath:` path to play.
 * @param autoPlay whether playback starts as soon as the media is ready. Defaults to `false`.
 * @param loop whether playback starts over when it reaches the end. Defaults to `false`.
 * @param muted whether the audio starts muted. Defaults to `false`.
 * @param volume the initial volume, a fraction from `0.0` to `1.0`; out-of-range values are clamped.
 * @param preserveRatio whether the picture keeps its aspect ratio instead of stretching to fill the
 *   box. Defaults to `true`.
 */
class VideoPane(
    source: String = "",
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muted: Boolean = false,
    volume: Double = DEFAULT_VOLUME,
    preserveRatio: Boolean = true,
) : Component<VideoPane>() {

    /** Creates a pane for a file on disk. */
    constructor(file: File) : this(file.toURI().toString())

    /** Creates a pane for a URI. */
    constructor(uri: URI) : this(uri.toString())

    /** Creates a pane for a URL. */
    constructor(url: URL) : this(url.toExternalForm())

    private val defaultSource: String = source
    private val defaultAutoPlay: Boolean = autoPlay
    private val defaultLoop: Boolean = loop
    private val defaultMuted: Boolean = muted
    private val defaultVolume: Double = clampVolume(volume)
    private val defaultPreserveRatio: Boolean = preserveRatio

    private var sourceValue: String = this.defaultSource
    private var autoPlayValue: Boolean = this.defaultAutoPlay
    private var loopValue: Boolean = this.defaultLoop
    private var mutedValue: Boolean = this.defaultMuted
    private var volumeValue: Double = this.defaultVolume
    private var preserveRatioValue: Boolean = this.defaultPreserveRatio

    private var root: StackPane? = null
    private var view: MediaView? = null
    private var player: MediaPlayer? = null
    private var clipShape: Rectangle? = null

    private var appliedSource: String? = null
    private var failedSource: String? = null
    private var syncScheduled: Boolean = false
    private var playRequested: Boolean = false
    private var mediaReady: Boolean = false
    private var pendingSeek: Duration? = null

    private var readyHandler: (() -> Unit)? = null
    private var endHandler: (() -> Unit)? = null
    private var errorHandler: ((Throwable?) -> Unit)? = null
    private var statusHandler: ((MediaPlayer.Status) -> Unit)? = null

    init {
        // A video needs something opaque behind it: the picture can have a different aspect ratio
        // from the box, and a transparent pane would show the page through the bars.
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
    }

    /**
     * The source this pane plays.
     *
     * Assigning a different value reloads the media; assigning the value it already has does
     * nothing, so a redraw that repeatedly writes the same path does not restart the video.
     */
    var source: String
        get() = this.sourceValue
        set(value) {
            if (value == this.sourceValue) return
            this.sourceValue = value
            this.failedSource = null
            this.pendingSeek = null
            requestMediaSync()
        }

    /** Replaces [source] and returns this pane, so the call chains. */
    fun source(value: String): VideoPane {
        this.source = value
        return this
    }

    /** Whether playback starts as soon as the media is ready. Defaults to `false`. */
    var autoPlay: Boolean
        get() = this.autoPlayValue
        set(value) {
            this.autoPlayValue = value
            this.player?.isAutoPlay = value || this.playRequested
        }

    /** Whether playback starts over at the end. Defaults to `false`. */
    var loop: Boolean
        get() = this.loopValue
        set(value) {
            this.loopValue = value
            applyCycleCount()
        }

    /** Whether the audio is muted. Defaults to `false`. */
    var muted: Boolean
        get() = this.mutedValue
        set(value) {
            this.mutedValue = value
            this.player?.isMute = value
        }

    /** The volume, a fraction from `0.0` to `1.0`. Values outside the range are clamped. */
    var volume: Double
        get() = this.volumeValue
        set(value) {
            val clamped = clampVolume(value)
            this.volumeValue = clamped
            this.player?.volume = clamped
        }

    /** Whether the picture keeps its aspect ratio instead of stretching. Defaults to `true`. */
    var preserveRatio: Boolean
        get() = this.preserveRatioValue
        set(value) {
            this.preserveRatioValue = value
            this.view?.isPreserveRatio = value
        }

    /** The current playback status, or `null` while no player exists. */
    val status: MediaPlayer.Status?
        get() = this.player?.status

    /** Whether the player is currently playing. */
    val isPlaying: Boolean
        get() = this.player?.status == MediaPlayer.Status.PLAYING

    /** The current playback position, or [Duration.ZERO] before a player exists. */
    val position: Duration
        get() {
            val millis = this.player?.currentTime?.toMillis() ?: return Duration.ZERO
            if (millis.isNaN() || millis <= 0.0) return Duration.ZERO
            return millis.roundToLong().milliseconds
        }

    /** The total duration, or [Duration.ZERO] while it is still unknown. */
    val duration: Duration
        get() {
            val total = this.player?.media?.duration ?: return Duration.ZERO
            if (total == FxDuration.UNKNOWN || total == FxDuration.INDEFINITE) return Duration.ZERO
            val millis = total.toMillis()
            if (millis.isNaN() || millis <= 0.0) return Duration.ZERO
            return millis.roundToLong().milliseconds
        }

    /**
     * Starts playback.
     *
     * Before the node exists the request is remembered and honoured when the media becomes ready;
     * afterwards the player starts immediately.
     */
    fun play(): VideoPane {
        this.playRequested = true
        val current = this.player
        if (current != null) {
            onFxThread { current.play() }
        } else {
            requestMediaSync()
        }
        return this
    }

    /** Pauses playback, keeping the current position. Does nothing before a player exists. */
    fun pause(): VideoPane {
        this.playRequested = false
        val current = this.player ?: return this
        onFxThread { current.pause() }
        return this
    }

    /** Stops playback and returns to the beginning. Does nothing before a player exists. */
    fun stop(): VideoPane {
        this.playRequested = false
        val current = this.player ?: return this
        onFxThread { current.stop() }
        return this
    }

    /** Pauses a playing video, or plays a paused or stopped one. */
    fun togglePlayPause(): VideoPane =
        if (this.isPlaying) pause() else play()

    /**
     * Moves playback to [position].
     *
     * A seek made before the media is ready is remembered and applied at the end of loading.
     */
    fun seekTo(position: Duration): VideoPane {
        val millis = position.inWholeMilliseconds.coerceAtLeast(0L)
        val current = this.player
        if (current != null && this.mediaReady) {
            onFxThread { current.seek(FxDuration.millis(millis.toDouble())) }
        } else {
            this.pendingSeek = millis.milliseconds
        }
        return this
    }

    /** Runs [handler] once the media is ready to play. Registering again replaces the previous one. */
    fun onReady(handler: () -> Unit): VideoPane {
        this.readyHandler = handler
        return this
    }

    /** Runs [handler] when playback reaches the end. Registering again replaces the previous one. */
    fun onEnd(handler: () -> Unit): VideoPane {
        this.endHandler = handler
        return this
    }

    /**
     * Runs [handler] when the media cannot be loaded or played.
     *
     * The argument is the [javafx.scene.media.MediaException] reported by JavaFX, or the exception
     * thrown while resolving the source — for example a `classpath:` path that does not exist.
     */
    fun onError(handler: (Throwable?) -> Unit): VideoPane {
        this.errorHandler = handler
        return this
    }

    /** Runs [handler] on every status change. Registering again replaces the previous one. */
    fun onStatusChanged(handler: (MediaPlayer.Status) -> Unit): VideoPane {
        this.statusHandler = handler
        return this
    }

    /**
     * Releases the player immediately and stops playback.
     *
     * The node stays usable: showing the pane again rebuilds the player from [source]. This is
     * called automatically when the component is removed from its container, so an application
     * normally does not have to call it.
     */
    fun dispose() {
        val current = this.player
        this.player = null
        this.view?.mediaPlayer = null
        this.mediaReady = false
        this.playRequested = false
        this.pendingSeek = null
        this.appliedSource = null
        this.failedSource = null
        if (current != null) {
            val release: () -> Unit = {
                runCatching { current.stop() }
                runCatching { current.dispose() }
            }
            if (Platform.isFxApplicationThread()) release() else Fx.dispatchToFxThread(release)
        }
    }

    override fun buildNode(): Node {
        val pane = StackPane()
        pane.isPickOnBounds = true

        val mediaView = MediaView()
        mediaView.isPreserveRatio = this.preserveRatioValue
        mediaView.isSmooth = true
        // Let clicks reach the pane, so a click handler on the VideoPane covers the whole picture.
        mediaView.isMouseTransparent = true
        mediaView.fitWidthProperty().bind(pane.widthProperty())
        mediaView.fitHeightProperty().bind(pane.heightProperty())
        pane.children += mediaView

        this.root = pane
        this.view = mediaView
        return pane
    }

    override fun onNodeCreated(node: Node) {
        super.onNodeCreated(node)
        syncMediaNow()
    }

    override fun applyStyle(width: Double, height: Double) {
        super.applyStyle(width, height)
        applyOutline(this.styleSpec.cornerPixels(width, height, this.resolvedFontSize), width, height)
        syncMediaNow()
    }

    override fun onStyleFrame(visual: StyleVisual) {
        super.onStyleFrame(visual)
        val pane = this.root ?: return
        applyOutline(visual.corner, pane.width, pane.height)
    }

    override fun onNodeDetached(node: Node) {
        this.root = null
        this.view = null
        this.clipShape = null
        dispose()
    }

    /**
     * Restores the values this pane was created with.
     *
     * The player is deliberately left alone: a redraw that merely repeats the same source should
     * not interrupt playback. [applyStyle] then reconciles the player with the restored values, so
     * a source that really changed reloads once, after the draw block has finished.
     */
    override fun reset() {
        super.reset()
        this.sourceValue = this.defaultSource
        this.autoPlayValue = this.defaultAutoPlay
        this.loopValue = this.defaultLoop
        this.mutedValue = this.defaultMuted
        this.volumeValue = this.defaultVolume
        this.preserveRatioValue = this.defaultPreserveRatio
        // `playRequested` and `pendingSeek` are commands, not constructor values. A call made
        // before the component was drawn must survive the reset that the first draw performs.
        this.readyHandler = null
        this.endHandler = null
        this.errorHandler = null
        this.statusHandler = null
        this.view?.isPreserveRatio = this.preserveRatioValue
        this.styleSpec.background { color = DEFAULT_BACKGROUND }
    }

    private fun applyOutline(radius: Double, width: Double, height: Double) {
        val pane = this.root ?: return
        if (width <= 0.0 || height <= 0.0) return
        if (radius <= 0.0) {
            pane.clip = null
            return
        }
        val shape = this.clipShape ?: Rectangle().also {
            this.clipShape = it
            pane.clip = it
        }
        shape.width = width
        shape.height = height
        shape.arcWidth = radius * 2.0
        shape.arcHeight = radius * 2.0
        if (pane.clip !== shape) pane.clip = shape
    }

    private fun requestMediaSync() {
        if (this.builtNode == null || this.syncScheduled) return
        this.syncScheduled = true
        try {
            Platform.runLater {
                this.syncScheduled = false
                syncMediaNow()
            }
        } catch (_: IllegalStateException) {
            // The runtime stopped; there is nothing left to paint.
            this.syncScheduled = false
        }
    }

    private fun syncMediaNow() {
        if (!Platform.isFxApplicationThread()) {
            requestMediaSync()
            return
        }
        val mediaView = this.view ?: return

        if (this.sourceValue == this.failedSource) return

        val resolved = try {
            resolveMediaUri(this.sourceValue)
        } catch (throwable: Throwable) {
            this.failedSource = this.sourceValue
            reportError(throwable)
            return
        }

        if (resolved.isEmpty()) {
            releasePlayer()
            this.appliedSource = null
            this.failedSource = null
            return
        }
        if (resolved == this.appliedSource && this.player != null) {
            applyPlayerSettings()
            return
        }

        releasePlayer()
        try {
            val media = Media(resolved)
            val created = MediaPlayer(media)
            this.player = created
            mediaView.mediaPlayer = created
            this.appliedSource = resolved
            this.mediaReady = false
            created.isAutoPlay = this.autoPlayValue || this.playRequested
            applyPlayerSettings()
            created.setOnReady {
                this.mediaReady = true
                this.pendingSeek?.let { seek ->
                    created.seek(FxDuration.millis(seek.inWholeMilliseconds.toDouble()))
                    this.pendingSeek = null
                }
                if (this.playRequested) created.play()
                this.readyHandler?.invoke()
            }
            created.setOnEndOfMedia { this.endHandler?.invoke() }
            created.setOnError { reportError(created.error) }
            created.statusProperty().addListener { _, _, status -> this.statusHandler?.invoke(status) }
        } catch (throwable: Throwable) {
            this.failedSource = this.sourceValue
            reportError(throwable)
        }
    }

    private fun applyPlayerSettings() {
        val current = this.player ?: return
        current.isAutoPlay = this.autoPlayValue || this.playRequested
        current.isMute = this.mutedValue
        current.volume = this.volumeValue
        applyCycleCount()
    }

    private fun applyCycleCount() {
        val current = this.player ?: return
        current.cycleCount = if (this.loopValue) MediaPlayer.INDEFINITE else 1
    }

    private fun releasePlayer() {
        val current = this.player ?: return
        this.player = null
        this.view?.mediaPlayer = null
        this.mediaReady = false
        runCatching { current.stop() }
        runCatching { current.dispose() }
    }

    private fun reportError(throwable: Throwable?) {
        this.errorHandler?.invoke(throwable)
    }

    private fun onFxThread(block: () -> Unit) {
        if (Platform.isFxApplicationThread()) block() else Fx.dispatchToFxThread(block)
    }

    companion object {
        /** The fill painted behind a video. */
        val DEFAULT_BACKGROUND: Color = Color.BLACK

        /** The default playback volume. */
        const val DEFAULT_VOLUME: Double = 1.0

        private fun clampVolume(value: Double): Double =
            if (value.isNaN()) DEFAULT_VOLUME else value.coerceIn(0.0, 1.0)

        /**
         * Turns a source into a URI JavaFX media can open.
         *
         * A `classpath:` prefix is resolved through [Class.getResource]; a Windows drive path is
         * accepted with either slash even though `URI` would read `C:` as a scheme; every other
         * relative path is made absolute through [File].
         */
        private fun resolveMediaUri(source: String): String {
            val value = source.trim()
            if (value.isEmpty()) return ""
            if (value.startsWith("classpath:")) {
                val resource = value.removePrefix("classpath:").let {
                    if (it.startsWith("/")) it else "/$it"
                }
                return VideoPane::class.java.getResource(resource)?.toExternalForm()
                    ?: throw IllegalArgumentException("No classpath resource at '$resource'")
            }
            if (value.length >= 2 && value[1] == ':' && value[0].isLetter()) {
                return File(value).toURI().toString()
            }
            val uri = runCatching { URI(value) }.getOrNull()
            if (uri != null && uri.isAbsolute) return uri.toString()
            return File(value).toURI().toString()
        }
    }
}

/**
 * Draws a [VideoPane] at an optional size.
 *
 * ```kotlin
 * frame.layout {
 *     videoPane("media/intro.mp4", w = 80.percent, h = 45.percent) {
 *         autoPlay = true
 *     }
 * }
 * ```
 *
 * The block runs with the pane as its receiver, so playback properties can be written directly.
 */
fun SurfaceScope<*>.videoPane(
    source: String = "",
    w: Length? = null,
    h: Length? = null,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muted: Boolean = false,
    volume: Double = VideoPane.DEFAULT_VOLUME,
    preserveRatio: Boolean = true,
    block: VideoPane.() -> Unit = {},
): VideoPane = draw(
    VideoPane(
        source = source,
        autoPlay = autoPlay,
        loop = loop,
        muted = muted,
        volume = volume,
        preserveRatio = preserveRatio,
    ),
) { video ->
    layout {
        if (w != null) width = w
        if (h != null) height = h
    }
    block(video)
}
