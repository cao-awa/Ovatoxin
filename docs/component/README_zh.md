# 组件
[English](README.md) | [中文](README_zh.md)

## Label

``Label``是表单标签：一小段文字，用来标注旁边那个控件是什么。它自己不带内容，内容是它标的那一项的。

它的特别之处不在于文字，而在于**它和控件的关联**：``labelFor``指向谁，点这段文字就会把焦点交给谁，读屏软件也会把两者一起念出来。所以``Label("用户名", labelFor = nameField)``旁边的输入框不必再把标题写进占位符。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.input.TextField
import com.github.kusa233.ovatoxin.widget.text.Label
```

```kotlin
frame.layout {
    // 先画出控件，好把它的引用交给标签
    val name = draw(TextField()) { field ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 45.percent
            height = 6.percent
        }
        field.hint = "Your name"
    }
    // 这个标签标注的就是上面那个输入框
    draw(Label("User name", labelFor = name)) { label ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 45.percent
            height = 6.percent
        }
        // 标签的文字走前景色，和其它文字组件一样
        style {
            foreground = Color.web("#4c8bf5")
        }
    }
}
```

规则：

* 标签只负责一段短文字，不摆放任何东西，位置和大小照旧由``layout``决定；
* ``labelFor``可以先写在构造里，也可以之后再挂：``label.labelFor = name``和``label.labelFor(name)``等价，``label.labelFor``读回来的就是当前标的那一项；
* 顺序不重要：控件还没渲染时关联会先记下来，等标签真正建出节点时再补上，所以标签画在控件前面也可以；
* ``labelFor``传``null``就断开关联，标签退化成一截普通文字；
* ``text``可以随时改：``label.text = "账户"``，已经建出的节点会立刻跟着变；
* ``reset()``回到创建时的样子——文字和关联都回到构造里给的那一对，而不是清成空文字；
* 要一段自己承载内容的只读文字（段落、译文）请用``TextArea``，标签只是旁边那一项的短名字。

## FlexBox

``FlexBox``是一个会自己摆放子组件的画布：从左往右排，排满就换到下一行，行高取该行最高的子组件。
``FlexBox``同样可以``draw``，也可以放在``Div``里。

它自己有三个属性，都直接写在``FlexBox``上，不写在``layout``块里：

* ``gap``——子组件之间的间距，同时也是行与行之间的间距，默认``0.px``；
* ``justify``——一行排完之后剩下的空位怎么分，默认``Justify.START``；
* ``align``——子组件在自己那一行里靠上、居中还是靠下，默认``Align.START``。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.Align
import com.github.kusa233.ovatoxin.widget.container.FlexBox
import com.github.kusa233.ovatoxin.widget.container.Justify
```

```kotlin
frame.layout {
    draw(FlexBox()) { row ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 80.percent
            height = 40.percent
            // 内边距只是内缩，和子组件之间的间距无关
            hPadding = 8.px
            vPadding = 8.px
        }
        // 间距和分布写在 FlexBox 自己身上
        row.gap = 8.px
        row.justify = Justify.SPACE_BETWEEN
        row.align = Align.CENTER
        row.layout {
            for (index in 1..12) {
                draw(Button()) { cell ->
                    layout {
                        // 只给大小，位置交给 FlexBox
                        width = 18.percent
                        height = 30.px
                    }
                    cell.text = "$index"
                }
            }
        }
    }
}
```

规则：

* 不加``x``/``y``时，位置由流动排出；
* 写了``x``/``y``也只当作偏移，叠加在``FlexBox``算出的位置上；
* ``hPadding``/``vPadding``只做内缩：第一行的起点是内边距，``justify``分空位时也不会越过内边距；
* ``gap``是唯一决定间距的值，横向和纵向都用它；按百分比给时，横向按内容区宽度解析，纵向按内容区高度解析；
* ``gap``和``hPadding``/``vPadding``都可以写成``em``：此时按``FlexBox``**自己**的字号解析，
  所以``row``的``style``里写了``fontSize = 20.px``之后``row.gap = 1.em``就是 20 像素——整块面板的字号一起改，间距跟着走；
* ``justify``只在有空位时起作用，且永远不改子组件的大小：
  ``START``靠左、``CENTER``居中、``END``靠右、``SPACE_BETWEEN``首尾贴边、``SPACE_AROUND``每边各留半个份额；
* ``justify``分出的空位是**叠加**在``gap``上的，所以``SPACE_BETWEEN``的实际间距是``gap``加上那一份；
  只有一个子组件的行里，``SPACE_BETWEEN``等于``START``，``SPACE_AROUND``等于居中；
* 一行排不下时有子组件溢出，此时没有空位可分，整行退化成``START``——子组件永远不会被压窄；
* ``align``只在一行里比较：行高仍是该行最高的子组件，``CENTER``把它上下各让一半，``END``贴着行的底部；
  行与行之间照旧依次向下排，与``align``无关；
* 百分比宽度按内缩后的宽度解析，比例为``100.percent``的子组件会独占一行；
* 子组件的``width``/``height``写``em``时，按**子组件自己**的字号解析（通常是继承来的那个），
  因为它量的是自己的大小，不是``FlexBox``的。

## TabBar 与 TabPane

``TabBar``是一个装满标签按钮的``FlexBox``。``TabPane``把``TabBar``放在自己的四条边之一，并显示当前选中标签所绑定的组件；绑定在其他标签上的组件全部隐藏。

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
import com.github.kusa233.ovatoxin.widget.container.LEFT
import com.github.kusa233.ovatoxin.widget.container.TabPane
import com.github.kusa233.ovatoxin.widget.text.TextArea
```

```kotlin
val pane = TabPane()
pane.side = LEFT

val one = pane.add(Button("第一页"))
pane.bind(one, TextArea("第一页内容"))

val two = pane.add(Button("第二页"))
pane.bind(two, TextArea()) {
    // 代码块在 DrawScope 中运行，参数就是绑定的组件。
    // 页面自动填满内容区；x 和 y 不起作用。
    it.text = "第二页内容"
}
```

规则：

* ``side``取``UP``、``BOTTOM``、``LEFT``或``RIGHT``；``pane.side``和``pane.tab.side``都能设置。``UP``/``BOTTOM``是横条，``LEFT``/``RIGHT``是竖条；
* ``tab.add(button) { ... }``追加一个按钮并选中它，``tab.remove(button)``删除它；在``TabPane``上直接调用``add``/``remove``效果相同；
* 标签按钮的``x``和``y``不起作用——位置由``TabBar``决定；``width``/``height``仍会保留；
* ``bind(button, component) { ... }``把内容绑定到``button``指定的标签并返回该组件。代码块类型是``DrawScope<T>.(T) -> Unit``：可以从接收者使用``layout { }``/``style { }``，组件的自身 API 则通过其参数访问。该按钮必须已经属于这个标签条。同一个标签可以绑定多个组件，它们会一起显示；
* 绑定组件在``bind``时立即绘制，并会在状态驱动的重绘中重放它的代码块。它默认填满自己的页面（宽高都是``100.percent``），这里的百分比按减去``TabBar``后剩下的内容区域解析，而不是整个``TabPane``；其``x``/``y``不起作用，调用者写出的宽或高会保留；
* ``remove(button)``会一并删除绑定在该标签上的组件。删除当前选中的标签后，会选中剩下的第一个标签；
* ``barSize``设置标签条在所占据方向上的尺寸：``UP``/``BOTTOM``时是高度，``LEFT``/``RIGHT``时是宽度。默认``36.px``，也可以通过``TabPane(barSize = ...)``构造参数传入；``tabBarThickness``保留为它的别名；
* 标签按钮会填满标签条的厚度方向：``UP``/``BOTTOM``时每个按钮的``height = 100.percent``，``LEFT``/``RIGHT``时每个按钮的``width = 100.percent``；另一方向保留按钮自身设置的尺寸。水平的标签条默认``justify = Justify.CENTER``，需要时可以由调用方自行设置``justify``覆盖；
* 设置``TabPane``的样式时，底层``TabBar``也会应用同样的样式：每次布局都会把背景、圆角、文字颜色和``transition``复制到``tab``，所以标签条与整个窗格外观一致。标签按钮在没有自行设置背景时不会涂自己的颜色，未选中的标签会透出窗格背景，而不是被控件默认的白色盖住。``selectColor``只给选中的标签按钮涂上该背景色；``null``（默认值）表示不绘制选中差异。``pane.selectColor``与``pane.tab.selectColor``是同一个属性；
* 写在``TabPane``样式里的``transition``也会传给标签按钮，所以切换标签时``selectColor``的变化会有过渡动画。若某个标签按钮自己设置了``transition``，则以按钮自己的为准。