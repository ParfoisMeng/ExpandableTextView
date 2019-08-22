package com.parfoismeng.expandabletextview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTest.setContentText("短文本无展开收起")

        tvTestLong.setContentText("“清明前后，种瓜种豆。”当然，那都是为漫长夏日的怎样过活做准备的，儿时生在乡下的人，这些话多半还是有些耳熟的。一般的话，清明过后是一日热胜一日，偶尔会有个不应时的桃花暮春雪，也是个稀少的意外，大多这个时候，植物随着气候变化也即将转换着面目，即便是在城里，也能掐算着哪个时候能见上什么。踏春的时尚，之外哪能少的了吃货们的小算计呢。")
    }
}
