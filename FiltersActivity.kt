package com.rentablezone.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class FiltersActivity: AppCompatActivity(){
    private val p by lazy { getSharedPreferences("settings",0) }
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_filters)
        val cash=findViewById<Spinner>(R.id.spCash); cash.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Cabify: no filtrar efectivo","Cabify: advertir efectivo","Cabify: rechazar efectivo")); cash.setSelection(p.getInt("cashMode",0))
        findViewById<EditText>(R.id.eMinKm).setText(p.getFloat("minKm",500f).toString()); findViewById<EditText>(R.id.eMinHour).setText(p.getFloat("minHour",12000f).toString()); findViewById<EditText>(R.id.eMaxKm).setText(p.getFloat("maxKm",30f).toString()); findViewById<EditText>(R.id.eMaxMin).setText(p.getFloat("maxMin",60f).toString()); findViewById<EditText>(R.id.eMinTrips).setText(p.getInt("minTrips",0).toString()); findViewById<SeekBar>(R.id.seekAlpha).progress=p.getInt("alpha",60)
        findViewById<Button>(R.id.btnSave).setOnClickListener{p.edit().putFloat("minKm",v(R.id.eMinKm)).putFloat("minHour",v(R.id.eMinHour)).putFloat("maxKm",v(R.id.eMaxKm)).putFloat("maxMin",v(R.id.eMaxMin)).putInt("minTrips",findViewById<EditText>(R.id.eMinTrips).text.toString().toIntOrNull()?:0).putInt("cashMode",cash.selectedItemPosition).putInt("alpha",findViewById<SeekBar>(R.id.seekAlpha).progress).apply();toast("Configuración guardada");finish()}
    }
    private fun v(id:Int)=findViewById<EditText>(id).text.toString().replace(',','.').toFloatOrNull()?:0f
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
