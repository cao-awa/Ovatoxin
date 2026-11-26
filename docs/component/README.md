# Components

[English](README.md) | [中文](README_zh.md)

## Contents

* Text and display: [Label](#label), [TextArea](#textarea), [Link](#link)
* Actions and input: [Button](#button), [TextField](#textfield), [CheckBox](#checkbox), [ComboBox](#combobox)
* Collections and progress: [ListView](#listview), [ProgressBar](#progressbar)
* Feedback and overlays: [Toast](#toast)
* Layout: [Div](#div), [FlexBox](#flexbox), [Card](#card), [TabBar and TabPane](#tabbar-and-tabpane)

## Common component behaviour

Every component in this chapter is a ``Component``, so the following rules apply everywhere and are not repeated in each section.

* ``layout { ... }`` describes the geometry of the component itself (``x``, ``y``, ``width``, ``height``, padding and so on); ``style { ... }`` describes its fill, corner, foreground, font and transition. Both are documented in [Usage](../usage/README.md).
* ``onClick { ... }`` registers a click handler and replaces any handler registered before it. Inside the block ``this`` is the component, so ``button.onClick { println(this.text) }`` reads the component's own properties.
* ``visible = false`` hides an already-built component; it defaults to ``true``.
* ``withState(state)`` binds the component to a [State](../usage/README.md): the component is redrawn when the state changes, and only that component is redrawn.
* Immediately before a draw block runs again, ``reset()`` restores the values the component was **created** with — caption, URL, checked state, geometry, style and handlers that were set during the previous pass. It does not reset the component's identity, its node or its children, and it deliberately leaves live collections such as ``ListView.items`` and ``ComboBox.items`` alone.
* A component with no style of its own starts with a white fill (``Style.DEFAULT_COLOR``) and leaves text at the toolkit's default dark colour until ``style { foreground = ... }`` sets one; [Div](#div) is transparent by design, the [Toast](#toast) overlay is transparent too, and [Link](#link) is blue by default.

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

## TextArea

``TextArea`` is read-only text that carries its own content: a paragraph, a message or a translation. It is not an editor — for an editable single-line input use [TextField](#textfield).

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.text.TextAlignment
```

```kotlin
frame.layout {
    draw(TextArea("A paragraph that belongs to the page itself.")) { text ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 80.percent
            height = 30.percent
        }
        text.fontSize = 16.0
        text.bold = true
        text.align = TextAlignment.CENTER
        text.wrap = true
        style {
            foreground = Color.web("#333333")
        }
    }
}
```

Rules:

* ``TextArea("hello")`` starts with that text; ``text`` reads or replaces it, and ``text.text("new")`` is the same as ``text.text = "new"``;
* ``fontSize`` is measured in points and defaults to ``14``. It is stored in the component's style, so ``text.fontSize = 20.0`` and ``style { fontSize = 20.px }`` mean the same thing; a percentage fontSize is taken from the area's own height, and an ``em`` fontSize is rejected because it would measure the font against itself;
* unlike most other components a text area **does not inherit** the font size of the panel it is drawn into: an area without a size of its own uses ``14`` points. Give it a height and a percentage size if it should scale with its box;
* ``bold`` selects a bold weight and ``fontFamily`` selects the family; a ``fontWeight`` written in the style overrides ``bold``;
* ``align`` is a JavaFX ``TextAlignment`` (``LEFT``, ``CENTER``, ``RIGHT`` or ``JUSTIFY``) and defaults to ``LEFT``; ``wrap`` defaults to ``false``, so long lines run past the box unless it is enabled;
* the text colour comes from ``style { foreground = ... }`` like every other text component;
* ``reset()`` restores the constructor text, ``14`` points, the default family, ``LEFT`` alignment, ``wrap = false`` and drops the style/handlers set in the previous pass. ``TextArea("hello")`` therefore resets to ``"hello"``, not to empty text.

When the text should sit directly on the surrounding surface, use the top-level ``bareTextArea`` helper:

```kotlin
import com.github.kusa233.ovatoxin.widget.text.bareTextArea

frame.layout {
    bareTextArea("A caption") { area ->
        layout { height = 24.px }
        style { foreground = Color.web("#162033") }
        area.bold = true
    }
}
```

It only makes the ``TextArea``'s own box transparent and then runs the ``config`` block; the text, size, alignment and so on still come from the area's properties and style.

## Link

``Link`` is a caption that behaves like a hyperlink: it is underlined, shows a hand cursor and opens a URL in the system browser when clicked.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.text.Link
```

```kotlin
frame.layout {
    draw(Link("Ovatoxin", "https://github.com/cao-awa/Ovatoxin")) { link ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 40.percent
            height = 6.percent
        }
    }
}
```

Rules:

* ``text`` is the visible caption and ``url`` is the address that a click opens; both can be changed at any time;
* clicking a link with no ``onClick`` handler opens ``url`` in the operating system's default browser. An empty ``url`` makes the click do nothing;
* registering ``onClick`` **replaces** the default browser action: a link with a handler runs only the handler and opens nothing. The handler receives the link as ``this``, so ``this.url`` remains readable inside it;
* ``open()`` performs the browser action explicitly, which is useful when a custom handler wants to keep it;
* the caption is underlined and uses a hand cursor. Its default foreground is ``#4c8bf5``, and ``style { foreground = ... }`` overrides it;
* ``reset()`` restores the constructor caption, the constructor URL and the default blue colour.

## Button

``Button`` is a push button with a caption that runs a handler when it is pressed.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
```

```kotlin
frame.layout {
    draw(Button("Save")) { button ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 25.percent
            height = 8.percent
        }
        button.onClick {
            println("saving ${this.text}")
        }
        button.enabled = true
    }
}
```

Rules:

* ``Button("Save")`` starts with that caption; ``text`` reads or replaces it, and the accessor form ``button.text("Save")`` is equivalent to the property;
* ``enabled`` defaults to ``true``. A disabled button ignores clicks and does not run its handler;
* ``onClick { ... }`` is the only click handler; registering it again replaces the previous one, and inside the block ``this`` is the button. The generic handler is shared with every other component;
* the visual style is mostly flat: the style layer removes JavaFX's native border, gradient and focus ring, then paints the configured background, optional border and foreground. A button with no style is a white rectangle with dark text;
* ``reset()`` restores the constructor caption, re-enables the button and removes the click handler set by the previous draw pass. ``Button("Save")`` therefore resets to ``"Save"``.

## TextField

``TextField`` is an editable, single-line text box.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.input.TextField
```

```kotlin
frame.layout {
    draw(TextField()) { input ->
        layout {
            x = 20.percent
            y = 20.percent
            width = 60.percent
            height = 8.percent
        }
        input.hint = "Type your name"
        input.onChange { value -> println("now: $value") }
        input.onSubmit { value -> println("submitted: $value") }
    }
}
```

Rules:

* ``TextField("initial")`` starts with that content; ``text`` reads or replaces it, and ``input.text("new")`` is equivalent to the property;
* ``hint`` is the placeholder shown while the box is empty and defaults to an empty string; ``editable`` defaults to ``true`` and ``editable = false`` makes the box read-only;
* ``onChange { ... }`` receives the new content whenever the user edits the box, **and also** when the program writes ``text`` — a programmatic write is reported like an edit. Registering it again replaces the previous handler;
* ``onSubmit { ... }`` runs when the user presses ``Enter``; ``performSubmit()`` invokes the same handler manually, which is mainly useful in tests;
* the native JavaFX text field supplies selection, IME and clipboard behaviour; the style layer supplies the fill and text colour;
* ``reset()`` restores the constructor content, clears the hint, makes the field editable again and drops both handlers. Resetting assigns the content directly, so the dropped ``onChange`` handler is not called for it.

## CheckBox

``CheckBox`` is a box with a caption on its right that is either ticked or not.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.input.CheckBox
```

```kotlin
frame.layout {
    draw(CheckBox("Remember me")) { box ->
        layout {
            x = 10.percent
            y = 70.percent
            width = 80.percent
            height = 8.percent
        }
        box.checked = true
        box.onChange { checked -> println("checked: $checked") }
    }
}
```

Rules:

* ``checked`` starts as ``false`` and can be read or written at any time; the constructor only supplies the caption, so a tick must be set through the property;
* ``onChange { checked -> ... }`` receives the new boolean state and is the handler meant for a checkbox. ``onClick { ... }`` also works and runs on the same click in addition to ``onChange`` when both are registered;
* a tick written by the program is **silent**: ``box.checked = true`` changes the screen but runs no handler. Only a user click reaches the handlers, which keeps a handler that writes ``checked`` from calling itself;
* ``enabled`` defaults to ``true``; a disabled box reports nothing;
* ``reset()`` restores the constructor caption and its unticked state, enables the box and removes the handler. The JavaFX skin is flattened by the component stylesheet;
* unlike a raw JavaFX checkbox, the OvatoXin checkbox keeps one handler per kind and replaces it when registered again.

## ComboBox

``ComboBox<T>`` shows one item in a field and drops down the rest of a list when clicked.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.input.ComboBox
```

```kotlin
frame.layout {
    draw(ComboBox(listOf("Small", "Medium", "Large"))) { combo ->
        layout {
            x = 10.percent
            y = 40.percent
            width = 40.percent
            height = 8.percent
        }
        combo.selectedIndex = 1
        combo.onSelectItem { size -> println("size: $size") }
    }
}
```

For an item type that has no useful ``toString``, pass a formatter as the second constructor argument:

```kotlin
data class Language(val code: String, val name: String)

draw(ComboBox(languages) { it.name }) { combo ->
    combo.prompt = "Choose a language"
}
```

Rules:

* ``items`` is a **live** ``MutableList``: ``combo.items.add(...)``, ``removeAt``, ``clear`` and every other ``MutableList`` operation update the dropdown immediately. ``combo.items(list)`` replaces every item at once;
* the selection follows the item it points at through mutations: an item inserted above the selection shifts the selection down, and losing the selected item drops the selection. Replacing the items with ``items(value)`` keeps the selection only if the same item is still present;
* ``itemText`` decides both the text in the field and the text of every dropdown row; the default formatter is ``toString()``;
* ``selectedIndex`` is the selected index, or ``ComboBox.NO_SELECTION`` (``-1``) when nothing is selected; a new combo box starts unselected. ``selectedItem`` and ``selectedText`` read the selected item and its formatted text; ``size`` and ``item(index)`` inspect the items;
* ``select(index)`` is the programmatic path and stays silent; ``clearSelection()`` drops the selection. An index outside the items other than ``NO_SELECTION`` throws. ``onSelect { index -> ... }`` and ``onSelectItem { item -> ... }`` run only for a pick the user makes, and both may be registered;
* ``prompt`` is shown while nothing is selected and defaults to empty, and ``enabled`` defaults to ``true`` and controls whether the dropdown can be opened;
* a combo box starts with a white fill and black text, regardless of the JavaFX default; ``reset()`` clears the selection and the prompt, re-enables it, drops the handlers and reseeds those colours, but it **keeps the items**. Use ``combo.items(combo.initialItems)`` to restore the constructor items.

## ListView

``ListView<T>`` is a vertical, selectable list of rows.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.list.ListView
```

```kotlin
frame.layout {
    draw(ListView(listOf("Alpha", "Beta", "Gamma"))) { list ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 40.percent
            height = 60.percent
        }
        list.onSelectItem { item -> println("picked: $item") }
    }
}
```

A formatter renders item types that do not have a useful ``toString``:

```kotlin
data class User(val name: String)

draw(ListView(users) { it.name }) { list ->
    list.selectedBackground = Color.web("#dbeafe")
}
```

Rules:

* ``items`` is a live ``MutableList``. ``add``, ``addAll``, ``remove``, ``removeAt`` and ``clear`` are also available directly on the list and are shorthands for the same operations; every change updates the rows before the call returns;
* the selection follows the selected **item** through mutations: rows inserted above it shift it, and removing the selected row drops it. ``list.items(otherList)`` replaces all rows at once and keeps the selection only if the same item is still present;
* ``selectedIndex`` is the selected row or ``ListView.NO_SELECTION`` (``-1``) and a new list starts with no selection; ``selectedItem`` and ``selectedText`` read it; ``select(index)`` and ``clearSelection()`` are the programmatic path and run no handler;
* ``onSelect { index -> ... }`` and ``onSelectItem { item -> ... }`` run only when the user clicks a row (primary button); both may be registered. A user click selects the row before either handler runs;
* ``selectedBackground`` defaults to ``#eeeeee``. The list itself starts with a white fill and black text, and its text colour also paints every row;
* rows are laid out in a scroll pane with a vertical scroll bar that appears only when the rows overflow. Rows always span the full width of the list, so the selected background reads as a full-width bar;
* ``reset()`` drops the selection, the ``selectedBackground`` override and both handlers, and reseeds the default colours, but it **keeps the current rows**. Use ``list.items(list.initialItems)`` to return to the constructor rows;
* because a draw block is re-run on every redraw, an unconditional ``list.items.add(...)`` inside it appends again on each run. Guard such an operation on the state value that describes the change;
* ``size`` and ``item(index)`` inspect the rows, and ``removeAt``/``item`` throw when the index is outside the list.

## ProgressBar

``ProgressBar`` is a horizontal indicator that fills up to show how much of a task is done.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.progress.ProgressBar
```

```kotlin
val progress = State(40.percent)

frame.layout {
    draw(ProgressBar(progress.value)) { bar ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 80.percent
            height = 6.px
        }
        style {
            corner = 3.px
            background {
                color = Color.web("#dbeafe")
            }
            foreground = Color.web("#4c8bf5")
        }
        bar.progress = progress.value
    }.withState(progress)
}
```

Rules:

* ``ProgressBar(0.4)`` takes a fraction from ``0.0`` to ``1.0``; ``ProgressBar(40.percent)`` takes the same fullness as a percentage. ``ProgressBar()`` starts empty;
* ``progress()`` reads the fraction; ``progress(0.4)`` and ``progress(40.percent)`` set it; the ``progress`` property speaks in ``Percent``, so ``bar.progress = 40.percent`` is the property form and ``bar.progress >= 100.percent`` is how a loop tests completion. A pixel is not a fraction and never compiles as progress;
* values outside the range are clamped rather than rejected, which is useful when a loop over-reports: ``1.2`` shows a full bar and ``-1.0`` an empty one. ``NaN`` is treated as empty;
* there is no percentage label and no built-in animation. Drive the bar from a [State](../usage/README.md) as in the example, or from a timer that updates the state; the bar itself only displays the value it is given;
* a non-zero ``corner`` rounds the track and clips the filled band to the same outline. The style's ``background`` paints the empty track and its ``foreground`` paints the filled band, so set both to make the indicator readable;
* the filled band is drawn by the component stylesheet in a single flat colour, using ``style { foreground = ... }``; when no foreground is set it uses the widget's default blue (``#0096c9``). The ``background`` of the style paints the empty part of the track, not the filled part;
* ``reset()`` restores the progress the bar was created with.

To avoid repeating the ``layout`` and ``style`` blocks, use the top-level ``progressBar`` helper:

```kotlin
import com.github.kusa233.ovatoxin.widget.progress.progressBar

frame.layout {
    progressBar(
        h = 14.px,
        value = 40.percent,
        tone = Color.web("#10b981"),
        track = Color.web("#e8edf7"),
    )
}
```

A ``track`` of ``null`` (the default) leaves the background untouched, so the bar inherits the surface it is drawn on; ``radius`` defaults to ``50.percent``.

## Div

``Div`` is a plain box that frames a region of the window. It is a drawing surface, not a widget: it has no fill of its own unless a style gives it one, but it **rescopes percentages** for its content and **clips** anything that leaves its bounds.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.widget.text.TextArea
```

```kotlin
frame.layout {
    draw(Div()) { panel ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 80.percent
            height = 50.percent
        }
        panel.layout {
            // 50% is half of the panel, not half of the window.
            draw(TextArea("inside the panel")) { text ->
                layout {
                    x = 50.percent
                    width = 50.percent
                    height = 100.percent
                }
            }
        }
    }
}
```

Rules:

* the outer ``layout { ... }`` passed to ``draw`` configures the panel's own geometry; the inner content is reached through ``panel.layout { ... }``, exactly as with any drawing surface;
* every child inside resolves ``x``/``width`` against the panel's content box, so moving or resizing the panel moves its whole content with it;
* ``clip`` defaults to ``true``. Content outside the panel is not drawn; set ``clip = false`` to let it spill out. A non-zero ``corner`` also rounds the clipping outline;
* a ``Div`` starts fully transparent. Give it ``style { background { color = ... } }`` when it should paint a card or panel;
* it places its children at absolute positions. To flow children instead, draw a [FlexBox](#flexbox) into the panel and let it arrange them;
* a ``Div`` can contain another ``Div`` or any other component, and its percentages nest naturally;
* ``reset()`` restores the two defaults of a bare area: clipping on and a transparent fill.

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

The top-level ``flexFlow`` helper draws a flow row with a fixed row height, padding and spacing in one call:

```kotlin
import com.github.kusa233.ovatoxin.widget.container.flexFlow

frame.layout {
    flexFlow(
        rowHeight = 132.px,
        justify = Justify.START,
        align = Align.CENTER,
        gap = 10.0,
        fill = Color.web("#f8fafc"),
        borderColor = Color.web("#e4e9f2"),
    ) {
        repeat(4) { index ->
            draw(Button("Item ${index + 1}")) {
                layout {
                    width = 110.px
                    height = 42.px
                }
            }
        }
    }
}
```

A ``fill`` or ``borderColor`` of ``null`` (the default) leaves that part undrawn; ``pad`` defaults to ``12.0`` and ``radius`` to ``12.px``.

## Card

``Card`` is a rounded panel with padding and a vertical gap, used to group related content. It is a [FlexBox](#flexbox), so ``gap``, ``justify``, ``align`` and every FlexBox layout rule still apply; on top of that it seeds the look a card usually needs and offers two member methods, ``text`` and ``button``:

* ``fill`` — the background colour, white by default;
* ``borderColor`` — the one-pixel outline colour, or ``null`` for no outline; defaults to ``#e4e9f2``;
* ``pad`` — the inset on all four sides, ``18.0`` pixels by default;
* ``gap`` — the space between two children, ``8.0`` pixels by default;
* ``corner`` — the corner radius, ``16.px`` by default.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.container.Card
import com.github.kusa233.ovatoxin.widget.container.card
```

Used directly, ``text`` and ``button`` draw their children into that card:

```kotlin
frame.layout {
    draw(Card(h = 180.px)) { card ->
        card.text("Overview", h = 28.px, size = 19.0, bold = true)
        card.text("This sentence wraps onto more than one line.", h = 36.px, size = 13.0, wrap = true)
        card.button("Explore") {
            println("clicked")
        }
    }
}
```

When the body is written as a block, the ``card(...)`` factory is shorter; it still returns a real ``Card``, so the result can be bound to a state or given an ``onClick`` later:

```kotlin
frame.layout {
    card(h = 180.px) {
        text("Overview", h = 28.px, size = 19.0, bold = true)
        button("Explore")
    }
}
```

Rules:

* ``Card`` is a ``FlexBox``: ``layout { ... }`` still decides its own position and size, and percentages inside it resolve against the card;
* ``text`` and ``button`` are only convenient ways to ``draw`` children into the card; when a divider or a row is needed, ``draw(FlexBox())``, ``draw(Div())`` and the rest of the DSL still work inside the body;
* the constructor's ``fill``, ``borderColor``, ``pad``, ``gap`` and ``corner`` are restored by ``reset()``, so a state-driven redraw cannot turn the card back into a plain white ``FlexBox``;
* the defaults of ``text`` and ``button`` are card-oriented: text is dark on a transparent background, and a button is blue with white text; both accept parameters to override them;
* ``text(...)``/``button(...)`` inside ``card(...)`` share the same defaults as the ``Card`` member methods.

## TabBar and TabPane

``TabBar`` is a ``FlexBox`` filled with tab buttons. ``TabPane`` holds one ``TabBar`` on one of its four sides and shows the content bound to the selected tab; content bound to every other tab is hidden.

Imports:

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.LEFT
import com.github.kusa233.ovatoxin.widget.container.TabPane
import com.github.kusa233.ovatoxin.widget.text.TextArea
```

```kotlin
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
* ``windowDragEnabled = true`` makes the empty background of the bar act as a window drag handle: press and drag there to move the window. Tabs and scroll bars keep their normal clicks and drags. Use it when a frameless window (``Frame(titleBarEnabled = false)``) should be moved by its tab bar; it is off by default and is also available as ``pane.windowDragEnabled``;
* A style set on the pane is applied to its bar too: the pane copies its background, corner radius, text colour and transition onto ``tab`` on every layout pass, so the strip shares the pane's appearance. A tab button has no fill of its own unless it sets one, so an unselected tab lets the pane's background show through instead of covering it with the control's default white. ``selectColor { background = Color.WHITE; foreground = Color.BLACK }`` paints the background and text colour of the selected tab only; an unset ``background`` or ``foreground`` keeps the button's own or inherited appearance. The older ``pane.selectColor = Color.WHITE`` property still works but is deprecated and sets only the background. ``pane.selectionStyle`` and ``pane.tab.selectionStyle`` are the same object, and ``pane.selectColor { }`` is equivalent to ``pane.tab.selectColor { }``;
* A ``transition`` written in the pane style reaches its tab buttons as well, so the ``selectColor`` switch animates when tabs change. A transition set on an individual tab button takes precedence over the pane's.

## Toast

``Toast`` is a layer that fills the surface it is drawn into. Calling ``show`` fades in a rounded card, leaves it there for the requested duration, and fades it out again. By default the layer is transparent and mouse-transparent, which suits messages such as "saved" or "request failed" that should not interrupt the user or take up room in the page layout. The card can hold a single sentence or any component drawn with the ``show { }`` block; when the page should be interrupted, ``dimOutside = true`` dims everything outside the toast.

A ``Toast`` is an **overlay**, not a widget that participates in page layout. Draw it as the last child of the surface it should cover, usually the whole window:

```kotlin
val toast = Toast()

frame.layout {
    draw(TabPane()) { /* page content */ }

    // Drawn last, so it floats above everything else.
    draw(toast) {
        layout {
            width = 100.percent
            height = 100.percent
        }
    }
}

toast.show("Settings saved")
```

Inside a ``SurfaceScope`` the ``toast()`` factory does the full-size geometry for you:

```kotlin
val toast = toast(duration = 3.seconds)

toast.show("An ordinary message")
toast.show("Saved: settings updated.", tone = ToastTone.SUCCESS)
toast.show(
    message = "Request failed: check your connection.",
    tone = ToastTone.ERROR,
    position = ToastPosition.TOP_CENTER,
)
toast.show("Centred message", position = ToastPosition.CENTER)

// The card body may also be a drawing block; components use the ordinary draw DSL.
toast.show(
    duration = 5.seconds,
    position = ToastPosition.CENTER,
    tone = ToastTone.SUCCESS,
    dimOutside = true,
) {
    text("A toast with components", height = 24.px, size = 17.0, color = Color.WHITE, bold = true)
    text("The page is dimmed; the button inside the card still works.", height = 36.px, size = 12.5, color = Color.WHITE, wrap = true)
    button(label = "Got it", width = 112.px, height = 34.px, fill = Color.WHITE) {
        toast.dismiss()
    }
}
```

Imports:

```kotlin
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.widget.container.button
import com.github.kusa233.ovatoxin.widget.container.text
import com.github.kusa233.ovatoxin.widget.feedback.Toast
import com.github.kusa233.ovatoxin.widget.feedback.ToastPosition
import com.github.kusa233.ovatoxin.widget.feedback.ToastTone
import com.github.kusa233.ovatoxin.widget.feedback.toast
import javafx.scene.paint.Color
import kotlin.time.Duration.Companion.seconds
```

Rules:

* Every parameter of ``show(message, duration, tone, position, dimOutside)`` has a default: ``duration`` is ``2500.milliseconds``, ``tone`` is ``ToastTone.INFO``, ``position`` is ``ToastPosition.BOTTOM_CENTER`` and ``dimOutside`` is ``false``. The constructor sets the defaults and each ``show`` call may override any of them;
* ``tone`` is a ``ToastTone(background, foreground)``; ``INFO``, ``SUCCESS``, ``WARNING`` and ``ERROR`` are provided. A brand colour can be created directly with ``ToastTone(Color.web("#4c8bf5"), Color.WHITE)``. A tone changes the card colours only — not the text, position or duration;
* ``position`` is one of ``TOP_START``, ``TOP_CENTER``, ``TOP_END``, ``BOTTOM_START``, ``BOTTOM_CENTER``, ``BOTTOM_END`` and ``CENTER``; it places the card inside the overlay, while the overlay itself always fills the surface. ``CENTER`` puts the card in the exact middle of the surface, which suits a confirmation that needs attention;
* A string draws the ordinary text card. The trailing-lambda ``show { }`` overload instead receives a ``FlexBox`` drawing surface, so the card may contain any components built with ``text(...)``, ``button(...)``, ``draw(...)`` and the rest of the DSL. The block runs on the JavaFX application thread when the card is created, not when ``show`` is called;
* A custom-content card has a fixed ``420px`` content width and grows vertically with its content; buttons inside it work even without a scrim;
* ``dimOutside = true`` puts a translucent scrim under the card, dims the page outside the toast, and makes the overlay receive input so the rest of the page cannot be clicked. The card itself stays interactive. With ``dimOutside = false`` an ordinary text toast remains mouse-transparent;
* Repeated ``show`` calls follow "the latest wins": the new message replaces the current card and restarts its timer. There is no queue and no stack of simultaneous cards;
* With ``duration = Duration.INFINITE`` (or any non-positive duration) the card stays until the next ``show`` or a call to ``dismiss()``; ``dismiss()`` stops the timer and fades the current card out;
* ``isShowing`` reports whether a message is active, while ``message``, ``currentTone``, ``currentPosition`` and ``currentDimOutside`` read the current state. A custom-content toast has no string message, so ``message`` is ``null``. These values update as soon as ``show`` is called, whether or not the node exists yet;
* ``show`` and ``dismiss`` may be called from any thread. Before the toolkit is running they only record the request; the node, drawing block and animations are created on the JavaFX application thread;
* ``reset()`` restores the transparent layer but deliberately keeps an active message, so a page redraw cannot make a toast disappear by accident;
* A ``Toast`` takes no space in the page layout and does not affect the size of its neighbours. For a persistent notice that belongs in the page, use an ordinary [Card](#card) or [TextArea](#textarea) instead.
