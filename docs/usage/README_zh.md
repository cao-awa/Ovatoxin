# 窗口

创建一个窗口需要导入``Frame``类：
```kotlin
import com.github.kusa233.ovatoxin.window.Frame
```

直接实例化后调用``.show()``即可显示窗口：
```kotlin 
val frame = Frame()
frame.show()
```

使用``.title = "Title"``来设置窗口标题：
```kotlin
frame.title = "Ovatoxin - demo"
```

# 布局和渲染

在此先引入按钮作为示例，API请在后续内容了解。

首先导入Button类：
```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
```

```kotlin
val frame = Frame()
frame.layout {
    draw(Button()) { button ->
        // 设置Button的布局
        layout {
            // 从上往下空出10%像素
            x = 10.percent
            // 从左往右空出50%像素
            y = 50.percent
            // 80%窗口横向像素的大小
            width = 80.percent
            // 10%窗口垂直像素的大小
            height = 10.percent
        }
        // 设置Button的样式
        style {
            background {
                // 设置背景色调
                color = Color.web("#4c8bf5")
                // 设置背景图，设置后背景配置将不可用
                image = "file:/picture/bg.png"
            }
            // 设置圆角
            corner = 35.percent
            // 设置前景色调
            foreground = Color.web("#000000")
        }
    }
}
frame.show()
```

## 内边距（padding）

内边距是``layout``块里的两个值：``hPadding``（左右）和``vPadding``（上下）。它和``x``、``width``写在同一个地方，即组件自己的``layout``块里：

```kotlin
val frame = Frame()
frame.layout {
    draw(Div()) { panel ->
        layout {
            width = 80.percent
            height = 50.percent
            hPadding = 24.px
            vPadding = 16.px
        }
        panel.layout {
            draw(Button()) { button ->
                layout {
                    // 内容是往内缩的：x = 0 落在左内边距上，而不是 panel 最左边
                    x = 0.px
                    y = 0.px
                    // 100% 是内缩之后剩下的宽度，所以右边不会溢出
                    width = 100.percent
                    height = 20.percent
                }
            }
        }
    }
}
```

内边距是**内容内缩**：

* 左右各缩``hPadding``，上下各缩``vPadding``，可用区域相应变小；
* 子组件的``x``/``y``从内缩后的盒子左上角开始计算；
* 百分比按内缩后的盒子解析，所以``100.percent``正好停在内边距上；
* 窗口、``Div``和``FlexBox``都支持；内边距只影响它自己的子组件，不影响外面；
* 内边距只做内缩：它不会把子组件之间推开，``FlexBox``的间距由``gap``决定（见下一节）；
* 内边距超过自身宽度（或高度）的一半时，会被截到一半，内容区压成零而不是变成负数；
* 写在哪个组件的``layout``里，就只决定那个组件的子组件怎么摆，位置和大小照旧。

窗口本身不是谁的子组件，所以它的内边距直接写在窗口上：

```kotlin
frame.applyLayout {
    hPadding = 24.px
    vPadding = 16.px
}
```
