package com.github.kusa233.ovatoxin.component

import javafx.scene.Group
import javafx.scene.Node
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Guards that a lazily built JavaFX node starts in the visibility the component already has. */
class VisibilityTest {

    /** A component with the simplest possible node, so no toolkit is needed to build it. */
    private class Probe : Component<Probe>() {
        override fun buildNode(): Node = Group()
    }

    @Test
    fun `a component hidden before its node is built stays hidden`() {
        val component = Probe()
        component.visible = false

        assertFalse(component.node.isVisible)
        assertFalse(component.visible)
    }

    @Test
    fun `a visible component builds a visible node`() {
        val component = Probe()

        assertTrue(component.node.isVisible)
        assertTrue(component.visible)
    }

    @Test
    fun `visibility changes reach an existing node`() {
        val component = Probe()
        assertTrue(component.node.isVisible)

        component.visible = false
        assertFalse(component.node.isVisible)

        component.visible = true
        assertTrue(component.node.isVisible)
    }
}
