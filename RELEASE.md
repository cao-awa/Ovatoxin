# Ovatoxin v1.0.0-rc4

**Release date:** 2026-11-26  (UTC+9)

**Status:** Release candidate

**License:** MIT

## Style borders

* The style DSL now has a `border { size = …; color = … }` block. `size` is a `Length`, so it accepts `px`, `percent` (of the shorter side, like `corner`) and `em`; a negative size is treated as zero.
* The border follows the corner radius and is animated together with the rest of the style through `transition`. It is disabled while `size` is zero and can be removed with `color = null` or `size = 0.px`.

## Progress bar color

* `ProgressBar` now paints its filled portion with the style's `foreground` colour, while `background` keeps painting the empty track. A bar with no foreground keeps the widget's default blue band.

## Window size limits

* `Frame` now exposes `minWidth`, `minHeight`, `maxWidth` and `maxHeight` (also accepted by the constructor) to constrain user resizing. Minimums default to `0` and maximums to `Double.MAX_VALUE`.
* The limits apply to ordinary decorated windows through the JavaFX stage and are also enforced by the custom resize handles of a borderless window.
