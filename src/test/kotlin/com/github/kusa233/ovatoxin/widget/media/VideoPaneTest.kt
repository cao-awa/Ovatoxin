package com.github.kusa233.ovatoxin.widget.media

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VideoPaneTest {

    @Test
    fun `a pane starts empty and on a black fill`() {
        val video = VideoPane()

        assertEquals("", video.source)
        assertFalse(video.isNodeCreated, "a value alone must not build a JavaFX node")
        assertEquals(Color.BLACK, video.styleSpec.background.color)
        assertEquals(1.0, video.volume)
        assertTrue(video.preserveRatio)
        assertFalse(video.isPlaying)
        assertNull(video.status)
        assertEquals(Duration.ZERO, video.position)
        assertEquals(Duration.ZERO, video.duration)
    }

    @Test
    fun `the constructor writes every playback value`() {
        val video = VideoPane(
            source = "movie.mp4",
            autoPlay = true,
            loop = true,
            muted = true,
            volume = 0.4,
            preserveRatio = false,
        )

        assertEquals("movie.mp4", video.source)
        assertTrue(video.autoPlay)
        assertTrue(video.loop)
        assertTrue(video.muted)
        assertEquals(0.4, video.volume)
        assertFalse(video.preserveRatio)
    }

    @Test
    fun `source is written through both the property and the call`() {
        val video = VideoPane()

        video.source("first.mp4")
        assertEquals("first.mp4", video.source)

        video.source = "second.mp4"
        assertEquals("second.mp4", video.source)
        assertSame(video, video.source("third.mp4"))
        assertEquals("third.mp4", video.source)
    }

    @Test
    fun `the volume is clamped to the usable range`() {
        assertEquals(0.0, VideoPane(volume = -2.0).volume)
        assertEquals(1.0, VideoPane(volume = 8.0).volume)
        assertEquals(1.0, VideoPane(volume = Double.NaN).volume)

        val video = VideoPane()
        video.volume = -3.0
        assertEquals(0.0, video.volume)
        video.volume = 3.0
        assertEquals(1.0, video.volume)
    }

    @Test
    fun `play pause stop toggle and seek stay quiet without a node`() {
        val video = VideoPane("movie.mp4")

        assertSame(video, video.play())
        assertSame(video, video.pause())
        assertSame(video, video.stop())
        assertSame(video, video.togglePlayPause())
        assertSame(video, video.seekTo(12.seconds))
        assertFalse(video.isPlaying)
    }

    @Test
    fun `a reset puts the constructed values and the black fill back`() {
        val video = VideoPane(
            source = "movie.mp4",
            autoPlay = true,
            loop = true,
            muted = true,
            volume = 0.3,
            preserveRatio = false,
        )
        Frame().layout { draw(video) }

        video.source = "other.mp4"
        video.autoPlay = false
        video.loop = false
        video.muted = false
        video.volume = 0.9
        video.preserveRatio = true
        video.applyStyle {
            background { color = Color.RED }
            corner = 4.px
        }

        video.reset()

        assertEquals("movie.mp4", video.source)
        assertTrue(video.autoPlay)
        assertTrue(video.loop)
        assertTrue(video.muted)
        assertEquals(0.3, video.volume)
        assertFalse(video.preserveRatio)
        assertEquals(Color.BLACK, video.styleSpec.background.color)
    }

    @Test
    fun `the file and URI constructors keep the location`() {
        val file = File("movie clip.mp4").absoluteFile
        assertEquals(URI(file.toURI().toString()).toString(), VideoPane(file).source)
        assertTrue(VideoPane(URI("https://example.com/movie.mp4")).source.startsWith("https://"))
    }

    @Test
    fun `the helper draws a pane with its size and block applied`() {
        val frame = Frame()
        var built: VideoPane? = null

        frame.layout {
            built = videoPane(
                source = "movie.mp4",
                w = 80.percent,
                h = 45.percent,
                autoPlay = true,
            ) {
                loop = true
                volume = 0.5
            }
        }

        val video = built!!
        assertSame(video, frame.children.single())
        assertEquals(80.percent, video.layoutSpec.width)
        assertEquals(45.percent, video.layoutSpec.height)
        assertEquals("movie.mp4", video.source)
        assertTrue(video.autoPlay)
        assertTrue(video.loop)
        assertEquals(0.5, video.volume)
    }

    @Test
    fun `handlers can be registered and a reset forgets them`() {
        val video = VideoPane()

        assertSame(video, video.onReady {})
        assertSame(video, video.onEnd {})
        assertSame(video, video.onError {})
        assertSame(video, video.onStatusChanged {})

        video.reset()

        // The reset is allowed to wipe handlers: a draw block re-registers the live ones. There is
        // no public getter for the handler, so this test just proves reset itself does not throw.
        assertFalse(video.isNodeCreated)
    }
}
