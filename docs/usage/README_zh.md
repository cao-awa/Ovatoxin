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
import com.github.kusa233.ovatoxin.widget.Button
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