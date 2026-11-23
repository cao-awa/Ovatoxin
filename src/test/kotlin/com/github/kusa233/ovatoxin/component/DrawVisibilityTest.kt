package com.github.kusa233.ovatoxin.component

import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards *where* `draw` lives.
 *
 * The restriction "only a `Frame` and a `Div` can draw, and only inside their own `layout` block"
 * is expressed in the type system, not in a runtime check: `draw` is a member of [SurfaceScope],
 * which only [ComponentDrawable.layout] hands over. That makes a wrong call a compile error, but
 * it also makes the rule invisible to a behavioural test — a `draw` added back to [Container] for
 * convenience would compile, pass every existing test and quietly reopen the hole. So the shape is
 * asserted here by reflection, which is the closest a test can get to the compile errors that
 * document the rule: `frame.draw(...)`, `button.draw(...)` and a bare `draw(...)` are rejected by
 * the compiler, and this test pins the declaration that makes them so.
 */
class DrawVisibilityTest {

    @Test
    fun `only the surface scope can draw`() {
        val drawing = listOf(Component::class.java, Container::class.java, ComponentDrawable::class.java)
            .filter { type -> type.declaredMethods.any { it.name == "draw" } }
            .map { it.simpleName }

        assertTrue(
            drawing.isEmpty(),
            "draw must be declared by SurfaceScope alone, but ${drawing.joinToString()} declare it: " +
                "on a component or a container it is reachable outside a layout block again",
        )
        assertNotNull(
            SurfaceScope::class.java.declaredMethods.firstOrNull { it.name == "draw" },
            "SurfaceScope is where draw belongs",
        )
    }

    @Test
    fun `a surface only draws through its layout block`() {
        // The block's receiver is the scope, so `this` inside it is not the surface: that is what
        // keeps draw out of every other position.
        val layout = ComponentDrawable::class.java.declaredMethods
            .firstOrNull { it.name == "layout" && it.parameterCount == 1 }
        assertNotNull(layout, "a surface must offer a layout block")
        // A receiver lambda erases to Function1, so the receiver is the first type argument of the
        // generic signature, as in `Function1<SurfaceScope<T>, Unit>`. Kotlin writes the receiver
        // contravariantly, so it arrives wrapped in a `? super ...` wildcard that has to be opened.
        val block = layout.genericParameterTypes.single() as ParameterizedType
        val receiver = block.actualTypeArguments.first()
        val receiverClass = when (receiver) {
            is WildcardType -> receiver.lowerBounds.single() as ParameterizedType
            is ParameterizedType -> receiver
            else -> error("unexpected receiver type $receiver")
        }.rawType
        assertEquals(
            SurfaceScope::class.java,
            receiverClass,
            "layout must hand over a SurfaceScope, not the surface itself",
        )

        // A container that is not a surface cannot declare children at all.
        assertNull(
            Container::class.java.declaredMethods.firstOrNull { it.name == "layout" },
            "a plain container must not open a layout block",
        )
    }

    @Test
    fun `the two surfaces are the only drawables`() {
        val surfaces = listOf(Frame::class.java, Div::class.java)
        surfaces.forEach { surface ->
            assertTrue(
                ComponentDrawable::class.java.isAssignableFrom(surface),
                "${surface.simpleName} is a drawing surface",
            )
        }
        // Everything else is drawn, never drawing.
        listOf(Button::class.java, TextArea::class.java).forEach { plain ->
            assertTrue(
                !ComponentDrawable::class.java.isAssignableFrom(plain),
                "${plain.simpleName} must not be a drawing surface",
            )
        }
    }
}
