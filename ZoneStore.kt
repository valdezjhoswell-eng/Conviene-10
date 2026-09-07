package com.rentablezone.app

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

data class SavedZone(val type: Int, val points: List<LatLng>)
data class SavedLocality(val type: Int, val name: String, val lat: Double, val lng: Double)

object ZoneStore {
    private const val KEY = "saved_zones"
    private const val LOCALITY_KEY = "saved_localities"
    private fun prefs(c: Context) = c.getSharedPreferences("zones", 0)

    fun load(c: Context): List<SavedZone> {
        val array = JSONArray(prefs(c).getString(KEY, "[]") ?: "[]")
        val result = mutableListOf<SavedZone>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val pointArray = obj.getJSONArray("p")
            val points = mutableListOf<LatLng>()
            for (j in 0 until pointArray.length()) {
                val point = pointArray.getJSONObject(j)
                points.add(LatLng(point.getDouble("lat"), point.getDouble("lng")))
            }
            result.add(SavedZone(obj.getInt("t"), points))
        }
        return result
    }

    fun save(c: Context, zones: List<SavedZone>) {
        val array = JSONArray()
        zones.forEach { zone ->
            val points = JSONArray()
            zone.points.forEach { point -> points.put(JSONObject().put("lat", point.latitude).put("lng", point.longitude)) }
            array.put(JSONObject().put("t", zone.type).put("p", points))
        }
        prefs(c).edit().putString(KEY, array.toString()).apply()
    }

    fun loadLocalities(c: Context): List<SavedLocality> {
        val array = JSONArray(prefs(c).getString(LOCALITY_KEY, "[]") ?: "[]")
        val result = mutableListOf<SavedLocality>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(SavedLocality(o.getInt("t"), o.getString("n"), o.getDouble("lat"), o.getDouble("lng")))
        }
        return result
    }

    fun saveLocalities(c: Context, localities: List<SavedLocality>) {
        val array = JSONArray()
        localities.forEach { l ->
            array.put(JSONObject().put("t", l.type).put("n", l.name).put("lat", l.lat).put("lng", l.lng))
        }
        prefs(c).edit().putString(LOCALITY_KEY, array.toString()).apply()
    }

    fun zoneFor(c: Context, lat: Double, lng: Double): Int? {
        val point = LatLng(lat, lng)
        return load(c).asSequence().filter { it.points.size >= 3 && contains(it.points, point) }.map { it.type }.firstOrNull()
    }

    fun zoneForLocality(c: Context, locality: String?, subLocality: String?, featureName: String?, adminArea: String?): Int? {
        val candidates = listOf(locality, subLocality, featureName, adminArea).filterNotNull().map(::norm).filter { it.isNotBlank() }
        return loadLocalities(c).firstOrNull { saved ->
            val n = norm(saved.name)
            candidates.any { it == n || it.contains(n) || n.contains(it) }
        }?.type
    }

    private fun norm(s: String): String = java.text.Normalizer.normalize(s.lowercase(java.util.Locale.getDefault()), java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").replace("[^a-z0-9 ]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()

    private fun contains(poly: List<LatLng>, p: LatLng): Boolean {
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val xi = poly[i].longitude; val yi = poly[i].latitude
            val xj = poly[j].longitude; val yj = poly[j].latitude
            val crosses = ((yi > p.latitude) != (yj > p.latitude)) &&
                (p.longitude < (xj - xi) * (p.latitude - yi) / (yj - yi + 1e-12) + xi)
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }
}
