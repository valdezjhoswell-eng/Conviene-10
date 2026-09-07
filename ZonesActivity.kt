package com.rentablezone.app

import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import java.util.Locale
import java.util.concurrent.Executors

class ZonesActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private val executor = Executors.newSingleThreadExecutor()
    private val localities = mutableListOf<SavedLocality>()
    private val markers = mutableMapOf<String, Marker>()
    private val circles = mutableMapOf<String, Circle>()
    private lateinit var list: TextView
    private lateinit var spinner: Spinner

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_zones)
        localities.addAll(ZoneStore.loadLocalities(this))
        list = findViewById(R.id.txtSelected)
        spinner = findViewById(R.id.spZone)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("🟢 Permitida", "🟡 Precaución", "🔴 Peligrosa"))
        findViewById<Button>(R.id.btnClear).setOnClickListener { localities.clear(); ZoneStore.saveLocalities(this, localities); redrawSaved(); updateList() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { ZoneStore.saveLocalities(this, localities); toast("Selección guardada (${localities.size} localidades)") }
        findViewById<Button>(R.id.btnBackPolygon).setOnClickListener { toast("Ahora se seleccionan localidades tocando directamente el mapa") }
        updateList()
        val mv = findViewById<MapView>(R.id.map); mv.onCreate(b); mv.getMapAsync(this)
    }

    override fun onMapReady(g: GoogleMap) {
        map = g
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-34.6037, -58.3816), 10.8f))
        map.setOnMapClickListener { ll -> resolveLocality(ll) }
        redrawSaved()
    }

    private fun resolveLocality(ll: LatLng) {
        toast("Buscando localidad…")
        executor.execute {
            val address = try { Geocoder(this, Locale("es", "AR")).getFromLocation(ll.latitude, ll.longitude, 1)?.firstOrNull() } catch (_: Exception) { null }
            runOnUiThread {
                if (address == null) { toast("No se pudo identificar la localidad. Acercá el mapa e intentá otra vez."); return@runOnUiThread }
                val name = localityName(address)
                if (name.isBlank()) { toast("No se encontró una localidad en ese punto"); return@runOnUiThread }
                val key = norm(name)
                val existing = localities.indexOfFirst { norm(it.name) == key }
                if (existing >= 0) {
                    localities.removeAt(existing)
                    toast("$name deseleccionada")
                } else {
                    localities.add(SavedLocality(spinner.selectedItemPosition, name, ll.latitude, ll.longitude))
                    toast("$name seleccionada")
                }
                ZoneStore.saveLocalities(this, localities)
                redrawSaved(); updateList()
            }
        }
    }

    private fun localityName(a: Address): String {
        val raw = a.locality ?: a.subLocality ?: a.subAdminArea ?: a.adminArea ?: a.featureName ?: ""
        return raw.replace("Partido de ", "", ignoreCase = true).trim()
    }

    private fun redrawSaved() {
        if (!::map.isInitialized) return
        markers.values.forEach { it.remove() }; markers.clear()
        circles.values.forEach { it.remove() }; circles.clear()
        localities.forEach { l ->
            val pos = LatLng(l.lat, l.lng); val c = color(l.type)
            val marker = map.addMarker(MarkerOptions().position(pos).title("${label(l.type)} ${l.name}").snippet("Tocá nuevamente para quitarla"))
            if (marker != null) markers[norm(l.name)] = marker
            circles[norm(l.name)] = map.addCircle(CircleOptions().center(pos).radius(1200.0).strokeColor(c).strokeWidth(4f).fillColor(Color.argb(35, Color.red(c), Color.green(c), Color.blue(c))))
        }
    }

    private fun updateList() { list.text = if (localities.isEmpty()) "Ninguna localidad seleccionada" else localities.joinToString("\n") { "${label(it.type)} ${it.name}" } }
    private fun label(t: Int) = when (t) { 0 -> "🟢"; 1 -> "🟡"; else -> "🔴" }
    private fun color(t: Int) = when (t) { 0 -> Color.rgb(30,170,90); 1 -> Color.rgb(220,160,20); else -> Color.rgb(210,55,70) }
    private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(Locale.getDefault()), java.text.Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "").trim()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { executor.shutdownNow(); findViewById<MapView>(R.id.map)?.onDestroy(); super.onDestroy() }
    override fun onResume() { super.onResume(); findViewById<MapView>(R.id.map)?.onResume() }
    override fun onPause() { findViewById<MapView>(R.id.map)?.onPause(); super.onPause() }
    override fun onLowMemory() { super.onLowMemory(); findViewById<MapView>(R.id.map)?.onLowMemory() }
}
