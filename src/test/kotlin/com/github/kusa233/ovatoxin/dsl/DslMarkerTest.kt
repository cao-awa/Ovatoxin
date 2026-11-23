package com.github.kusa233.ovatoxin.dsl

import com.github.kusa233.ovatoxin.component.ComponentDrawable
import com.github.kusa233.ovatoxin.component.Container
import com.github.kusa233.ovatoxin.component.DrawScope
import com.github.kusa233.ovatoxin.component.SurfaceScope
import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.style.Background
import com.github.kusa233.ovatoxin.style.Style
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the [OvatoxinDsl] markers on the DSL receiver types.
 *
 * The marker is what keeps a block from reaching the receiver around it, and it is invisible in
 * normal use: dropping one annotation makes the DSL accept a scope leak again without failing a
 * single behavioural test. [DslMarker] has `BINARY` retention, so reflection cannot see it — the
 * annotation is read out of the compiled class file instead.
 */
class DslMarkerTest {

    private val receivers = listOf(
        "layout block" to Layout::class,
        "style block" to Style::class,
        "background block" to Background::class,
        "draw block" to DrawScope::class,
        "surface block" to SurfaceScope::class,
        "container block" to Container::class,
        "drawable block" to ComponentDrawable::class,
    )

    @Test
    fun `every dsl receiver is marked`() {
        this.receivers.forEach { (name, type) ->
            assertTrue(
                markedWithOvatoxinDsl(type.java),
                "the $name (${type.simpleName}) must be annotated with @OvatoxinDsl, " +
                    "otherwise a nested block can silently write into the outer scope",
            )
        }
    }

    @Test
    fun `the marker is a dsl marker`() {
        assertTrue(
            markedWith(kotlin.DslMarker::class.java, this.annotationBytes(OvatoxinDsl::class.java)),
            "@OvatoxinDsl must itself be annotated with kotlin.DslMarker to have any effect",
        )
    }

    private fun markedWithOvatoxinDsl(type: Class<*>): Boolean =
        markedWith(OvatoxinDsl::class.java, this.annotationBytes(type))

    /** The raw class file of [type], read through the class loader that loaded it. */
    private fun annotationBytes(type: Class<*>): String {
        val resource = type.name.replace('.', '/') + ".class"
        val bytes = type.classLoader.getResourceAsStream(resource)?.use { it.readBytes() }
        requireNotNull(bytes) { "cannot read the class file of $resource" }
        return String(bytes, Charsets.ISO_8859_1)
    }

    /**
     * Whether a binary annotation of [annotation] is referenced by the class file of [bytes].
     *
     * A class level binary annotation is stored in the constant pool by its descriptor, so the
     * descriptor appearing at all is the evidence that the annotation was applied.
     */
    private fun markedWith(annotation: Class<*>, bytes: String): Boolean {
        val descriptor = "L" + annotation.name.replace('.', '/') + ";"
        return bytes.contains(descriptor)
    }
}
