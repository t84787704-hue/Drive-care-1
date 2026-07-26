package com.drivecare.app.data.tracker

data class TrackerPayload(
    val trackerCode: String, // IMEI or Hardware Tracker ID
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val protocolVendor: String = "GENERIC_JSON", // "TELTONIKA", "CONCOX", "SINOTRACK", "COBAN", "JT808", "GENERIC_JSON"
    val addressName: String = ""
)
