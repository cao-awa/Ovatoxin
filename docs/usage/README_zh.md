# 窗口
[English](README.md) | [中文](README_zh.md)

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

它也没有可以拖动窗口的标题栏。可以让标签条承担这个角色：在``TabPane``（或独立的``TabBar``）上设置``windowDragEnabled = true``，之后在标签条的空白背景上按下并拖动即可移动窗口，标签按钮和滚动条仍保持原本的行为：

```kotlin
frame.layout {
    draw(TabPane()) { pane ->
        pane.windowDragEnabled = true
        // ...
    }
}
```

## 尺寸限制（minWidth / minHeight / maxWidth / maxHeight）

用``minWidth``、``minHeight``、``maxWidth``和``maxHeight``限制用户缩放窗口时的尺寸范围。数值单位为像素，既可以在构造时传入，也可以在创建后设置：

```kotlin
val frame = Frame(
    minWidth = 400.0,
    minHeight = 300.0,
    maxWidth = 1200.0,
    maxHeight = 900.0,
)

// 构造之后也可以修改：
frame.maxWidth = 1000.0
```

最小值默认是``0``，最大值默认是``Double.MAX_VALUE``（不限制）。这些限制对带标题栏的窗口和无边框窗口的自定义缩放手柄都生效。

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
                // 设置背景图，设置后背景色调配置将不可用
                image = "file:/picture/bg.png"
            }
            border {
                // 设置描边粗细和颜色
                size = 1.px
                color = Color.web("#1d4ed8")
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
```

```kotlin
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
                    button.text("$index")
                }
            }
        }
    }
}
```

模板是**快照**：应用时会把模板的值整份复制进组件（``style``连背景和边框一起复制），因此

* 之后再改模板，不会影响已经用过它的组件；
* 多个组件共用一个模板，彼此独立——改其中一个的背景，不会连带改到别的组件，也不会改到模板本身。

``layout(...)``/``style(...)``复制的是模板的**全部**槽位：模板里没写的槽位取文档默认值，所以应用模板会把组件原有的值一并覆盖。逐组件不同的地方，写在后跟的那个块里即可。

## 边框（border）

``border { }``块用来给组件画一圈描边：

```kotlin
style {
    border {
        size = 2.px
        color = Color.web("#1d4ed8")
    }
}
```

* ``size``是长度。``px``是绝对像素；``percent``按盒子的**短边**计算，和``corner``一致；``em``跟随组件字号。负数按零处理；
* ``color``是描边颜色，默认是黑色，但``size``为零时不会绘制。想去掉模板继承来的边框，可以写``color = null``或``size = 0.px``；
* 描边会跟随``corner``，画在盒子内侧，所以圆角填充也能得到匹配的圆角边框；
* ``reset()``会移除边框，样式模板也会连同边框一起复制。

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
    button.text("Ok")
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

## 字重（fontWeight）

``style``块里可以写``fontWeight``，直接使用 JavaFX 的``FontWeight``：

```kotlin
import javafx.scene.text.FontWeight
```

```kotlin
draw(Button()) { button ->
    style {
        fontWeight = FontWeight.BOLD
    }
    button.text("保存")
}
```

``fontWeight``可选的值就是``FontWeight``的枚举：``THIN``、``EXTRA_LIGHT``、``LIGHT``、``NORMAL``、``MEDIUM``、``SEMI_BOLD``、``BOLD``、``EXTRA_BOLD``、``BLACK``。

规则：

* 不写``fontWeight``（默认``null``）时保持控件原本的字重，不改变原生外观；
* 按钮、输入框、标签、链接等控件会把字重写进``-fx-font-weight``，按``FontWeight``对应的数值（如``BOLD``是``700``）生效；
* 自带文字的``TextArea``在构造自己的``Font``时会使用这个字重；它原有的``bold``属性仍然可用，写了``fontWeight``时以``fontWeight``为准；
* 某个字重能否真正画出区别，取决于所选的字体家族是否提供该字重；系统默认字体可能把``SEMI_BOLD``之类回退成常规，换一个提供多字重的``fontFamily``才会看到差异；
* 和``foreground``一样，自身没有文字的``Div``、``Frame``会忽略它；字重不会像``fontSize``那样被子树继承；
* ``fontWeight``存在``style``里，重绘时``reset()``会把它清掉，想每次都生效就在每次运行的``style``块里再写一次。

字重（和字号一样）不参与``transition``的插值：过渡只平滑背景色、前景色、圆角以及边框粗细和颜色，改``fontWeight``时字重立即切换。

## 动画过渡（transition）

``style``块里可以写一个``transition``，让样式**变化**时平滑过渡，而不是一下子跳过去：

```kotlin
val selected = State(true)
draw(Button()) { button ->
    style {
        background { color = if (selected.value) Color.WHITE else Color.web("#4c8bf5") }
        foreground = if (selected.value) Color.web("#4c8bf5") else Color.WHITE
        transition = 200.milliseconds
    }
    button.onClick { selected.update(!selected.value) }
}.withState(selected)
```

``transition``的单位是 Kotlin 标准库的``Duration``，所以直接写``200.milliseconds``；想用别的单位也可以写成``1.seconds``、``500.microseconds``等，取的是``kotlin.time.Duration``里的写法。

规则：

* **第一次**拿到的样式立刻生效，``transition``只负责它之后的**变化**；
* 被插值的是``background``（颜色和``opacity``一起）、``foreground``、``corner``以及``border``（粗细和颜色一起），所以颜色、透明度、圆角和边框粗细都会随帧过渡；
* ``background``设了``image``时图片不参与插值，图片本身直接切换，颜色层照常淡入淡出；
* ``Duration.ZERO``和默认的``transition = null``一样，都是立即生效、不做动画；负数或``Duration.INFINITE``会直接报错；
* ``transition``存在``style``里，和别的样式值一样会在重绘时被``reset()``清掉；想让重绘后继续有动画，就在每次都会运行的``style``块里再写一次；
* 动画需要 JavaFX 的线程，组件已经显示出来后由布局过程自动驱动，不需要手动调用。

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
