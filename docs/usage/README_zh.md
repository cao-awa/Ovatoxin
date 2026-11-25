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

## 关闭订阅（onClose）

用``.onClose { }``订阅“用户请求关闭窗口”的事件（点击标题栏的关闭按钮或平台等价操作）。处理块里的``this``就是这个窗口，标题、宽高和组件树都还能读取，适合保存状态、做确认提示等：

```kotlin
frame.onClose {
    println("Closing '${this.title}'")
}
```

可以订阅多次，每个处理块都会按注册顺序执行。需要注意：``onClose``只是**观察**关闭请求，不能阻止窗口关闭；而代码里直接调用``frame.close()``也不会触发它。

## 标题栏（titleBarEnabled）

JavaFX 在窗口第一次显示时就固定了窗口装饰，之后无法再切换标题栏。所以在构造时传入``titleBarEnabled``决定是否显示标题栏：

```kotlin
// 无标题栏（无边框）窗口
val frame = Frame("Ovatoxin - demo", titleBarEnabled = false)
```

无标题栏的窗口没有系统关闭按钮，需要自己提供关闭方式（例如自定义的关闭按钮）。

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
            // 设置字号，下面用 em 写的长度都按这个数解析
            fontSize = 16.px
        }
    }
}
frame.show()
```

## 模板预设（layout / style）

直接写在组件里的``layout { }``和``style { }``只作用于那一个组件。如果一组位置、尺寸或外观要在多处复用，可以在任何组件之外用同名的顶层函数``layout { }``、``style { }``先做出一个**模板**，它返回一个普通的``Layout``/``Style``值：

```kotlin
import com.github.kusa233.ovatoxin.dsl.layout
import com.github.kusa233.ovatoxin.dsl.style

// 五分之一宽、占满高度的按钮
val fifth = layout {
    width = 20.percent
    height = 100.percent
}

// 一张蓝色卡片
val card = style {
    background { color = Color.web("#4c8bf5") }
    corner = 30.percent
}
```

应用模板时，把模板作为第一个参数传给``layout(...)``/``style(...)``，后面可以再跟一个块做**逐组件微调**：

```kotlin
frame.layout {
    draw(FlexBox()) { box ->
        box.layout {
            for (index in 1..4) {
                draw(Button()) { button ->
                    layout(fifth) { /* 只调这一个组件 */ }
                    style(card) { /* 只调这一个组件 */ }
                    button.text = "$index"
                }
            }
        }
    }
}
```

模板是**快照**：应用时会把模板的值整份复制进组件（``style``连背景一起复制），因此

* 之后再改模板，不会影响已经用过它的组件；
* 多个组件共用一个模板，彼此独立——改其中一个的背景，不会连带改到别的组件，也不会改到模板本身。

``layout(...)``/``style(...)``复制的是模板的**全部**槽位：模板里没写的槽位取文档默认值，所以应用模板会把组件原有的值一并覆盖。逐组件不同的地方，写在后跟的那个块里即可。

## 长度单位（percent / px / em）

``x``、``y``、``width``、``height``、``hPadding``、``vPadding``都是长度，可以写成三种单位之一：

* ``percent``——父容器的百分比：``80.percent``是父容器宽度的八成（``10.p``是简写）；
* ``px``——设备无关像素：``16.px``。用来写不该跟着窗口变的东西，细线、图标、固定侧栏；
* ``em``——组件自己字号的一倍：``1.em``就是一行字的高度。

```kotlin
layout {
    x = 10.percent
    y = 1.em
    width = 16.px
}
```

``em``的好处是它跟着字号走：字号变大，用``em``写的内边距、间距、圆角一起变大，而用``px``写的不会动。下面这块面板的内边距永远是一行字那么宽、半行字那么高，字号改成多少都成立：

```kotlin
draw(Div()) { panel ->
    layout {
        hPadding = 1.em
        vPadding = 0.5.em
    }
    // 字号变了，上面那行 em 的量就跟着变
    style { fontSize = 20.px }
}
```

三种单位各算各的，只有同一种单位之间才能加减、缩放和比较：``10.percent + 4.px``编译不过，因为只有父容器的大小和字号才能决定两者怎么相加。真要混着用时，先用``10.percent.toPixels(width)``把相对的那一边冻成像素。

字号``fontSize``也是一个长度，但只用得上``px``和``percent``两种——``em``本身要拿字号来算，不能再拿来写字号，写了会报错，详见下面的“字号”一节。

## 字号（fontSize）

``em``是对**组件自己的字号**。组件可以在``style``块里写一个``fontSize``，它下面的整棵子树都继承这个字号：

```kotlin
draw(Div()) { panel ->
    style { fontSize = 20.px }
    panel.layout {
        // 这里所有 em 都按 20px 解析
        draw(Button()) { button ->
            style { fontSize = 16.px }   // 这一支改用 16px，不再跟外面的 20px
            layout {
                width = 6.em
                height = 2.em
            }
        }
    }
}
```

``fontSize``是一个长度，但只有两种单位有意义：

* ``px``——设备无关像素，绝对大小：``fontSize = 16.px``；
* ``percent``——**组件自己盒子高度**的比例：几十像素高的按钮写``fontSize = 50.percent``，字号就是它高度的一半，字跟着盒子一起缩放；
* ``em``——**不允许**。``em``本身就是“字号的多少倍”，拿它去写字号等于让字号按自己算，所以写``fontSize = 1.em``会直接抛``IllegalArgumentException``。

```kotlin
style { fontSize = 20.px }        // 二十像素
style { fontSize = 50.percent }   // 组件自己高度的 50%
style { fontSize = 1.em }         // 报错：em 没有字号可以参照
```

``percent``要拿盒子高度，而高度在布局之前还不知道；更要紧的是，高度写成``auto``或``em``的组件会跟着字号一起长，字号和盒子互相追。所以百分比**只在该组件的高度是``px``或``percent``时生效**，高度是``auto``或``em``时退回继承来的字号。想让百分比字号有意义，就给组件写一个明确的``height``：

```kotlin
draw(Button()) { button ->
    layout { height = 40.px }
    style { fontSize = 50.percent }   // 四十像素高的按钮，字号二十像素
    button.text = "Ok"
}
```

规则：

* ``fontSize``写成``px``或``percent``；
* 不写就继承父组件的字号，一直往上找；整棵树都没写时用库默认的``14.0``；
* ``percent``按组件**自己盒子的高度**算，所以``50.percent``是“字占盒子一半高”，盒子变高字就变大，和周围文字无关；
* 算出来的字号不只用于``em``的解析，还会真正设到控件的文字上（按钮、输入框、标签等的``-fx-font-size``），所以屏幕上的字确实跟着盒子缩放；
* 自带文字的组件（``TextArea``）用自己的文字大小，不再往上看；它的字号同样存在``style``里，所以``style { fontSize = 24.px }``也能改它画出来的字；
* 改动``fontSize``会重新摆放子树，已经写好的``em``立刻按新字号解析；
* 一次重绘（``reset()``）会丢掉这一轮没再设的``fontSize``，回到继承。

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
* 没有子组件的叶子组件（``Button``、``TextField`` 等）则把内边距作用到自己身上：左右各内缩``hPadding``；上下则**先尝试挪动**——盒子保持自己的高度，在流为它留下的那一行（band）里上下居中，于是上下留白相等——只有当“高度 + 2×``vPadding``”超过那一行时，才把盒子压小，上下各留``vPadding``的空白。例如让按钮在``FlexBox``里上下各露出等宽的两条背景色；
* 内边距只做内缩/位移：它不会把子组件之间推开，``FlexBox``的间距由``gap``决定（见下一节）；
* 内边距超过自身宽度（或高度）的一半时，会被截到一半，内容区压成零而不是变成负数；
* 写在容器上时，它只决定那个容器里子组件怎么摆，容器自身的位置和大小照旧；写在叶子组件上时，左右内缩叶子自己的盒子、上下按上面说的先居中、放不下才压缩。

窗口本身不是谁的子组件，所以它的内边距直接写在窗口上：

```kotlin
frame.applyLayout {
    hPadding = 24.px
    vPadding = 16.px
}
```
