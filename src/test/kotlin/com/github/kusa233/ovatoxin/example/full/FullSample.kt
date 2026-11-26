package com.github.kusa233.ovatoxin.example.full

import com.github.kusa233.ovatoxin.dsl.layout
import com.github.kusa233.ovatoxin.dsl.style
import com.github.kusa233.ovatoxin.layout.Layout
import com.github.kusa233.ovatoxin.layout.percent
import com.github.kusa233.ovatoxin.layout.px
import com.github.kusa233.ovatoxin.style.Style
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.widget.container.FlexBox
import com.github.kusa233.ovatoxin.widget.container.TabPane
import com.github.kusa233.ovatoxin.widget.container.TabSide
import com.github.kusa233.ovatoxin.widget.text.TextArea
import com.github.kusa233.ovatoxin.window.Frame
import javafx.application.Platform
import javafx.scene.paint.Color
import kotlin.time.Duration.Companion.milliseconds

object FullSample {
    @JvmStatic
    fun main(args: Array<String>) {
        val frame = Frame("Ovatoxin full sample", titleBarEnabled = false)
        drawBasePane(frame)
        frame.show()
    }

    fun drawBasePane(frame: Frame) {
        val paneLayout = layout {
            width = 100.percent
            height = 100.percent
        }
        val paneStyle = style {
            background {
                color = Color.WHITE
            }
            fontSize = 16.px
        }
        frame.layout {
            draw(TabPane()) { pane ->
                layout {
                    width = 100.percent
                    height = 100.percent
                }
                style {
                    background {
                        color = Color.web("#6a9cf7")
                    }
                    foreground = Color.WHITE
                    transition = 100.milliseconds
                }
                pane.selectColor {
                    background = Color.WHITE
                    foreground = Color.BLACK
                }
                pane.side = TabSide.LEFT
                pane.barSize = 100.px
                bindMain(pane, paneLayout, paneStyle)
                bindTextArea(pane, paneLayout, paneStyle)
                bindTextField(pane, paneLayout, paneStyle)
                bindLabel(pane, paneLayout, paneStyle)
                bindButton(pane, paneLayout, paneStyle)
                pane.select(0)
            }
        }
    }

    fun bindMain(pane: TabPane, layout: Layout, style: Style) {
        val button = pane.add(Button("主界面"))
        val buttonStyle = style {
            background {
                color = Color.web("#6a9cf7")
            }
            corner = 45.percent
            foreground = Color.WHITE
        }
        pane.bind(button, FlexBox()) { box ->
            layout(layout) {
                hPadding = 5.percent
            }
            style(style)
            box.gap = 5.percent
            box.layout {
                draw(
                    TextArea(
                        """
                    本样例是组件集合大汇总，每个页面都有一个对应的简单用例，
                    可以通过左侧TabBar或者以下按钮前往特定示例。
                    """
                    )
                ) { summary ->
                    layout {
                        x = 10.percent
                        y = 5.percent
                        width = 100.percent
                        height = 15.percent
                    }
                    summary.wrap = true
                }
                // Padding manually
                draw(Div()) {
                    layout {
                        x = 10.percent
                        y = 20.percent
                        width = 100.percent
                        height = 5.percent
                    }
                }
                draw(Button("TextArea")) { area ->
                    layout {
                        width = 20.percent
                        height = 8.percent
                    }
                    style(buttonStyle)
                    area.onClick {
                        pane.select(1)
                    }
                }
                draw(Button("TextField")) { field ->
                    layout {
                        width = 20.percent
                        height = 8.percent
                    }
                    style(buttonStyle)
                    field.onClick {
                        pane.select(2)
                    }
                }
                draw(Button("Label")) { label ->
                    layout {
                        width = 20.percent
                        height = 8.percent
                    }
                    style(buttonStyle)
                    label.onClick {
                        pane.select(3)
                    }
                }
                draw(Button("Button")) { button ->
                    layout {
                        width = 20.percent
                        height = 8.percent
                    }
                    style(buttonStyle)
                    button.onClick {
                        pane.select(4)
                    }
                }
                draw(Button("退出")) { exit ->
                    layout {
                        width = 20.percent
                        height = 8.percent
                    }
                    style(buttonStyle)
                    exit.onClick {
                        Platform.exit()
                    }
                }
            }
        }
    }

    fun bindTextArea(pane: TabPane, layout: Layout, style: Style) {
        val button = pane.add(Button("TextArea"))
        pane.bind(button, Div()) { div ->
            layout(layout)
            style(style)
        }
    }


    fun bindTextField(pane: TabPane, layout: Layout, style: Style) {
        val button = pane.add(Button("TextField"))
        pane.bind(button, Div()) { div ->
            layout(layout)
            style(style)
        }
    }

    fun bindLabel(pane: TabPane, layout: Layout, style: Style) {
        val button = pane.add(Button("Label"))
        pane.bind(button, Div()) { div ->
            layout(layout)
            style(style)
        }
    }

    fun bindButton(pane: TabPane, layout: Layout, style: Style) {
        val button = pane.add(Button("Button"))
        pane.bind(button, Div()) { div ->
            layout(layout)
            style(style)
        }
    }
}