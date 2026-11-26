# Ovatoxin

[English](README.md) | [中文](README_zh.md)

**Ovatoxin**是一个基于JavaFX的Kotlin现代化桌面端UI框架，采用DSL设计，充斥着大量语法糖。

# 开始使用

添加如下代码至``build.gradle``文件:

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
    api "org.openjfx:javafx-base:${javafxVersion}:mac"
    api "org.openjfx:javafx-graphics:${javafxVersion}:mac"
    api "org.openjfx:javafx-controls:${javafxVersion}:mac"
    api "org.openjfx:javafx-base:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-graphics:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-controls:${javafxVersion}:mac-aarch64"
    api "org.openjfx:javafx-base:${javafxVersion}:linux"
    api "org.openjfx:javafx-graphics:${javafxVersion}:linux"
    api "org.openjfx:javafx-controls:${javafxVersion}:linux"
    api "org.openjfx:javafx-base:${javafxVersion}:linux-aarch64"
    api "org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64"
    api "org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64"
}
```

版本请见：[JitPack](https://jitpack.io/#cao-awa/Ovatoxin)。

## 文档

* [介绍](docs/README_zh.md)
* [用法](docs/usage/README_zh.md)
* [组件](docs/component/README_zh.md)

## 示例
以下示例展示一个无标题栏的窗口，
最顶部8%高度、100%宽度的区域被绘制一个蓝色背景的FlexBox，
里面有4个按钮，选中时背景变为白色（200ms动画渐变），其他时候与FlexBox同色。

注意：这个示例去除了标题栏，也没有绘制退出按钮，因此没法关闭，只能在IDE或任务管理器中强制关闭。

```kotlin
// 创建窗口
val frame = Frame("Ovatoxin - Tab bar demo", titleBarEnabled = false)
// 布局预设
val layout = layout {
    width = 20.percent
    height = 80.percent
    vPadding = 10.percent
}
// 样式预设
val style = style {
    corner = 55.percent
    transition = 200.milliseconds
}
// 用于重绘的State
val selected = State(0)
frame.layout {
    // 绘制一个FlexBox
    draw(FlexBox()) { flexBox ->
        // 只占最顶端的8%，这是一个Tab bar
        layout(0.percent, 0.percent, 100.percent, 8.percent)
        // 蓝色背景
        style { background { color = Color.web("#4c8bf5") } }
        // 居中对其
        flexBox.justify = Justify.CENTER
        // 元素间隔1em
        flexBox.gap = 1.em
        flexBox.layout {
            // 创建四个按钮
            for (index in 0..3) {
                draw(Button()) { button ->
                    // 使用模板布局
                    layout(layout)
                    // 使用模板布局后微调
                    style(style) {
                        background {
                            // 当selected时将背景改为白色，否则和FlexBox相同
                            color = if (selected.value == index) Color.WHITE else Color.web("#4c8bf5")
                        }
                        // 当selected时将背景改为和FlexBox相同颜色，否则白色
                        foreground = if (selected.value == index) Color.web("#4c8bf5") else Color.WHITE
                    }
                    // 按钮文本
                    button.text = "$index"
                    // 点击按钮时触发更新重绘
                    button.onClick {
                        selected.update(index)
                    }
                }
            }
        }
    }.withState(selected) // 绑定State用于重绘
}
// 展示窗口
frame.show()
```

# Loli count

![](https://count.getloli.com/@@cao-awa.ovatoxin?name=%40cao-awa.ovatoxin&padding=7&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)