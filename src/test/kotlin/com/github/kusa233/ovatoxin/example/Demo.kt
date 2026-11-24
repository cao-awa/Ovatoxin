package com.github.kusa233.ovatoxin.example

import com.github.kusa233.ovatoxin.example.I18N.languages
import com.github.kusa233.ovatoxin.layout.Length
import com.github.kusa233.ovatoxin.layout.Percent
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.state.State
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.Button
import com.github.kusa233.ovatoxin.widget.CheckBox
import com.github.kusa233.ovatoxin.widget.ComboBox
import com.github.kusa233.ovatoxin.widget.Div
import com.github.kusa233.ovatoxin.widget.FlexBox
import com.github.kusa233.ovatoxin.widget.Link
import com.github.kusa233.ovatoxin.widget.ListView
import com.github.kusa233.ovatoxin.widget.ProgressBar
import com.github.kusa233.ovatoxin.widget.TextArea
import com.github.kusa233.ovatoxin.widget.TextField
import com.github.kusa233.ovatoxin.window.Frame
import javafx.scene.paint.Color

fun main() {
    val frame = Frame()
    frame.title = "Ovatoxin – DSL demo"
    testPadding(frame)
    frame.show()
}

fun testProgressBarWithState(frame: Frame) {
    frame.layout {
        val progress = State(Percent.ZERO)
        draw(ProgressBar()) { bar ->
            layout(
                10.percent, 10.percent,
                80.percent, 5.percent
            )
            bar.progress(progress.value)
        }.withState(progress)

        Thread.startVirtualThread {
            while (true) {
                progress.update(
                    if (progress.value >= 100.percent) 0.percent else progress.value + 1.percent
                )
                Thread.sleep(100)
            }
        }
    }
}

fun testProgressBar(frame: Frame) {
    frame.layout {
        val progress = draw(ProgressBar()) {
            layout(
                10.percent, 10.percent,
                80.percent, 5.percent
            )
        }

        Thread.startVirtualThread {
            while (true) {
                progress.progress(
                    if (progress.progress >= 100.percent) 0.percent else progress.progress + 1.percent
                )
                Thread.sleep(100)
            }
        }
    }
}

fun testComboBox(frame: Frame) {
    frame.layout {
        draw(ComboBox(listOf("A", "B"))) { comboBox ->
            layout {
                x = 10.percent
                y = 10.percent
                width = 10.percent
                height = 5.percent
            }
        }
    }
}

fun testCheckBox(frame: Frame) {
    frame.layout {
        draw(CheckBox()) { checkBox ->
            layout {
                x = 10.percent
                y = 10.percent
                width = 30.percent
                height = 5.percent
            }
            checkBox.text = "Remember"
        }
    }
}

fun testButton(frame: Frame) {
    frame.layout {
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
                println("Button clicked!")
            }
        }
    }
}

fun testDiv(frame: Frame) {
    frame.layout {
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
    }
}

fun testPadding(frame: Frame) {
    frame.layout {
        draw(Div()) { flush ->
            layout {
                x = 10.percent
                y = 6.percent
                width = 80.percent
                height = 16.percent
            }
            style {
                background {
                    color = Style.DEFAULT_COLOR
                }
            }
            flush.layout {
                draw(TextArea()) { child ->
                    layout {
                        x = 0.percent
                        y = 0.percent
                        width = 100.percent
                        height = 40.percent
                    }
                    child.text("no padding: flush with the edge")
                }
            }
        }

        draw(Div()) { inset ->
            layout {
                x = 10.percent
                y = 24.percent
                width = 80.percent
                height = 16.percent
                hPadding = 20.px
                vPadding = 12.px
            }
            style {
                background {
                    color = Style.DEFAULT_COLOR
                }
            }
            inset.layout {
                draw(TextArea()) { child ->
                    layout {
                        x = 0.percent
                        y = 0.percent
                        width = 100.percent
                        height = 40.percent
                    }
                    child.text("20px / 12px padding: inset from the edge")
                }
            }
        }
    }
}

fun testFlexBox(frame: Frame) {
    frame.layout {
        draw(Div()) { panel ->
            layout {
                x = 10.percent
                y = 44.percent
                width = 80.percent
                height = 24.percent
            }
            style {
                background {
                    color = Style.DEFAULT_COLOR
                }
                corner = 12.percent
            }
            panel.layout {
                draw(FlexBox()) { row ->
                    layout {
                        x = 0.percent
                        y = 0.percent
                        width = 100.percent
                        height = 100.percent
                        hPadding = 8.px
                        vPadding = 8.px
                    }
                    row.layout {
                        for (index in 1..12) {
                            draw(Button()) { cell ->
                                layout {
                                    width = 18.percent
                                    height = 18.percent
                                }
                                cell.text = "$index"
                            }
                        }
                    }
                }
            }
        }
    }
}

fun testLink(frame: Frame) {
    frame.layout {
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
}

fun testList(frame: Frame) {
    open class ListOp; class ListDoNothingOp : ListOp()
    class ListAdd(val item: String) : ListOp(); class ListRemove(val item: String) : ListOp()

    val opState = State<ListOp>(ListDoNothingOp())
    frame.layout {
        val list = draw(ListView(mutableListOf("A", "B", "C"))) { list ->
            layout(
                10.percent, 10.percent, 80.percent, 50.percent
            )
            when (val op = opState.value) {
                is ListDoNothingOp -> {}
                is ListAdd -> list.add(op.item)
                is ListRemove -> list.remove(op.item)
            }
        }.withState(opState)
        val adding = draw(TextField()) { adding ->
            layout(
                10.percent, 70.percent, 80.percent, 10.percent
            )
            adding.hint = "Adding item..."
        }
        draw(Button()) { remove ->
            layout(
                10.percent, 90.percent, 30.percent, 5.percent
            )
            remove.onClick {
                if (list.selectedItem != null) opState.update(ListRemove(list.selectedItem!!))
            }
            remove.text("Remove item...")
        }
        draw(Button()) { add ->
            layout(
                50.percent, 90.percent, 30.percent, 5.percent
            )
            add.onClick {
                if (adding.text().isNotEmpty()) opState.update(ListAdd(adding.text()))
            }
            add.text("Add item...")
        }
    }
}

fun testStateWithColor(frame: Frame) {
    val state = State("")
    frame.layout {
        draw(TextField()) { input ->
            style {
                background {
                    val colorCode = state.value.let {
                        if (it.startsWith("#") && it.length == 7) {
                            it
                        } else {
                            "#ffffff"
                        }
                    }
                    color = Color.web(colorCode)
                }
            }
            layout {
                x = 10.percent
                y = 30.percent
                width = 80.percent
                height = 10.percent
            }
            input.hint = "Type color here..."
            input.onChange {
                state.update(it)
            }
        }.withState(state)
    }
}

fun testStateWithI18N(frame: Frame) {
    val state = State("")
    frame.layout {
        draw(TextArea()) { text ->
            layout(
                10.percent, 10.percent,
                80.percent, 10.percent
            )
            val translation = languages[state.value]?.get("test.translation") ?: "No translation"
            text.text(translation)
        }.withState(state)
        draw(TextField()) { input ->
            layout(
                0.percent, 0.percent,
                80.percent, 10.percent
            )
            input.hint = "Type here..."
            input.onChange {
                state.update(it)
            }
        }
    }
}

object I18N {
    val languages: Map<String, MutableMap<String, String>> = mapOf(
        "zh-cn" to mutableMapOf("test.translation" to "翻译测试"),
        "en-us" to mutableMapOf("test.translation" to "Test translation"),
    )
}
