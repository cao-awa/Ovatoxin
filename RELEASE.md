# Ovatoxin v1.0.0-rc7

**Release date:** 2026-11-27  (UTC+9)

**Status:** Release candidate

**License:** MIT

This release adds first-class internationalization, introduces the `VideoPane` media component, publishes JavaFX
artifacts for every desktop platform, and changes the text API to a call-only form.

## Internationalization

* Added the process-wide `I18N` registry with `I18N.lang(langKey, translationKey, content)` and `I18N.currentLang`. The
  first registration selects `currentLang`; an unknown key falls back to the key itself.
* Added `Translatable`, plus the `String.translatable` extension, so a translation key can be passed around before a
  language is selected:

  ```kotlin
  label.text("No translatable")          // always a literal string
  label.text("translate.key".translatable)
  label.text(Translatable("translate.key"))
  ```

* Every widget with a caption now accepts a literal `String`, a `Translatable`, or `String.translatable`: `Button`,
  `Label`, `TextArea`, `Link`, `CheckBox`, `TextField`, and the `Card` text/button helpers.
* A widget given a `Translatable` keeps the key and re-resolves it whenever `I18N.currentLang` changes, so existing
  components update without being rebuilt. A literal string is never translated, even when it happens to look like a
  key.
* `TextField` is the deliberate exception: its content is user input, so a translatable value is resolved when written
  and a later language switch does not overwrite what the user has typed.
* `Card.text(...)`, `Card.button(...)`, and the top-level card DSL overloads accept both literal strings and translation
  keys.

## Breaking text API change

* `text` is now a read-only property, and the only way to replace it is the `text(...)` method. Property assignment no
  longer compiles:

  ```kotlin
  label.text = "hello"   // no longer compiles
  label.text("hello")    // use this instead
  ```

* Reading through `text` is unchanged, for example `label.text` or `button.text()`.
* Existing literal captions keep their previous behaviour; only the assignment syntax must be migrated.

## New component: VideoPane

* `VideoPane` plays a video from a local file, a `file`/`jar` URI, an `http`/`https` URL, or a `classpath:` resource.
  `File`, `URI`, and `URL` constructors perform the usual conversion, and `videoPane(...)` is the shorthand factory.
* The pane is a black drawing surface by default, so letterbox bars hide the page behind a video whose aspect ratio
  differs from its box. A non-zero `corner` clips both the fill and the picture to the same rounded outline.
* The player is created lazily with the node and only when `source` is non-empty. A missing or unreadable source is
  reported through `onError` and leaves the pane in place instead of breaking the draw tree.
* Playback is controlled with `play()`, `pause()`, `stop()`, `togglePlayPause()`, and `seekTo(duration)`. Calls made
  before the node or media is ready are remembered and applied once loading finishes.
* `autoPlay`, `loop`, `muted`, `volume`, and `preserveRatio` are readable and writable. Volume is clamped to `0.0`–
  `1.0`, and `preserveRatio` defaults to `true`.
* `status`, `isPlaying`, `position`, and `duration` expose the current state with neutral values before a player exists,
  making them safe to read from a draw block.
* `onReady`, `onEnd`, `onError`, and `onStatusChanged` report media events on the JavaFX application thread.
* `reset()` restores the source and playback values the pane was created with. An unchanged source keeps playing where
  it was; a different source reloads from the beginning. Handlers must be registered inside the draw block.
* The player is released when the component is removed from its container; `dispose()` releases it immediately and stops
  playback. A new protected `Component.onNodeDetached(node)` hook lets any widget release resources that are not
  reachable from its JavaFX node.
* `VideoPane` requires the `javafx-media` module; the Gradle snippets in the README now include it for every platform.

## Platform and build

* JavaFX dependencies are now exported as `api` artifacts for all supported desktop classifiers: `win`, `mac`,
  `mac-aarch64`, `linux`, and `linux-aarch64`. Published POMs no longer depend on the operating system and architecture
  of the build machine.
* The README dependency snippets were updated to list every platform classifier, including `javafx-media`.
* Removed the obsolete `example/full/FullSample.kt`; the maintained examples and the documented DSL snippets are the
  reference samples.

## Compatibility

* This release is source-breaking for code that assigns to `text`; replace `.text = value` with `.text(value)`. Property
  reads are unchanged.
* Literal strings keep their existing behaviour, and the new translation overloads are additive.
* `VideoPane` is new and does not affect existing component trees. Applications that use it must add the `javafx-media`
  dependency.
* All existing `Div`, `FlexBox`, `CollapsePane`, scrolling, and widget APIs remain compatible with `v1.0.0-rc5`.
