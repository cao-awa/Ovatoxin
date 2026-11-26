# Components

[English](README.md) | [中文](README_zh.md)

## Label

``Label`` is a form label: a short piece of text that names the control next to it. It carries no content of its own; the content belongs to the item it labels.

What makes it special is not the text but **its association with a control**: wherever ``labelFor`` points, clicking the text moves focus there, and screen readers announce the two together. So the input field beside ``Label("User name", labelFor = nameField)`` no longer needs its caption written into its placeholder.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.input.TextField
import com.github.kusa233.ovatoxin.widget.text.Label
```

```kotlin
frame.layout {
    // Draw the control first so the label can refer to it
    val name = draw(TextField()) { field ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 45.percent
            height = 6.percent
        }
        field.hint = "Your name"
    }
    // This label labels the input field above
    draw(Label("User name", labelFor = name)) { label ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 45.percent
            height = 6.percent
        }
        // A label's text uses the foreground colour, like every other text component
        style {
            foreground = Color.web("#4c8bf5")
        }
    }
}
```

Rules:

* A label is responsible only for a short piece of text; it lays nothing out, and its position and size still come from ``layout``;
* ``labelFor`` can be written in the constructor or attached later: ``label.labelFor = name`` and ``label.labelFor(name)`` are equivalent, and ``label.labelFor`` reads back the item currently being labelled;
* Order does not matter: an association made before a control is rendered is remembered and applied when the label creates its node, so a label may be drawn before its control;
* Passing ``null`` to ``labelFor`` breaks the association and turns the label back into ordinary text;
* ``text`` can be changed at any time: ``label.text = "Account"``, and an already-created node updates immediately;
* ``reset()`` returns to the state the label was created in — both the text and the association go back to the pair given in the constructor rather than being cleared to empty text;
* For a longer read-only piece of text that carries its own content (a paragraph or a translation), use ``TextArea``; a label is only the short name of the item beside it.

## FlexBox

``FlexBox`` is a canvas that lays out its children by itself: it fills from left to right and wraps to the next line when a line is full, with each line as tall as its tallest child.
``FlexBox`` can also be ``draw``n, and it can be placed inside a ``Div``.

It has three properties of its own, written directly on the ``FlexBox`` rather than in a ``layout`` block:

* ``gap`` — the spacing between children, which is also the spacing between lines; defaults to ``0.px``;
* ``justify`` — how leftover space on a line is distributed after the line is filled; defaults to ``Justify.START``;
* ``align`` — whether children sit at the top, centre, or bottom of their line; defaults to ``Align.START``.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Align
import com.github.kusa233.ovatoxin.widget.container.FlexBox
import com.github.kusa233.ovatoxin.widget.container.Justify
```

```kotlin
frame.layout {
    draw(FlexBox()) { row ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 80.percent
            height = 40.percent
            // Padding only insets the content; it does not change spacing between children
            hPadding = 8.px
            vPadding = 8.px
        }
        // Spacing and distribution live on the FlexBox itself
        row.gap = 8.px
        row.justify = Justify.SPACE_BETWEEN
        row.align = Align.CENTER
        row.layout {
            for (index in 1..12) {
                draw(Button()) { cell ->
                    layout {
                        // Give only a size; leave positioning to the FlexBox
                        width = 18.percent
                        height = 30.px
                    }
                    cell.text = "$index"
                }
            }
        }
    }
}
```

Rules:

* Without ``x``/``y``, positions come from the flow;
* An explicit ``x``/``y`` is treated only as an offset added to the position the ``FlexBox`` computed;
* ``hPadding``/``vPadding`` only inset: the first line starts at the padding, and ``justify`` never distributes space beyond the padding;
* ``gap`` is the only value that controls spacing, and it is used both horizontally and vertically; as a percentage it resolves against the content width horizontally and the content height vertically;
* ``gap`` and ``hPadding``/``vPadding`` may be written in ``em``: they then resolve against the **FlexBox's own** font size, so after the ``row``'s ``style`` sets ``fontSize = 20.px``, ``row.gap = 1.em`` is 20 pixels — change the panel's font size and the spacing follows;
* ``justify`` only has an effect when there is free space, and it never changes a child's size:
  ``START`` aligns left, ``CENTER`` centres, ``END`` aligns right, ``SPACE_BETWEEN`` pushes the first and last children to the edges, and ``SPACE_AROUND`` leaves half a share on each side;
* The free space produced by ``justify`` is **added on top of** ``gap``, so the actual spacing under ``SPACE_BETWEEN`` is ``gap`` plus that share;
  On a line with only one child, ``SPACE_BETWEEN`` behaves like ``START`` and ``SPACE_AROUND`` behaves like centring;
* If a line cannot fit its children they overflow; there is then no free space to distribute, and the whole line falls back to ``START`` — children are never squeezed narrower;
* ``align`` compares children within one line only: the line is still as tall as its tallest child, ``CENTER`` gives each child half the remaining height above and below, and ``END`` sits against the bottom of the line;
  Lines still stack downward one after another, independently of ``align``;
* Percentage widths resolve against the inset width, so a child at ``100.percent`` takes a whole line by itself;
* When a child's ``width``/``height`` is written in ``em``, it resolves against **the child's own** font size (usually the inherited one), because it measures the child's own size, not the ``FlexBox``'s.
