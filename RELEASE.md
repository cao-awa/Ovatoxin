# Ovatoxin v1.0.0-rc3

**Release date:** 2026-11-26  (UTC+9)

**Status:** Release candidate

**License:** MIT

This release adds tabbed panes and completes the component reference in English and Chinese.

## New components

### TabPane

* A container with a `TabBar` on any of four sides: `TabSide.UP`, `TabSide.BOTTOM`, `TabSide.LEFT` or `TabSide.RIGHT` (also available unqualified as `UP`, `BOTTOM`, `LEFT` and `RIGHT`).
* `add(button)` adds a tab and selects it; `bind(tab, component)` attaches one or more components to a tab. Selecting a tab shows its bound components and hides the others without rebuilding them.
* Bound components fill the content area that remains after the bar: a missing width or height becomes `100.percent` of that area, while an explicit size is kept. `x` and `y` of tab buttons and bound content are controlled by the pane.
* `barSize` sets the strip thickness across the side it occupies (default `36.px`); `tabBarThickness` remains as a source-compatible alias.
* `selectColor { background = ...; foreground = ... }` paints the selected tab's background and text color; an unset part keeps the tab's own or inherited appearance. The single-colour `selectColor = ...` property remains as a deprecated alias that sets only the background.

### TabBar

* A `FlexBox` subclass that arranges its buttons in one row for `UP`/`BOTTOM` or one column for `LEFT`/`RIGHT`.
* `add` selects the new tab and `remove` removes it, selecting the first remaining tab when the removed one was selected. `bind` is also available on a standalone bar for content rendered elsewhere.
* The bar owns button positions and stretches each button across the bar thickness; a horizontal bar centres its tabs by default.
* `selectColor { ... }`, `selectionStyle`, `selected`, `selectedButton` and `tabs` expose selection state and the selected-tab style. With no selection style set, no selected/unselected background difference is painted.
* `windowDragEnabled` lets the empty background of the bar drag its window, so a frameless window can use the bar as a title bar. It is off by default, exposed as `pane.windowDragEnabled` on `TabPane`, and leaves tab clicks and scroll bars untouched.

## Tab scrolling

* A `TabBar` that outgrows its allotted space now scrolls instead of wrapping. A horizontal bar shows a horizontal scroll bar and a vertical bar shows a vertical scroll bar, each only when needed.
* The selected tab is scrolled into view automatically when the selection changes.
* The scroll bars use a flat grey thumb with no track, frame or arrow buttons, matching the rest of the flat styling.
* Tab selection participates in the normal style transition system.

## Documentation

* `docs/component/README.md` and `docs/component/README_zh.md` now cover every public component in mirrored English and Chinese: `Label`, `TextArea`, `Link`, `Button`, `TextField`, `CheckBox`, `ComboBox`, `ListView`, `ProgressBar`, `Div`, `FlexBox`, and `TabBar`/`TabPane`.
* Each section documents imports, a concrete example, constructor parameters, properties, handlers, defaults, `reset()` semantics and the behaviour a component deliberately does not provide.
* Both component indexes include a contents list and a shared description of common component behaviour (`layout`, `style`, `onClick`, `visible`, `withState`, `reset()` and default colours).

## Compatibility

* No breaking API changes from `v1.0.0-rc2`; all additions are additive.
* `TabPane.tabBarThickness` remains a source-compatible alias for `barSize`.
