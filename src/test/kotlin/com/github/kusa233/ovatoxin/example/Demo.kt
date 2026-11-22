package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.widget.TextField
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color

/**
 * The example from `doc/DSL.md`, plus a [Div] panel.
 *
 * It lives in the test source set on purpose: it is a manual smoke test, not part of the
 * published library. Run it with `gradlew demo`.
 */
fun main() {
    val frame = Frame()
    frame.title = "Ovatoxin – DSL demo"
    frame.layout {
        val textShow = draw(TextArea()) { text ->
            layout {
                x = 10.percent // Or 10.p
                y = 10.percent
                width = 80.percent
                height = 10.percent
            }
            text.text("Test")
            text.onClick {
                println("Text area clicked!")
            }
        }
        val textInput = draw(TextField()) { input ->
            layout {
                x = 10.percent
                y = 30.percent
                width = 80.percent
                height = 10.percent
            }
            input.hint = "Type here..."
            input.onChange { println("typed: $it") }
        }
        draw(Button()) { button ->
            layout {
                x = 10.percent
                y = 50.percent
                width = 80.percent
                height = 10.percent
            }
            style {
                background {
                    color = Color.web("#4c8bf5")
                }
                corner = 35.percent
                foreground = Color.web("#ffffff")
            }
            button.text = "Button"
            button.onClick {
                textShow.text(textInput.text())
            }
        }

        draw(Div()) { band ->
            layout {
                x = 10.percent
                y = 65.percent
                width = 80.percent
                height = 5.percent
            }
            style {
                background {
                    color = Color.web("#4c8bf5")
                    opacity = 25 // A whole percentage: 25, not 25.percent
                }
                corner = 50.percent
            }
        }

        draw(Div()) { panel ->
            layout {
                x = 10.percent
                y = 74.percent
                width = 80.percent
                height = 20.percent
            }
            style {
                background {
                    color = Style.DEFAULT_COLOR
                }
                corner = 12.percent
            }
            panel.layout {
                draw(TextArea()) { left ->
                    layout {
                        x = 0.percent
                        y = 10.percent
                        width = 50.percent
                        height = 80.percent
                    }
                    left.text("left half of the panel")
                }
                draw(TextArea()) { right ->
                    layout {
                        x = 100.percent
                        y = 10.percent
                        width = 50.percent
                        height = 80.percent
                    }
                    right.text("outside the panel, not rendered")
                }
            }
        }

        draw(Link("Ovatoxin on GitHub", "https://github.com/cao-awa/Ovatoxin")) { link ->
            layout {
                x = 10.percent
                y = 90.percent
                width = 80.percent
                height = 10.percent
            }
            link.text = "opens the browser — click me"
        }
    }
    frame.show()
}
