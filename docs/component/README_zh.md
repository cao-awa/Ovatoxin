# 组件
[English](README.md) | [中文](README_zh.md)

## 目录

* 文字与展示：[Label](#label)、[TextArea](#textarea)、[Link](#link)
* 操作与输入：[Button](#button)、[TextField](#textfield)、[CheckBox](#checkbox)、[ComboBox](#combobox)
* 列表与进度：[ListView](#listview)、[ProgressBar](#progressbar)
* 布局：[Div](#div)、[FlexBox](#flexbox)、[TabBar 与 TabPane](#tabbar-与-tabpane)

## 组件通用行为

本章的每个组件都是``Component``，因此下面这些规则处处适用，后面各节不再重复。

* ``layout { ... }``描述组件自身的几何（``x``、``y``、``width``、``height``、内边距等），``style { ... }``描述它的填充、圆角、前景色、字体和过渡；两者详见[用法](../usage/README_zh.md)；
* ``onClick { ... }``注册点击处理器，并替换之前注册的那个。块里的``this``就是组件本身，所以``button.onClick { println(this.text) }``可以直接读组件自己的属性；
* ``visible = false``会隐藏已经建好的组件，默认是``true``；
* ``withState(state)``把组件绑定到一个[State](../usage/README_zh.md)：状态变化时重绘该组件，并且只重绘该组件；
* 每次绘制块重新执行之前，``reset()``会把组件恢复到**创建时**的值——标题、URL、勾选状态、几何、样式以及上一轮设置过的处理器都会被还原；它不会改变组件的身份、节点或子组件，也会刻意保留``ListView.items``、``ComboBox.items``这类实时集合；
* 没有自己样式的组件起始为白色填充（``Style.DEFAULT_COLOR``），在``style { foreground = ... }``设置之前文字使用工具包默认的深色；[Div](#div)按设计透明，[Link](#link)默认是蓝色。

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

## TextArea

``TextArea``是自带内容的只读文字：一段话、一条消息或一段译文。它不是编辑器——需要可编辑的单行输入请用[TextField](#textfield)。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.text.TextArea
import javafx.scene.text.TextAlignment
```

```kotlin
frame.layout {
    draw(TextArea("属于页面本身的一段话。")) { text ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 80.percent
            height = 30.percent
        }
        text.fontSize = 16.0
        text.bold = true
        text.align = TextAlignment.CENTER
        text.wrap = true
        style {
            foreground = Color.web("#333333")
        }
    }
}
```

规则：

* ``TextArea("hello")``以这段文字起始；``text``读取或替换内容，``text.text("new")``和``text.text = "new"``等价；
* ``fontSize``的单位是点，默认``14``。它存在组件的样式里，所以``text.fontSize = 20.0``和``style { fontSize = 20.px }``表达同一件事；百分比字号按该文本区自身的高度计算，``em``字号会被拒绝，因为它等于用字号本身来度量字号；
* 和其它大多数组件不同，文本区**不会继承**所在面板的字号：没有自己字号的文本区使用``14``点。如果希望它随盒子缩放，请给它高度并使用百分比字号；
* ``bold``选择粗体，``fontFamily``选择字体；样式里写的``fontWeight``会覆盖``bold``；
* ``align``是 JavaFX 的``TextAlignment``（``LEFT``、``CENTER``、``RIGHT``或``JUSTIFY``），默认``LEFT``；``wrap``默认``false``，不开启时长行会直接超出盒子；
* 文字颜色和其它文字组件一样来自``style { foreground = ... }``；
* ``reset()``恢复构造时的文字、``14``点、默认字体、``LEFT``对齐和``wrap = false``，并丢掉上一轮设置的样式/处理器。所以``TextArea("hello")``重置回``"hello"``，不是空文字。

## Link

``Link``是表现成超链接的文字：带下划线、悬停时是手形光标，点击后在系统浏览器中打开一个 URL。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.text.Link
```

```kotlin
frame.layout {
    draw(Link("Ovatoxin", "https://github.com/cao-awa/Ovatoxin")) { link ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 40.percent
            height = 6.percent
        }
    }
}
```

规则：

* ``text``是可见文字，``url``是点击要打开的地址，两者都可以随时修改；
* 没有``onClick``处理器的链接，点击会用操作系统默认浏览器打开``url``；``url``为空时点击什么也不做；
* 注册``onClick``会**替换**默认的浏览器行为：带处理器的链接只执行处理器，不打开任何东西。处理器以链接为``this``，所以里面仍可读``this.url``；
* ``open()``显式执行浏览器动作，适合自定义处理器还想保留打开功能的场合；
* 文字带下划线，光标为手形。默认前景色是``#4c8bf5``，``style { foreground = ... }``可以覆盖；
* ``reset()``恢复构造时的文字、URL 和默认蓝色。

## Button

``Button``是按下时执行处理器的按钮。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.action.Button
```

```kotlin
frame.layout {
    draw(Button("Save")) { button ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 25.percent
            height = 8.percent
        }
        button.onClick {
            println("saving ${this.text}")
        }
        button.enabled = true
    }
}
```

规则：

* ``Button("Save")``以该标题起始；``text``读取或替换它，``button.text("Save")``与属性写法等价；
* ``enabled``默认``true``。禁用的按钮忽略点击，也不执行处理器；
* ``onClick { ... }``是唯一的点击处理器，再次注册会替换前一个，块里的``this``是按钮。这个通用处理器对所有组件都可用；
* 外观以扁平化为主：样式层去掉 JavaFX 自带的边框、渐变和焦点框，再涂上配置的背景、可选边框和前景色。没有样式的按钮是白底深色文字的矩形；
* ``reset()``恢复构造时的标题、重新启用按钮，并移除上一轮绘制设置的处理器。``Button("Save")``因此重置回``"Save"``。

## TextField

``TextField``是可编辑的单行文本框。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.input.TextField
```

```kotlin
frame.layout {
    draw(TextField()) { input ->
        layout {
            x = 20.percent
            y = 20.percent
            width = 60.percent
            height = 8.percent
        }
        input.hint = "Type your name"
        input.onChange { value -> println("now: $value") }
        input.onSubmit { value -> println("submitted: $value") }
    }
}
```

规则：

* ``TextField("initial")``以该内容起始；``text``读取或替换它，``input.text("new")``与属性写法等价；
* ``hint``是输入框为空时的占位提示，默认空字符串；``editable``默认``true``，设成``false``让输入框只读；
* ``onChange { ... }``在用户编辑时收到新内容，**程序写入``text``时也会触发**——程序写入和编辑一样会被报告。再次注册会替换前一个处理器；
* ``onSubmit { ... }``在用户按``Enter``时执行；``performSubmit()``可以手动触发同一个处理器，主要用于测试；
* 选区、输入法、剪贴板等行为来自 JavaFX 原生文本框；样式层负责填充和文字颜色；
* ``reset()``恢复构造时的内容、清空提示、重新设为可编辑，并丢掉两个处理器。重置是直接赋值，所以被丢掉的``onChange``不会为它触发。

## CheckBox

``CheckBox``是右边带标题、可以勾选或取消勾选的方框。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.input.CheckBox
```

```kotlin
frame.layout {
    draw(CheckBox("Remember me")) { box ->
        layout {
            x = 10.percent
            y = 70.percent
            width = 80.percent
            height = 8.percent
        }
        box.checked = true
        box.onChange { checked -> println("checked: $checked") }
    }
}
```

规则：

* ``checked``初始为``false``，可以随时读写；构造函数只提供标题，所以勾选状态要通过属性设置；
* ``onChange { checked -> ... }``收到新的布尔状态，是复选框最合适的处理器。``onClick { ... }``同样可用，并且在两者都注册时于同一次点击中一起执行；
* 程序写入的勾选是**静默**的：``box.checked = true``只改变画面，不执行任何处理器。只有用户点击才会到达处理器，这样处理器里再写``checked``也不会自己调用自己；
* ``enabled``默认``true``；禁用的复选框不报告任何操作；
* ``reset()``恢复构造时的标题和未勾选状态、重新启用，并移除处理器。组件样式表会把 JavaFX 的皮肤扁平化；
* 每个处理器各只有一个，再次注册会替换；这与直接使用 JavaFX CheckBox 时监听器会叠加不同。

## ComboBox

``ComboBox<T>``在一个字段里显示当前项，点击时下拉其余项。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.input.ComboBox
```

```kotlin
frame.layout {
    draw(ComboBox(listOf("Small", "Medium", "Large"))) { combo ->
        layout {
            x = 10.percent
            y = 40.percent
            width = 40.percent
            height = 8.percent
        }
        combo.selectedIndex = 1
        combo.onSelectItem { size -> println("size: $size") }
    }
}
```

如果元素类型没有合适的``toString``，把格式化函数作为第二个构造参数：

```kotlin
data class Language(val code: String, val name: String)

draw(ComboBox(languages) { it.name }) { combo ->
    combo.prompt = "Choose a language"
}
```

规则：

* ``items``是**实时**的``MutableList``：``combo.items.add(...)``、``removeAt``、``clear``以及其它``MutableList``操作都会立刻更新下拉框；``combo.items(list)``一次性替换全部项；
* 选择会跟随它指向的那一项穿过各种增删：在选中项上方插入会让选中索引下移，删掉选中项则清空选择。用``items(value)``整体替换时，只有同一个元素仍存在才保留选择；
* ``itemText``同时决定字段里的文字和每一行下拉项的文字，默认格式化函数是``toString()``；
* ``selectedIndex``是当前选中索引，未选中时为``ComboBox.NO_SELECTION``（``-1``），新建的组合框一开始未选中；``selectedItem``和``selectedText``读取选中项及其格式化文字，``size``和``item(index)``用来查看项；
* ``select(index)``是程序路径，保持静默；``clearSelection()``清空选择。除``NO_SELECTION``外，索引越界会抛异常。``onSelect { index -> ... }``和``onSelectItem { item -> ... }``只在用户实际选择时执行，两者可以同时注册；
* ``prompt``在未选中时显示，默认空字符串；``enabled``默认``true``，控制下拉框能否打开；
* 组合框默认是白底黑字，不受 JavaFX 默认样式影响；``reset()``清空选择与提示、重新启用、丢掉处理器并重置这些颜色，但**保留现有项**。要回到构造时的项，用``combo.items(combo.initialItems)``。

## ListView

``ListView<T>``是纵向、可选中的列表。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.list.ListView
```

```kotlin
frame.layout {
    draw(ListView(listOf("Alpha", "Beta", "Gamma"))) { list ->
        layout {
            x = 10.percent
            y = 10.percent
            width = 40.percent
            height = 60.percent
        }
        list.onSelectItem { item -> println("picked: $item") }
    }
}
```

没有合适``toString``的元素类型可以传入格式化函数：

```kotlin
data class User(val name: String)

draw(ListView(users) { it.name }) { list ->
    list.selectedBackground = Color.web("#dbeafe")
}
```

规则：

* ``items``是实时``MutableList``。``add``、``addAll``、``remove``、``removeAt``和``clear``也直接挂在列表上，是同一批操作的简写；每次修改都会在调用返回前更新行；
* 选择跟随选中的**元素**穿过增删：在它上方插入行会移动选择，删除选中行会清空选择。``list.items(otherList)``一次性替换所有行，只有同一个元素仍在时才保留选择；
* ``selectedIndex``是选中行，未选中时为``ListView.NO_SELECTION``（``-1``），新建列表一开始无选中；``selectedItem``和``selectedText``读取它；``select(index)``和``clearSelection()``是程序路径，不执行处理器；
* ``onSelect { index -> ... }``和``onSelectItem { item -> ... }``只在用户点击行（主键）时执行，两者可以同时注册。用户点击会先选中该行，再执行处理器；
* ``selectedBackground``默认``#eeeeee``。列表本身起始为白底黑字，文字颜色也会涂到每一行；
* 行放在滚动面板里，行数超过可见范围时才出现纵向滚动条。行总是撑满列表宽度，所以选中背景看起来是一条整行的高亮；
* ``reset()``清空选择、``selectedBackground``覆盖和两个处理器，并重置默认颜色，但**保留当前行**。要回到构造时的行，用``list.items(list.initialItems)``；
* 绘制块每次重绘都会重跑，所以块里无条件写``list.items.add(...)``会在每轮追加一次；请用描述该变化的状态值做条件保护；
* ``size``和``item(index)``用来查看行，``removeAt``/``item``在索引越界时会抛异常。

## ProgressBar

``ProgressBar``是横向填充的进度指示条。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.progress.ProgressBar
```

```kotlin
val progress = State(40.percent)

frame.layout {
    draw(ProgressBar(progress.value)) { bar ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 80.percent
            height = 6.px
        }
        style {
            corner = 3.px
            background {
                color = Color.web("#dbeafe")
            }
        }
        bar.progress = progress.value
    }.withState(progress)
}
```

规则：

* ``ProgressBar(0.4)``接收``0.0``到``1.0``的比例；``ProgressBar(40.percent)``接收同样满度的百分比。``ProgressBar()``从空开始；
* ``progress()``读取比例；``progress(0.4)``和``progress(40.percent)``设置进度；``progress``属性用``Percent``表达，所以``bar.progress = 40.percent``是属性写法，``bar.progress >= 100.percent``是循环判断完成的方式。像素不是比例，写成进度根本编译不过；
* 越界的值会被夹到范围内而不是拒绝，这适合循环偶尔多报的情况：``1.2``显示满条，``-1.0``显示空条。``NaN``按空处理；
* 没有百分比标签，也没有内置动画。像示例那样用[State](../usage/README_zh.md)驱动进度条，或用定时器更新状态；进度条本身只显示收到的值；
* 非零``corner``会圆角化轨道，并把填充带裁剪到同一轮廓。默认填充是组件白色，所以要让指示条可读，通常要设置背景和圆角；
* 填充带由组件样式表画成单一纯色（``#0096c9``），目前没有重新着色的内置属性；样式里的``background``画的是空白轨道，不是填充带；
* ``reset()``恢复创建时的进度。

## Div

``Div``是给窗口划出一块区域的普通盒子。它是绘制面而不是控件：除非样式给它填充，否则自己没有背景；但它会为内容**重定百分比基准**，并**裁剪**超出边界的内容。

导入：

```kotlin
import com.github.kusa233.ovatoxin.widget.container.Div
import com.github.kusa233.ovatoxin.widget.text.TextArea
```

```kotlin
frame.layout {
    draw(Div()) { panel ->
        layout {
            x = 10.percent
            y = 20.percent
            width = 80.percent
            height = 50.percent
        }
        panel.layout {
            // 这里的 50% 是面板的一半，不是窗口的一半。
            draw(TextArea("inside the panel")) { text ->
                layout {
                    x = 50.percent
                    width = 50.percent
                    height = 100.percent
                }
            }
        }
    }
}
```

规则：

* 传给``draw``的外层``layout { ... }``配置面板自身的几何；内部内容通过``panel.layout { ... }``进入，和其它绘制面完全一样；
* 面板里每个子组件的``x``/``width``都相对面板内容盒解析，所以移动或缩放面板会带动整块内容；
* ``clip``默认``true``，面板外的内容不会被绘制；设成``clip = false``可以让它溢出。非零``corner``也会把裁剪轮廓圆角化；
* ``Div``起始完全透明。需要画卡片或面板时，用``style { background { color = ... } }``给它填充；
* 它按绝对位置摆放子组件。要让子组件流动排列，就在面板里画一个[FlexBox](#flexbox)交给它排；
* ``Div``里可以再放``Div``或任何组件，百分比会自然嵌套；
* ``reset()``恢复空白区域的两个默认值：开启裁剪、透明填充。

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

导入：

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
* 标签超出标签条可见范围时会出现滚动条：横条显示横向滚动条，竖条显示竖向滚动条，只在需要时出现。滚动时标签保持一行或一列，不会换行；通过代码选中某个标签后，它会被滚动到可见区域内。标签放得下时，标签条仍会铺满整个区域；
* ``windowDragEnabled = true``会让标签条的空白背景成为窗口拖拽手柄：在那里按下并拖动即可移动窗口，标签按钮和滚动条仍保持原本的点击与拖拽行为。无边框窗口（``Frame(titleBarEnabled = false)``）可用它把标签条当作标题栏来拖动窗口；该属性默认关闭，也可以通过``pane.windowDragEnabled``设置；
* 设置``TabPane``的样式时，底层``TabBar``也会应用同样的样式：每次布局都会把背景、圆角、文字颜色和``transition``复制到``tab``，所以标签条与整个窗格外观一致。标签按钮在没有自行设置背景时不会涂自己的颜色，未选中的标签会透出窗格背景，而不是被控件默认的白色盖住。``selectColor { background = Color.WHITE; foreground = Color.BLACK }``只给选中的标签按钮涂上所选背景和文字颜色；未设置的``background``或``foreground``保留按钮自身或从窗格继承的外观。旧写法``pane.selectColor = Color.WHITE``仍然可用，但已标记为弃用且只能设置背景。``pane.selectionStyle``与``pane.tab.selectionStyle``是同一个对象，``pane.selectColor { }``与``pane.tab.selectColor { }``作用相同；
* 写在``TabPane``样式里的``transition``也会传给标签按钮，所以切换标签时``selectColor``的变化会有过渡动画。若某个标签按钮自己设置了``transition``，则以按钮自己的为准。