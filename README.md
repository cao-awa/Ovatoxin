# Ovatoxin

[English](README.md) | [中文](README_zh.md)

**Ovatoxin** is a modern Kotlin desktop UI framework built on JavaFX, designed around a DSL and rife with sugars.

# Getting started

Add the following to your ``build.gradle`` file:

```groovy
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

def javafxVersion = '25.0.5'

dependencies {
    implementation 'com.github.cao-awa:Ovatoxin:{ovatoxin_version}'

    api "org.openjfx:javafx-base:${javafxVersion}:win"
    api "org.openjfx:javafx-graphics:${javafxVersion}:win"
    api "org.openjfx:javafx-controls:${javafxVersion}:win"
    api "org.openjfx:javafx-media:${javafxVersion}:win"
    api "org.openjfx:javafx-base:${javafxVersion}:mac"
    api "org.openjfx:javafx-graphics:${javafxVersion}:mac"
    api "org.openjfx:javafx-controls:${javafxVersion}:mac"
    api "org.openjfx:javafx-media:${javafxVersion}:mac"
    api "org.openjfx:javafx-base:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-graphics:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-controls:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-media:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-base:${javafxVersion}:linux"
    api "org.openjfx:javafx-graphics:${javafxVersion}:linux"
    api "org.openjfx:javafx-controls:${javafxVersion}:linux"
    api "org.openjfx:javafx-media:${javafxVersion}:linux"
    api "org.openjfx:javafx-base:${javafxVersion}:linux-aarch64"
    api "org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64"
    api "org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64"
    api "org.openjfx:javafx-media:${javafxVersion}:linux-aarch64"
}
```

For versions, see [JitPack](https://jitpack.io/#cao-awa/Ovatoxin).

## Documents

* [Introduction](docs/README.md)
* [Usage](docs/usage/README.md)
* [Components](docs/component/README.md)

## Example
The following example shows a window without a title bar.
The top 8% of the height, spanning 100% of the width, is painted with a FlexBox on a blue background.
It contains four buttons; the selected button's background turns white (with a 200 ms animated transition), while the others match the FlexBox.

Note: this example removes the title bar and draws no exit button, so it cannot be closed; close it through the IDE or Task Manager.

```kotlin
// Create the frame
val frame = Frame("Ovatoxin - Tab bar demo", titleBarEnabled = false)
// Layout preset
val layout = layout {
    width = 20.percent
    height = 80.percent
    vPadding = 10.percent
}
// Style preset
val style = style {
    corner = 55.percent
    transition = 200.milliseconds
}
// State used for redrawing
val selected = State(0)
frame.layout {
    // Draw a FlexBox
    draw(FlexBox()) { flexBox ->
        // Only the top 8% of the height - this is a tab bar
        layout(0.percent, 0.percent, 100.percent, 8.percent)
        // Blue background
        style { background { color = Color.web("#4c8bf5") } }
        // Centre the contents
        flexBox.justify = Justify.CENTER
        // 1em spacing between elements
        flexBox.gap = 1.em
        flexBox.layout {
            // Create four buttons
            for (index in 0..3) {
                draw(Button()) { button ->
                    // Use the template layout
                    layout(layout)
                    // Fine-tune after applying the template
                    style(style) {
                        background {
                            // When selected, make the background white; otherwise match the FlexBox
                            color = if (selected.value == index) Color.WHITE else Color.web("#4c8bf5")
                        }
                        // When selected, match the FlexBox color; otherwise white
                        foreground = if (selected.value == index) Color.web("#4c8bf5") else Color.WHITE
                    }
                    // Button text
                    button.text = "$index"
                    // Redraw when the button is clicked
                    button.onClick {
                        selected.update(index)
                    }
                }
            }
        }
    }.withState(selected) // Bind the State so the component redraws
}
// Show the frame
frame.show()
```

# Loli count

![](https://count.getloli.com/@@cao-awa.ovatoxin?name=%40cao-awa.ovatoxin&padding=7&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)
