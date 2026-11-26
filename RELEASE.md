# Ovatoxin v1.0.0-rc5

**Release date:** 2026-11-27  (UTC+9)

**Status:** Release candidate

**License:** MIT

This release adds a new collapsible panel, makes `Div` and `FlexBox` scroll vertically, and turns the full sample application into a complete 14-page component gallery with a runnable example on every page.

## New components

### CollapsePane

* `CollapsePane` is a panel with a clickable header that shows or hides its body. Collapsed, it keeps only the header and takes `collapsedHeight`, which defaults to `headerHeight`; expanded, it takes `expandedHeight` and the body receives the remaining space below the header.
* Clicking the header toggles the panel directly; no extra button is required. The header shows `▾` when expanded and `▸` when collapsed, and the arrow colour follows `headerForeground`.
* The body is declared with `content(...)`, which opens a normal `FlexBox` surface: `draw(...)`, `text(...)`, `button(...)` and the usual layout DSL work exactly as in any other container. `contentArea` exposes the internal `FlexBox` for inspection.
* The body scrolls vertically when its content is taller than the space left below the header. The header and body surfaces are transparent, so the panel fill, border and corner radius stay consistent behind the content.
* Constructor values for title, expanded/collapsed height, header height, fill, border, corner, padding and header text colour survive `reset()`, so state-driven redraws keep the panel's own appearance.
* `expanded` reads or sets the state, `toggle()` flips it, and `title` can be changed at any time. `CollapsePane.contentArea` gives access to the body surface, and `collapsePane(...)` is the shorthand factory for declaring a panel and its body in one call.

## Scrolling containers

* `Div.scrollY` and `FlexBox.scrollY` turn the component's own height into a vertical viewport. Content that exceeds that height scrolls up and down, while horizontal scrolling is disabled and the width still fits the container.
* Percentages inside a scrolling container resolve against the visible viewport height, not the full scroll extent. For a scrolling `FlexBox`, `gap`, `justify`, `align` and percentage sizing continue to use the visible content area.
* A single-line `FlexBox` line is allowed to grow taller than the viewport when `scrollY` is enabled, so tall padded leaves are not squeezed into the visible area.
* `scrollY` is off by default and must be set before the component is rendered. Changing it afterwards throws `IllegalStateException`, because a scrolling component needs a `ScrollPane` node that cannot be swapped after creation.
* Scrolling is implemented by an internal `ScrollSurface` around a `ScrollContentPane`; the viewport is fitted to the content width, vertical bars appear only when needed, and the stylesheet keeps the scroll pane frame transparent with a narrow flat grey bar.
* `Container.afterGeometry` was added as a post-placement hook so a scrolling surface can measure how far its placed children reach and size the inner content pane correctly.

## Fixes

* Fixed `CollapsePane` rendering white even when given a dark `fill`: its header and body `FlexBox` surfaces were reset to the default white fill on every draw. Their transparent backgrounds and header text colour are now applied inside their own draw blocks, so they survive `reset()` and follow the configured `headerForeground`.
* Fixed the header click handler being cleared by the same reset. The panel now installs `onClick` after the child surface has been reset, so clicking the title row reliably calls `toggle()`.
* Fixed scrolling `Div`/`FlexBox` measurement so a content pane never shrinks below the viewport and long content still produces a scrollable extent.

## Compatibility

* No breaking API changes from `v1.0.0-rc4`; all additions are additive.
* `scrollY` defaults to `false`, so existing `Div` and `FlexBox` layouts keep their existing non-scrolling behaviour.
* `CollapsePane` is new and does not affect existing component code.
