# Ovatoxin v1.0.0-rc4

**Release date:** 2026-11-26  (UTC+9)

**Status:** Release candidate

**License:** MIT

## Window size limits

* `Frame` now exposes `minWidth`, `minHeight`, `maxWidth` and `maxHeight` (also accepted by the constructor) to constrain user resizing. Minimums default to `0` and maximums to `Double.MAX_VALUE`.
* The limits apply to ordinary decorated windows through the JavaFX stage and are also enforced by the custom resize handles of a borderless window.
