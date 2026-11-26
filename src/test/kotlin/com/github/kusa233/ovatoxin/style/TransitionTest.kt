package com.github.kusa233.ovatoxin.style

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TransitionTest {

    @Test
    fun `a style has no transition until one is set`() {
        assertNull(Style().transition)
    }

    @Test
    fun `the style transition is written in kotlin duration`() {
        val style = Style().configure { transition = 200.milliseconds }

        assertEquals(200.milliseconds, style.transition)
        assertTrue(style.toString().contains("transition=200ms"), style.toString())
    }

    @Test
    fun `any duration unit can be used`() {
        val style = Style().configure { transition = 1.seconds }

        assertEquals(1000.milliseconds, style.transition)
        assertTrue(style.toString().contains("transition=1s"), style.toString())
    }

    @Test
    fun `zero is an instant transition and is allowed`() {
        val style = Style().configure { transition = Duration.ZERO }

        assertEquals(Duration.ZERO, style.transition)
    }

    @Test
    fun `a negative duration is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Style().configure { transition = (-1).milliseconds }
        }
    }

    @Test
    fun `an infinite duration is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Style().configure { transition = Duration.INFINITE }
        }
    }

    @Test
    fun `a reset drops the transition`() {
        val style = Style().configure { transition = 200.milliseconds }
        style.reset()
        assertNull(style.transition)
    }

    @Test
    fun `copyFrom carries the transition`() {
        val source = Style().configure { transition = 80.milliseconds }
        val target = Style()
        target.copyFrom(source)
        assertEquals(80.milliseconds, target.transition)
    }
}
