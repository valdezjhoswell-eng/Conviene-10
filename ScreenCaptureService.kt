package com.rentablezone.app

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.content.pm.ServiceInfo
import android.os.*
import android.location.Geocoder
import java.util.concurrent.Executors
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import kotlin.math.roundToInt

class ScreenCaptureService:Service(){
    private var projection:MediaProjection?=null; private var reader:ImageReader?=null; private var vd:VirtualDisplay?=null; private var overlay:OverlayView?=null; private var wm:WindowManager?=null; private val handler=Handler(Looper.getMainLooper()); private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); private var busy=false; private var lastDestination=""; private var lastOcrAt=0L; private val geoExec=Executors.newSingleThreadExecutor()
    private val prefs by lazy{getSharedPreferences("settings",0)}
    override fun onCreate(){super.onCreate();createChannel();}
    override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int{ if(i==null)return START_NOT_STICKY; val rc=i.getIntExtra("resultCode",0); val data=i.getParcelableExtra<Intent>("data")?:return START_NOT_STICKY; startForeground(77,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION); projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(rc,data); showOverlay(); startCapture(); return START_NOT_STICKY }
    private fun notification():Notification=NotificationCompat.Builder(this,"visor").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("¿Conviene? activo").setContentText("Analizando visualmente la oferta; no interactúa con Uber/Cabify").setOngoing(true).build()
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("visor","¿Conviene?",NotificationManager.IMPORTANCE_LOW))}
    private fun showOverlay(){if(!Settings.canDrawOverlays(this))return;wm=getSystemService(WindowManager::class.java);overlay=OverlayView(this);val lp=WindowManager.LayoutParams(dp(315),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP or Gravity.END;lp.x=12;lp.y=90;wm?.addView(overlay,lp)}
    private fun startCapture(){val dm=resources.displayMetrics;val w=dm.widthPixels;val h=dm.heightPixels;reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);vd=projection?.createVirtualDisplay("¿Conviene?",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,null);reader?.setOnImageAvailableListener({r->if(!busy)process(r)},handler);}
    private fun process(r:ImageReader){
        val now=System.currentTimeMillis()
        if(busy || now-lastOcrAt<450L) { try{r.acquireLatestImage()?.close()}catch(_:Exception){}; return }
        val im=try{r.acquireLatestImage()}catch(_:Exception){null}?:return
        busy=true; lastOcrAt=now
        val plane=im.planes[0]
        val pixelStride=plane.pixelStride
        val rowStride=plane.rowStride
        val rowPadding=rowStride-pixelStride*im.width
        val paddedWidth=im.width+rowPadding/pixelStride
        val tmp=Bitmap.createBitmap(paddedWidth,im.height,Bitmap.Config.ARGB_8888)
        try { tmp.copyPixelsFromBuffer(plane.buffer) } catch(_:Exception) { im.close(); tmp.recycle(); busy=false; return }
        im.close()
        val bmp=if(paddedWidth==im.width) tmp else Bitmap.createBitmap(tmp,0,0,im.width,im.height)
        if(bmp!==tmp) tmp.recycle()
        recognizer.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener{res->
            val a=OfferParser.parse(res.text,prefs)
            overlay?.showResult(a)
            val dest=a.destination
            if(dest.isNotBlank() && dest!=lastDestination){
                lastDestination=dest
                geoExec.execute{
                    val z=try{
                        val g=Geocoder(this,Locale("es","AR"))
                        g.getFromLocationName(dest,1)?.firstOrNull()
                    }catch(_:Exception){null}
                    if(z!=null){
                        val type=ZoneStore.zoneForLocality(this, z.locality, z.subLocality, z.featureName, z.adminArea)
                            ?: ZoneStore.zoneFor(this, z.latitude, z.longitude)
                        handler.post{overlay?.showResult(a.withZone(type))}
                    }
                }
            }
        }.addOnFailureListener{ overlay?.showResult(Analysis("⚠️ LECTURA EN CURSO","No se pudo leer este cuadro.\nEsperando la próxima oferta…",Color.YELLOW,""))
        }.addOnCompleteListener{busy=false;bmp.recycle()}
    }
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);vd?.release();reader?.close();projection?.stop();overlay?.let{try{wm?.removeView(it)}catch(_:Exception){}};recognizer.close();geoExec.shutdownNow();super.onDestroy()}
    override fun onBind(intent:Intent?)=null
    private fun dp(v:Int)= (v*resources.displayMetrics.density).roundToInt()
}

object OfferParser{
    private val money=Regex("(?:ARS|\\$)\\s*([0-9][0-9.,]*)|([0-9][0-9.,]*)\\s*ARS",RegexOption.IGNORE_CASE)
    private val km=Regex("([0-9]+(?:[.,][0-9]+)?)\\s*km",RegexOption.IGNORE_CASE)
    private val meters=Regex("([0-9]+(?:[.,][0-9]+)?)\\s*m(?:\\b|$)",RegexOption.IGNORE_CASE)
    private val min=Regex("([0-9]+)\\s*min",RegexOption.IGNORE_CASE)
    private val trips=Regex("([0-9]+)\\s*viajes",RegexOption.IGNORE_CASE)
    fun parse(text:String,p:android.content.SharedPreferences):Analysis{
        val lines=text.lines().map{it.trim()}.filter{it.isNotBlank()}; val t=lines.joinToString(" "); val low=t.lowercase(Locale.getDefault())
        val app=when{low.contains("cabify")->"CABIFY";low.contains("uber")->"UBER";else->"VIAJE"}
        val amounts=money.findAll(t).mapNotNull{m->parseNum(m.groupValues.drop(1).firstOrNull{it.isNotBlank()})}.toList()
        val fare=amounts.filter{it>=1000}.maxOrNull()?:amounts.maxOrNull()?:0.0
        val pickupDistance=distanceFromLine(lines){it.contains("distancia")||it.contains("recogida") || (min.containsMatchIn(it)&&meters.containsMatchIn(it))}
        val tripDistance=distanceFromLine(lines){it.contains("viaje de")||it.contains("viaje ")}
        val all=km.findAll(t).map{parseNum(it.groupValues[1])}.toMutableList(); meters.findAll(t).forEach{all+=parseNum(it.groupValues[1])/1000.0}
        val distance=when{pickupDistance!=null&&tripDistance!=null->pickupDistance+tripDistance;all.size>=2->all.takeLast(2).sum();all.size==1->all[0];else->0.0}
        val pickupMin=minutesFromLine(lines){it.contains("distancia")||it.contains("recogida") || (min.containsMatchIn(it)&&meters.containsMatchIn(it))}
        val tripMin=minutesFromLine(lines){it.contains("viaje de")||it.contains("viaje ")}
        val mins=min.findAll(t).mapNotNull{it.groupValues[1].toDoubleOrNull()}.toList()
        val duration=when{pickupMin!=null&&tripMin!=null->pickupMin+tripMin;mins.size>=2->mins.takeLast(2).sum();mins.size==1->mins[0];else->0.0}
        val perKm=if(distance>0)fare/distance else 0.0; val perHour=if(duration>0)fare/(duration/60.0) else 0.0
        val maxKm=p.getFloat("maxKm",30f).toDouble(); val maxMin=p.getFloat("maxMin",60f).toDouble(); val minKm=p.getFloat("minKm",500f).toDouble(); val minHour=p.getFloat("minHour",12000f).toDouble(); val minTrips=p.getInt("minTrips",0)
        val tripCount=trips.find(t)?.groupValues?.get(1)?.toIntOrNull()?:0; val cash=app=="CABIFY"&&(low.contains("efectivo")||low.contains("cash")); val cashMode=p.getInt("cashMode",0)
        val bad=distance>maxKm||duration>maxMin||perKm<minKm||perHour<minHour||(tripCount>0&&tripCount<minTrips)||(cash&&cashMode==2)
        val cashNote=when{cash&&cashMode==2->"💵 EFECTIVO: RECHAZAR";cash&&cashMode==1->"💵 EFECTIVO: REVISAR";cash->"💵 EFECTIVO";app=="CABIFY"->"💳 PAGO EN APP / NO DETECTADO";else->""}
        val label=when { fare <= 0.0 && app == "UBER" -> "🔵 UBER DETECTADO"; fare <= 0.0 && app == "CABIFY" -> "🔵 CABIFY DETECTADO"; bad -> "🔴 NO CONVIENE"; else -> "🟢 CONVIENE" }
        val color=when { fare <= 0.0 && app != "VIAJE" -> Color.CYAN; bad -> Color.rgb(255,100,100); else -> Color.rgb(90,240,150) }
        val detail=buildString{append(if(fare>0) "💰 ${fmt(fare)} ARS\n" else "💰 Oferta todavía no detectada\n");append("📏 ${fmt1(distance)} km recorridos\n");append("💵 ${fmt(perKm)}/km real\n");append("⏱️ ${fmt(perHour)}/h\n");append("⌛ ${fmt1(duration)} min totales\n");if(tripCount>0)append("👤 $tripCount viajes\n")else append("👤 Viajes no detectados\n");if(cashNote.isNotBlank())append(cashNote+"\n");append(app)}
        return Analysis(label,detail,color,extractDestination(lines))
    }
    private fun distanceFromLine(lines:List<String>,test:(String)->Boolean):Double?{for(raw in lines){val l=raw.lowercase(Locale.getDefault());if(test(l)){km.find(raw)?.let{return parseNum(it.groupValues[1])};meters.find(raw)?.let{return parseNum(it.groupValues[1])/1000.0}}};return null}
    private fun minutesFromLine(lines:List<String>,test:(String)->Boolean):Double?{for(raw in lines){val l=raw.lowercase(Locale.getDefault());if(test(l))return min.find(raw)?.groupValues?.get(1)?.toDoubleOrNull()};return null}
    private fun extractDestination(lines:List<String>):String{for(i in lines.indices){if(lines[i].lowercase(Locale.getDefault()).contains("viaje de")||Regex("\\d+\\s*min.*(?:km|m)",RegexOption.IGNORE_CASE).containsMatchIn(lines[i]))return lines.getOrNull(i+1).orEmpty()};return lines.lastOrNull().orEmpty()}
    private fun parseNum(s:String?):Double{if(s==null)return 0.0;val x=s.trim();return when{x.contains('.')&&x.contains(',')->x.replace(".","").replace(',','.').toDoubleOrNull()?:0.0;x.contains('.')&&x.substringAfter('.').length==3->x.replace(".","").toDoubleOrNull()?:0.0;x.contains(',')&&x.substringAfter(',').length==3->x.replace(",","").toDoubleOrNull()?:0.0;else->x.replace(",",".").toDoubleOrNull()?:0.0}}
    private fun fmt(v:Double)=String.format(Locale.US,"%,.0f",v).replace(',','.')
    private fun fmt1(v:Double)=String.format(Locale.US,"%.1f",v).replace('.',',')
}
