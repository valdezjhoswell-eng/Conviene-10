package com.rentablezone.app

import android.app.*
import android.content.*
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val captureRequest = 1001
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else toast("Permiso de superposición ya concedido") }
        findViewById<Button>(R.id.btnStart).setOnClickListener { startCapture() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopService(Intent(this, ScreenCaptureService::class.java)); findViewById<TextView>(R.id.txtStatus).text="Estado: detenido" }
        findViewById<Button>(R.id.btnZones).setOnClickListener { startActivity(Intent(this, ZonesActivity::class.java)) }
        findViewById<Button>(R.id.btnFilters).setOnClickListener { startActivity(Intent(this, FiltersActivity::class.java)) }
    }
    private fun startCapture() { if (!Settings.canDrawOverlays(this)) { toast("Primero autoriza la superposición"); return }; val mpm=getSystemService(MediaProjectionManager::class.java); startActivityForResult(mpm.createScreenCaptureIntent(), captureRequest) }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==captureRequest && resultCode==RESULT_OK && data!=null){ val i=Intent(this,ScreenCaptureService::class.java).apply{putExtra("resultCode",resultCode);putExtra("data",data)}; startForegroundService(i); findViewById<TextView>(R.id.txtStatus).text="Estado: visor activo" } }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
