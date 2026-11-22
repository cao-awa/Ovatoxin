package com.github.kusa233.ovatoxin.runtime

import java.awt.Desktop
import java.net.URI

/**
 * Opens URLs in the browser of the operating system.
 *
 * This is the seam through which [com.github.kusa233.ovatoxin.widget.Link] reaches the outside
 * world. Tests replace [opener] so that clicking a link never launches a real browser:
 *
 * ```kotlin
 * Browser.opener = { url -> opened += url }
 * ```
 *
 * The default implementation asks the desktop environment for its default browser and falls back
 * to the platform's command line helper where [Desktop] is unavailable.
 */
object Browser {

    /**
     * The function that actually opens a URL. Replaceable, mainly so tests can observe clicks
     * without starting a browser.
     */
    @Volatile
    var opener: (String) -> Unit = { url -> openWithDesktop(url) }

    /** Opens [url] with whatever [opener] currently is. */
    fun open(url: String) {
        opener(url)
    }

    /** Restores the platform default. */
    fun reset() {
        opener = { url -> openWithDesktop(url) }
    }

    /** The default opener: the desktop default browser, or a per-OS command line fallback. */
    private fun openWithDesktop(url: String) {
        if (openThroughDesktop(url)) return
        openThroughCommand(url)
    }

    /** Returns `true` when the desktop API handled the URL. */
    private fun openThroughDesktop(url: String): Boolean = try {
        val usable = Desktop.isDesktopSupported() &&
            Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        if (usable) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            false
        }
    } catch (_: Throwable) {
        // Headless, no desktop integration, or an environment that refuses to browse: fall back.
        false
    }

    /** Uses the platform's own launcher, so a bare JRE without desktop support still works. */
    private fun openThroughCommand(url: String) {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val command = when {
            os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
            os.contains("mac") -> listOf("open", url)
            else -> listOf("xdg-open", url)
        }
        try {
            ProcessBuilder(command).start()
        } catch (_: Throwable) {
            // Nothing left to try; a link that cannot open is not worth crashing the UI over.
        }
    }
}
