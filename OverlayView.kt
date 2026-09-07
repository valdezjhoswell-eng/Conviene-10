package com.rentablezone.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class OverlayView(private val context:Context):LinearLayout(context){
    private val title=TextView(context); private val detail=TextView(context)
    init{orientation=VERTICAL;setPadding(18,12,18,12);val bg=GradientDrawable();bg.setColor(Color.argb(210,20,30,45));bg.cornerRadius=24f;background=bg;title.setTextColor(Color.WHITE);title.textSize=18f;title.setTypeface(null,1);detail.setTextColor(Color.WHITE);detail.textSize=14f;addView(title);addView(detail)}
    fun showResult(result:Analysis){title.text=result.label;title.setTextColor(result.color);detail.text=result.detail}
}
data class Analysis(val label:String,val detail:String,val color:Int,val destination:String=""){ fun withZone(type:Int?):Analysis { val suffix=when(type){2->"\n🔴 ZONA PELIGROSA";1->"\n🟡 ZONA DE PRECAUCIÓN";else->"\n🟢 ZONA PERMITIDA"}; return copy(detail=detail+suffix,label=if(type==2)"🔴 NO CONVIENE" else label,color=if(type==2)Color.rgb(255,100,100) else color) }}
