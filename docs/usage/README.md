# Window

[English](README.md) | [中文](README_zh.md)

To create a window, import the ``Frame`` class:
```kotlin
import com.github.kusa233.ovatoxin.window.Frame
```

Instantiate it and call ``.show()`` to display the window:
```kotlin
val frame = Frame()
frame.show()
```

Use ``.title = "Title"`` to set the window title:
```kotlin
frame.title = "Ovatoxin - demo"
```

## Close subscription (onClose)

Use ``.onClose { }`` to subscribe to the "user requested to close the window" event (clicking the title bar's close button or the platform-equivalent action). Inside the handler, ``this`` is the window, and its title, size, and component tree are still readable, which is useful for saving state or showing a confirmation:

```kotlin
frame.onClose {
    println("Closing '${this.title}'")
}
```

You can subscribe more than once; every handler runs in registration order. Note that ``onClose`` only **observes** a close request and cannot prevent the window from closing; calling ``frame.close()`` directly from code does not trigger it either.

## Title bar (titleBarEnabled)

JavaFX fixes a window's decorations when the window is first shown, so the title bar cannot be toggled afterwards. Pass ``titleBarEnabled`` at construction time to decide whether the title bar is shown:

```kotlin
// A window without a title bar (frameless)
val frame = Frame("Ovatoxin - demo", titleBarEnabled = false)
```

A window without a title bar has no system close button, so you must provide a way to close it yourself (for example, a custom close button).

# Layout and rendering

We will introduce Button first as an example; the API is explained in the following sections.

First, import the Button class:
```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
```

```kotlin
val frame = Frame()
frame.layout {
    draw(Button()) { button ->
        // Set the button's layout
        layout {
            // 10% in from the left
            x = 10.percent
            // 50% down from the top
            y = 50.percent
            // 80% of the window width
            width = 80.percent
            // 10% of the window height
            height = 10.percent
        }
        // Set the button's style
        style {
            background {
                // Set the background colour
                color = Color.web("#4c8bf5")
                // Set a background image; when set, the background colour is ignored
                image = "file:/picture/bg.png"
            }
            // Set the corner radius
            corner = 35.percent
            // Set the foreground colour
            foreground = Color.web("#000000")
            // Set the font size; every em length below resolves against this number
            fontSize = 16.px
        }
    }
}
frame.show()
```

## Template presets (layout / style)

The ``layout { }`` and ``style { }`` written inside a component affect only that component. If a set of positions, sizes, or looks should be reused, use the top-level functions with the same names — ``layout { }`` and ``style { }`` — outside any component to build a **template** first. They return a plain ``Layout``/``Style`` value:

```kotlin
import com.github.kusa233.ovatoxin.dsl.layout
import com.github.kusa233.ovatoxin.dsl.style
```

```kotlin
// A button one fifth as wide as its parent and full height
val fifth = layout {
    width = 20.percent
    height = 100.percent
}

// A blue card
val card = style {
    background { color = Color.web("#4c8bf5") }
    corner = 30.percent
}
```

To apply a template, pass it as the first argument to ``layout(...)``/``style(...)``, optionally followed by a block that makes **per-component adjustments**:

```kotlin
frame.layout {
    draw(FlexBox()) { box ->
        box.layout {
            for (index in 1..4) {
                draw(Button()) { button ->
                    layout(fifth) { /* adjust only this component */ }
                    style(card) { /* adjust only this component */ }
                    button.text = "$index"
                }
            }
        }
    }
}
```

A template is a **snapshot**: applying it copies all of its values into the component (for ``style``, the background is copied too), so:

* Changing the template later does not affect components that already used it;
* Several components sharing one template remain independent — changing one component's background does not change another, and does not change the template itself.

``layout(...)``/``style(...)`` copy **all** slots of the template: slots the template leaves unset take the documented defaults, so applying a template also overwrites values the component already had. Put per-component differences in the block that follows.

## Length units (percent / px / em)

``x``, ``y``, ``width``, ``height``, ``hPadding``, and ``vPadding`` are all lengths and may be written in one of three units:

* ``percent`` — a percentage of the parent container: ``80.percent`` is 80% of the parent's width (``10.p`` is shorthand);
* ``px`` — device-independent pixels: ``16.px``. Use it for things that should not change with the window: hairlines, icons, fixed sidebars;
* ``em`` — one times the component's own font size: ``1.em`` is the height of one line of text.

```kotlin
layout {
    x = 10.percent
    y = 1.em
    width = 16.px
}
```

The advantage of ``em`` is that it follows the font size: when the font grows, padding, spacing, and corner radii written in ``em`` grow with it, while values written in ``px`` stay put. The panel below always has padding one line of text wide and half a line tall, whatever the font size is set to:

```kotlin
draw(Div()) { panel ->
    layout {
        hPadding = 1.em
        vPadding = 0.5.em
    }
    // When the font size changes, the em amounts above change with it
    style { fontSize = 20.px }
}
```

The three units do not mix: only values in the same unit can be added, scaled, or compared. ``10.percent + 4.px`` does not compile, because only the parent's size and the font size can decide how the two add up. If you really need to mix them, first freeze the relative side into pixels with ``10.percent.toPixels(width)``.

The font size ``fontSize`` is also a length, but only ``px`` and ``percent`` make sense; ``em`` is itself measured from the font size and cannot also be used as the font size — writing it raises an error. See the "Font size" section below for details.

## Font size (fontSize)

``em`` is relative to **the component's own font size**. A component can write a ``fontSize`` in its ``style`` block, and the whole subtree beneath it inherits that size:

```kotlin
draw(Div()) { panel ->
    style { fontSize = 20.px }
    panel.layout {
        // Everything in here resolves em against 20px
        draw(Button()) { button ->
            style { fontSize = 16.px }   // This branch switches to 16px and no longer follows the outer 20px
            layout {
                width = 6.em
                height = 2.em
            }
        }
    }
}
```

``fontSize`` is a length, but only two units are meaningful:

* ``px`` — device-independent pixels, an absolute size: ``fontSize = 16.px``;
* ``percent`` — a fraction of **the component's own box height**: a button a few dozen pixels tall with ``fontSize = 50.percent`` gets text half its height, so the text scales with the box;
* ``em`` — **not allowed**. ``em`` literally means "a multiple of the font size"; using it as the font size would measure the font against itself, so ``fontSize = 1.em`` throws ``IllegalArgumentException``.

```kotlin
style { fontSize = 20.px }        // Twenty pixels
style { fontSize = 50.percent }   // 50% of the component's own height
style { fontSize = 1.em }         // Error: em has no font size to refer to
```

``percent`` needs the box height, which is not known before layout; more importantly, a component whose height is ``auto`` or ``em`` grows with the font, so the font size and the box chase each other. A percentage therefore **only takes effect when the component's height is ``px`` or ``percent``**; when the height is ``auto`` or ``em`` it falls back to the inherited size. To make a percentage font size meaningful, give the component an explicit ``height``:

```kotlin
draw(Button()) { button ->
    layout { height = 40.px }
    style { fontSize = 50.percent }   // A 40px-tall button gets 20px text
    button.text = "Ok"
}
```

Rules:

* Write ``fontSize`` in ``px`` or ``percent``;
* If omitted, the component inherits its parent's font size, walking up; if no component in the tree sets one, the library default ``14.0`` applies;
* ``percent`` is taken of **the component's own box height**, so ``50.percent`` means "text half the box height"; a taller box means bigger text, independently of surrounding text;
* The computed size is used not only for resolving ``em`` but is actually set on control text (``-fx-font-size`` on buttons, input fields, labels, and so on), so the text on screen really does scale with the box;
* Components that draw their own text (``TextArea``) use their own text size and do not look upward; their size is also stored in ``style``, so ``style { fontSize = 24.px }`` also changes the text they render;
* Changing ``fontSize`` lays out the subtree again, and existing ``em`` values immediately resolve against the new size;
* A redraw (``reset()``) drops a ``fontSize`` that was not set again in that pass, returning to inheritance.

## Font weight (fontWeight)

A ``style`` block can write ``fontWeight``, using JavaFX's ``FontWeight`` directly:

```kotlin
import javafx.scene.text.FontWeight
```

```kotlin
draw(Button()) { button ->
    style {
        fontWeight = FontWeight.BOLD
    }
    button.text = "Save"
}
```

``fontWeight`` accepts the values of the ``FontWeight`` enum: ``THIN``, ``EXTRA_LIGHT``, ``LIGHT``, ``NORMAL``, ``MEDIUM``, ``SEMI_BOLD``, ``BOLD``, ``EXTRA_BOLD``, and ``BLACK``.

Rules:

* If ``fontWeight`` is omitted (the default ``null``), the control keeps its original weight and native appearance;
* Buttons, input fields, labels, links, and other controls write the weight into ``-fx-font-weight``, using the numeric value of the ``FontWeight`` (for example, ``BOLD`` is ``700``);
* ``TextArea``, which draws its own text, uses this weight when it builds its ``Font``; its existing ``bold`` property still works, but an explicit ``fontWeight`` wins;
* Whether a weight actually renders differently depends on whether the chosen font family provides that weight; the system default font may fall back from something like ``SEMI_BOLD`` to regular, so a ``fontFamily`` with multiple weights is needed to see the difference;
* Like ``foreground``, ``Div`` and ``Frame``, which have no text of their own, ignore it; font weight is not inherited by the subtree the way ``fontSize`` is;
* ``fontWeight`` lives in the style and is cleared by ``reset()`` on a redraw; write it again in every ``style`` block run where you want it to apply.

Font weight (like font size) is not interpolated by ``transition``: transitions only smooth background colour, foreground colour, and corner radius, so weight switches immediately when ``fontWeight`` changes.

## Animation transitions (transition)

A ``style`` block can set a ``transition`` to make style **changes** smooth instead of jumping:

```kotlin
val selected = State(true)
draw(Button()) { button ->
    style {
        background { color = if (selected.value) Color.WHITE else Color.web("#4c8bf5") }
        foreground = if (selected.value) Color.web("#4c8bf5") else Color.WHITE
        transition = 200.milliseconds
    }
    button.onClick { selected.update(!selected.value) }
}.withState(selected)
```

The unit of ``transition`` is the Kotlin standard library's ``Duration``, so write ``200.milliseconds`` directly; other units such as ``1.seconds`` or ``500.microseconds`` are also supported, following the notation of ``kotlin.time.Duration``.

Rules:

* The **first** style a component receives is applied immediately; ``transition`` is responsible only for **changes** after that;
* The interpolated values are ``background`` (colour and ``opacity`` together), ``foreground``, and ``corner``, so colours, transparency, and corner radius all animate frame by frame;
* When ``background`` has an ``image``, the image is not interpolated; the image switches immediately while the colour layer fades in and out as usual;
* ``Duration.ZERO`` behaves like the default ``transition = null``: instant, no animation; a negative duration or ``Duration.INFINITE`` raises an error;
* ``transition`` lives in the style and, like other style values, is cleared by ``reset()`` on a redraw; to keep animating after a redraw, write it again in the ``style`` block that runs each time;
* Animation needs the JavaFX thread; once the component is displayed, the layout process drives it automatically, with no manual call needed.

## Padding (padding)

Padding is the pair of values in a ``layout`` block: ``hPadding`` (left and right) and ``vPadding`` (top and bottom). It is written in the same place as ``x`` and ``width``, namely the component's own ``layout`` block:

```kotlin
val frame = Frame()
frame.layout {
    draw(Div()) { panel ->
        layout {
            width = 80.percent
            height = 50.percent
            hPadding = 24.px
            vPadding = 16.px
        }
        panel.layout {
            draw(Button()) { button ->
                layout {
                    // Content is inset: x = 0 lands on the left padding, not at the panel's left edge
                    x = 0.px
                    y = 0.px
                    // 100% is the width left after insetting, so the right edge does not overflow
                    width = 100.percent
                    height = 20.percent
                }
            }
        }
    }
}
```

Padding is **content inset**:

* The left and right edges move in by ``hPadding``, and the top and bottom by ``vPadding``, shrinking the available area accordingly;
* A child's ``x``/``y`` are measured from the top-left of the inset box;
* Percentages resolve against the inset box, so ``100.percent`` stops exactly at the padding;
* Windows, ``Div``, and ``FlexBox`` all support it; padding affects only the container's own children, never anything outside;
* A leaf component with no children (``Button``, ``TextField``, and so on) applies padding to itself: the left and right are inset by ``hPadding``; vertically it **first tries to move** — the box keeps its own height and is centred within the band the flow gives it, so the top and bottom whitespace are equal — and only when "height + 2×``vPadding``" exceeds that band is the box compressed, leaving ``vPadding`` of whitespace above and below. For example, this makes a button show equal strips of background above and below inside a ``FlexBox``;
* Padding only insets/offsets: it does not push children apart; ``FlexBox`` spacing is controlled by ``gap`` (see ``gap``);
* When padding exceeds half the component's own width (or height), it is clamped to half, so the content area becomes zero rather than negative;
* On a container, padding only decides how children inside that container are placed; the container's own position and size are unchanged. On a leaf component, it insets the leaf's own box left and right, and vertically centres first and compresses only when there is not enough room, as described above.

A window is not a child of anything, so its padding is written directly on the window:

```kotlin
frame.applyLayout {
    hPadding = 24.px
    vPadding = 16.px
}
```
