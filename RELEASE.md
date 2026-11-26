# Ovatoxin v1.0.0-rc4

**Release date:** 2026-11-27  (UTC+9)

**Status:** Release candidate

**License:** MIT

This release adds cards and a full-featured toast overlay, rounds out the style system with borders, and makes state-driven redraws safe from background threads. It also brings layout helpers, window sizing and dragging improvements, and a recolourable progress bar.

## New components

### Card

* `Card` is a rounded, padded, vertically spaced `FlexBox` for grouping related content. Its defaults are a white fill, a 1 px `#e4e9f2` outline, `18` px padding, an `8` px gap and a `16.px` corner.
* Constructor values for fill, border, padding, gap and corner survive `reset()`, so a state-driven redraw keeps the card's own look instead of turning it back into a plain white `FlexBox`.
* `Card.text(...)` and `Card.button(...)` draw the two kinds of content a card almost always holds; `card(...)` is a shorter factory for writing the body as a block and returns the same `Card`, so it can still be clicked, bound to a state or inspected.
* Text helpers default to dark `#162033` text on a transparent background; buttons default to a `#4c8bf5` fill with white text. The card body remains a real drawing surface, so rows, dividers and any other component can still be drawn directly.

### Toast

* `Toast` is a full-surface overlay: after `show`, a rounded card fades in, stays for a configurable duration and fades out. It is transparent and mouse-transparent by default, so a normal message does not block the page.
* `show("text")` displays a message card; `show { ... }` hands the card body to a `FlexBox` drawing surface, where `text(...)`, `button(...)`, `draw(...)` and the usual DSL can place arbitrary components.
* `ToastTone` provides `INFO`, `SUCCESS`, `WARNING` and `ERROR`; custom tones can be constructed from a background and foreground colour. `ToastPosition` supports `TOP_START`, `TOP_CENTER`, `TOP_END`, `BOTTOM_START`, `BOTTOM_CENTER`, `BOTTOM_END` and `CENTER`.
* Custom-content cards have a fixed `420px` content area and grow in height with their children. Buttons inside a card remain clickable even without the scrim, and the card body is transparent so the tone-coloured background is not covered by a white sheet; each child keeps control of its own text colour.
* `dimOutside = true` dims the page behind the card and blocks clicks outside it while leaving the card interactive. The latest `show` replaces the current card and restarts the timer; `dismiss()` fades it out early, and an infinite duration keeps it visible until replaced or dismissed.
* `show` and `dismiss` may be called from any thread: before the toolkit starts the request is only recorded, and the node, drawing block and animation are created on the JavaFX application thread. `reset()` restores the transparent overlay but keeps the currently displayed message.
* Draw the `Toast` after the content it should cover, normally as the last child of the frame. The `toast()` factory is available inside a `SurfaceScope` and sets the overlay to `100.percent` width and height.

## Layout helpers

* `flexFlow(...)` draws a full-width `FlexBox` row with a fixed height, configurable gap, padding, alignment, optional fill and optional border, while its body is declared with the normal layout DSL.
* `progressBar(...)` draws a horizontal `ProgressBar` with an explicit height and value; the filled part uses `tone` and the optional `track` colour paints the empty part.
* `bareTextArea(...)` draws a `TextArea` with a transparent background, so text sits directly on the surrounding surface instead of on a white rectangle. The transparent fill is part of the draw block and survives state-driven redraws.

## Style borders

* The style DSL now has a `border { size = …; color = … }` block. `size` is a `Length`, so it accepts `px`, `percent` (of the shorter side, like `corner`) and `em`; a negative size is treated as zero.
* The border follows the corner radius and is animated together with the rest of the style through `transition`. It is disabled while `size` is zero and can be removed with `color = null` or `size = 0.px`.

## Progress bar colour

* `ProgressBar` now paints its filled portion with the style's `foreground` colour, while `background` keeps painting the empty track. A bar with no foreground keeps the widget's default blue band, and an animated foreground is applied on every style frame.

## Window and tabs

* `Frame` now exposes `minWidth`, `minHeight`, `maxWidth` and `maxHeight` (also accepted by the constructor) to constrain user resizing. Minimums default to `0` and maximums to `Double.MAX_VALUE`.
* The limits apply to ordinary decorated windows through the JavaFX stage and are also enforced by the custom resize handles of a borderless window.
* `TabBar.windowDragEnabled` turns the empty background of the bar into a window drag handle, so a frameless window can use the bar as a title bar. Tabs, scroll bars and other children keep their normal click and drag behaviour; the option can also be set through `TabPane.windowDragEnabled`.
* `selectColor { background = …; foreground = … }` paints the selected tab's background and text colour independently. An unset part keeps the tab's own or inherited appearance, and unselected tabs are restored to their base style.

## Thread safety and fixes

* `State.update` may now be called from any thread. Once a bound component has a JavaFX node, its redraw is marshalled onto the JavaFX application thread instead of touching scene graph properties from the producer's thread; before a node exists the redraw runs in place so components remain usable without a toolkit.
* Repainting a single state-bound child inside a `FlexBox` no longer moves that child to the top of the box: the whole child list is measured on every pass, so the flow knows where earlier lines end.

## Compatibility

* No breaking API changes from `v1.0.0-rc3`; all additions are additive.
