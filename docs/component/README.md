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

## TabBar and TabPane

``TabBar`` is a ``FlexBox`` filled with tab buttons. ``TabPane`` holds one ``TabBar`` on one of its four sides and shows the content bound to the selected tab; content bound to every other tab is hidden.

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.LEFT
import com.github.kusa233.ovatoxin.widget.container.TabPane
import com.github.kusa233.ovatoxin.widget.text.TextArea

val pane = TabPane()
pane.side = LEFT

val one = pane.add(Button("One"))
pane.bind(one, TextArea("first page"))

val two = pane.add(Button("Two"))
pane.bind(two, TextArea()) {
    // The block runs in a DrawScope; its parameter is the bound component.
    // A page fills the content area; x and y are ignored.
    it.text = "second page"
}
```

Rules:

* ``side`` is ``UP``, ``BOTTOM``, ``LEFT``, or ``RIGHT``; both ``pane.side`` and ``pane.tab.side`` set it. ``UP`` and ``BOTTOM`` make a horizontal bar, ``LEFT`` and ``RIGHT`` a vertical one;
* ``tab.add(button) { ... }`` appends a button and selects it; ``tab.remove(button)`` removes it. The same calls are available directly on the pane;
* A tab button's ``x`` and ``y`` are ignored — the bar places its buttons. Its ``width``/``height`` are kept;
* ``bind(button, component) { ... }`` attaches content to the tab named by ``button`` and returns the component. The block has the type ``DrawScope<T>.(T) -> Unit``: use ``layout { }``/``style { }`` from the receiver and the component parameter for its own API. The button must already belong to the bar. Several components may be bound to one tab; all of them are visible together;
* A bound component is drawn as soon as it is bound, and its block is replayed on a state-driven redraw. It fills its page by default (``100.percent`` width and height); those percentages resolve against the content area left over after the tab bar, not against the whole pane. Its ``x``/``y`` are ignored, and a width or height written by the caller is kept;
* ``remove(button)`` also removes the components bound to that tab. Removing the selected tab selects the first remaining tab;
* ``barSize`` sets the bar's size across the side it occupies: its height for ``UP``/``BOTTOM`` and its width for ``LEFT``/``RIGHT``. It defaults to ``36.px``, can also be passed to the ``TabPane(barSize = ...)`` constructor, and ``tabBarThickness`` remains as an alias for it;
* Tabs fill the bar across its thickness: on ``UP``/``BOTTOM`` every button is given ``height = 100.percent``, on ``LEFT``/``RIGHT`` every button is given ``width = 100.percent``. The other axis keeps the size the button named. A horizontal bar also centres its tabs by default (``justify = Justify.CENTER``); set ``justify`` yourself to override it;
* The bar scrolls when its tabs outgrow the space it is given: a horizontal bar shows a horizontal scroll bar, a vertical bar shows a vertical one, and both appear only when needed. Scrolling keeps one row or column instead of wrapping, and a selected tab is scrolled into view after a programmatic selection. The bar still fills the whole strip when the tabs fit;
* A style set on the pane is applied to its bar too: the pane copies its background, corner radius, text colour and transition onto ``tab`` on every layout pass, so the strip shares the pane's appearance. A tab button has no fill of its own unless it sets one, so an unselected tab lets the pane's background show through instead of covering it with the control's default white. ``selectColor`` paints the background of the selected tab only; ``null`` (the default) paints no difference. ``pane.selectColor`` and ``pane.tab.selectColor`` are the same property;
* A ``transition`` written in the pane style reaches its tab buttons as well, so the ``selectColor`` switch animates when tabs change. A transition set on an individual tab button takes precedence over the pane's.
