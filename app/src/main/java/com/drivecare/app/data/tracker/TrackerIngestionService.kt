package com.drivecare.app.data.tracker

import org.json.JSONObject
import java.util.Locale

object TrackerIngestionService {

    /**
     * Parses a generic REST/JSON webhook or MQTT JSON message into a TrackerPayload.
     * Supported JSON formats:
     * 1. Standard: {"imei":"8642010...", "lat":37.7749, "lng":-122.4194, "speed":45.5, "timestamp":1700000000000}
     * 2. Teltonika REST: {"imei":"...", "latitude":..., "longitude":..., "speed":...}
     * 3. SinoTrack/Concox: {"device_id":"...", "lat":..., "lon":..., "speed_kmh":...}
     */
    fun parseJsonPayload(jsonString: String, protocolVendor: String = "GENERIC_JSON"): TrackerPayload? {
        return try {
            val json = JSONObject(jsonString)
            val code = when {
                json.has("imei") -> json.getString("imei")
                json.has("trackerId") -> json.getString("trackerId")
                json.has("device_id") -> json.getString("device_id")
                json.has("id") -> json.getString("id")
                else -> return null
            }

            val lat = when {
                json.has("latitude") -> json.getDouble("latitude")
                json.has("lat") -> json.getDouble("lat")
                else -> 0.0
            }

            val lng = when {
                json.has("longitude") -> json.getDouble("longitude")
                json.has("lng") -> json.getDouble("lng")
                json.has("lon") -> json.getDouble("lon")
                else -> 0.0
            }

            val speed = when {
                json.has("speedKmh") -> json.getDouble("speedKmh")
                json.has("speed_kmh") -> json.getDouble("speed_kmh")
                json.has("speed") -> json.getDouble("speed")
                else -> 0.0
            }

            val timestamp = when {
                json.has("timestamp") -> json.getLong("timestamp")
                json.has("time") -> json.getLong("time")
                else -> System.currentTimeMillis()
            }

            val address = if (json.has("address")) json.getString("address") else ""

            TrackerPayload(
                trackerCode = code,
                latitude = lat,
                longitude = lng,
                speedKmh = speed,
                timestamp = timestamp,
                protocolVendor = protocolVendor,
                addressName = address
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses NMEA or raw NMEA/GPS sentence strings (e.g. Coban / SinoTrack ASCII):
     * Example: "$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A"
     */
    fun parseNmeaSentence(imei: String, nmea: String): TrackerPayload? {
        return try {
            if (!nmea.contains("\$GPRMC") && !nmea.contains("\$GNRMC")) return null
            val parts = nmea.split(",")
            if (parts.size < 10) return null
            if (parts[2] != "A") return null // Data active/valid check

            val rawLat = parts[3]
            val latDir = parts[4]
            val rawLng = parts[5]
            val lngDir = parts[6]
            val knots = parts[7].toDoubleOrNull() ?: 0.0

            if (rawLat.isEmpty() || rawLng.isEmpty()) return null

            val latDeg = rawLat.substring(0, 2).toDouble()
            val latMin = rawLat.substring(2).toDouble()
            var latitude = latDeg + (latMin / 60.0)
            if (latDir.uppercase(Locale.US) == "S") latitude = -latitude

            val lngDeg = rawLng.substring(0, 3).toDouble()
            val lngMin = rawLng.substring(3).toDouble()
            var longitude = lngDeg + (lngMin / 60.0)
            if (lngDir.uppercase(Locale.US) == "W") longitude = -longitude

            val speedKmh = knots * 1.852

            TrackerPayload(
                trackerCode = imei,
                latitude = latitude,
                longitude = longitude,
                speedKmh = speedKmh,
                timestamp = System.currentTimeMillis(),
                protocolVendor = "NMEA_GENERIC"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
