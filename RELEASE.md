# Ovatoxin v1.0.0-rc1

**Release date:** 2026-11-26  (UTC+9)
**Status:** Release candidate  
**License:** MIT

Ovatoxin is a modern Kotlin desktop UI framework built on JavaFX, designed around a declarative DSL and packed with convenient syntax. This is the first public release candidate.

## Highlights

- Declarative Kotlin DSL for building JavaFX desktop interfaces
- Window and frame support, including frameless mode and resize handling
- Layout and style templates with reusable presets
- Length units based on percentages, device-independent pixels, and font-relative ems
- Typography controls for font size and font weight
- Animated style transitions for colors, opacity, and corner radius
- A growing set of built-in UI components

## Window and frame

- `Frame` creates and shows a JavaFX window.
- `titleBarEnabled` selects whether the window is created with a native title bar.
- `onClose` subscribes to close requests from the title bar or the platform-equivalent close action.
- Frameless windows support custom resize handling.
- Window padding is configurable through the layout API.

## Declarative UI DSL

- Components are composed with `frame.layout { }` and `draw(...) { }`.
- Each component has its own `layout { }` and `style { }` blocks.
- `State` and `withState(...)` provide reactive redraws when values change.
- Templates can be created with top-level `layout { }` and `style { }` functions and applied to multiple components.
- Templates are copied into each component, so later changes to a template do not affect components that already used it.
- Components support `reset()` to return to their initial state on a redraw.

## Layout

- Position and size with `x`, `y`, `width`, and `height`.
- Padding with `hPadding` and `vPadding`.
- Three length units:
    - `percent` for percentages of the parent
    - `px` for device-independent pixels
    - `em` for values relative to the component's own font size
- Content inset behavior that keeps percentages relative to the padded area.
- `FlexBox` with wrapping, `gap`, `justify`, and `align`.
- `Div` as a general-purpose container.

## Styling

- Background color and background image support.
- Corner radius control.
- Foreground color control.
- Font size in pixels or as a percentage of the component's box height.
- Font weight support through JavaFX's `FontWeight`.
- Style presets that can be reused across components.

## Transitions

- Style changes can be animated with a Kotlin `Duration`.
- Background color, foreground color, opacity, and corner radius are interpolated.
- The first style application is immediate; transitions apply to later changes.
- Negative durations and `Duration.INFINITE` are rejected.

## Components

This release includes:

- `Button`
- `Label`
- `Link`
- `TextField`
- `TextArea`
- `CheckBox`
- `ComboBox`
- `ProgressBar`
- `ListView`
- `FlexBox`
- `Div`

## Installation

### Gradle with JitPack

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.cao-awa:Ovatoxin:1.0.0-rc1")
}
```

The corresponding Git tag is `1.0.0-rc1`.

The Gradle publication coordinates are:

```text
com.github.kusa233:ovatoxin:1.0.0-rc1
```

## Requirements

- JDK 25
- Kotlin 2.4.0
- JavaFX 25.0.5

## Known limitations

- Font weight rendering depends on the selected font family; some weights may fall back to a regular face with the default system font.
- `fontWeight` is not inherited by child components the way `fontSize` is.
- A percentage `fontSize` requires the component to have an explicit pixel or percentage height.
- Transitions do not interpolate font size or font weight.
- Frameless windows have no native close button and must provide their own close action.
- JavaFX dependencies are selected for the host platform; public multi-platform consumption should account for platform-specific JavaFX artifacts.

## Breaking changes

None. This is the initial release candidate.

## Feedback

Please report issues and feedback at:

https://github.com/cao-awa/Ovatoxin/issues

## License

Ovatoxin is released under the MIT License. See `LICENSE` for details.

**Full Changelog:** https://github.com/cao-awa/Ovatoxin/commits/1.0.0-rc1
