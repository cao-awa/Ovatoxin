# 组件

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
* ``justify``只在有空位时起作用，且永远不改子组件的大小：
  ``START``靠左、``CENTER``居中、``END``靠右、``SPACE_BETWEEN``首尾贴边、``SPACE_AROUND``每边各留半个份额；
* ``justify``分出的空位是**叠加**在``gap``上的，所以``SPACE_BETWEEN``的实际间距是``gap``加上那一份；
  只有一个子组件的行里，``SPACE_BETWEEN``等于``START``，``SPACE_AROUND``等于居中；
* 一行排不下时有子组件溢出，此时没有空位可分，整行退化成``START``——子组件永远不会被压窄；
* ``align``只在一行里比较：行高仍是该行最高的子组件，``CENTER``把它上下各让一半，``END``贴着行的底部；
  行与行之间照旧依次向下排，与``align``无关；
* 百分比宽度按内缩后的宽度解析，比例为``100.percent``的子组件会独占一行。