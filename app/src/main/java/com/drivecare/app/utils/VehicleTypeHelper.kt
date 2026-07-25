package com.drivecare.app.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class VehicleTypeInfo(
    val code: String,
    val stringKey: String
)

object VehicleTypeHelper {
    val ALL_TYPES = listOf(
        VehicleTypeInfo("Car", "vtype_car"),
        VehicleTypeInfo("Motorcycle", "vtype_motorcycle"),
        VehicleTypeInfo("Scooter", "vtype_scooter"),
        VehicleTypeInfo("Bus", "vtype_bus"),
        VehicleTypeInfo("Van", "vtype_van"),
        VehicleTypeInfo("Pickup Truck", "vtype_pickup_truck"),
        VehicleTypeInfo("SUV", "vtype_suv"),
        VehicleTypeInfo("Jeep", "vtype_jeep"),
        VehicleTypeInfo("Taxi", "vtype_taxi"),
        VehicleTypeInfo("Truck", "vtype_truck"),
        VehicleTypeInfo("Tractor", "vtype_tractor"),
        VehicleTypeInfo("Trailer", "vtype_trailer"),
        VehicleTypeInfo("Minibus", "vtype_minibus"),
        VehicleTypeInfo("RV / Camper", "vtype_rv_camper"),
        VehicleTypeInfo("Electric Car", "vtype_electric_car"),
        VehicleTypeInfo("Hybrid Car", "vtype_hybrid_car"),
        VehicleTypeInfo("Bicycle", "vtype_bicycle"),
        VehicleTypeInfo("Fleet", "vtype_fleet"),
        VehicleTypeInfo("Other", "vtype_other")
    )

    /**
     * Get the Material Icon vector corresponding to any vehicle type string.
     */
    fun getVehicleIcon(type: String): ImageVector {
        val normalized = type.trim().lowercase()
        return when {
            normalized.contains("minibus") -> Icons.Default.DirectionsBus
            normalized.contains("bus") -> Icons.Default.DirectionsBus
            normalized.contains("scooter") || normalized.contains("moped") -> Icons.Default.Moped
            normalized.contains("motorcycle") || normalized.contains("bike") || normalized.contains("two") -> Icons.Default.TwoWheeler
            normalized.contains("bicycle") || normalized.contains("cycle") -> Icons.Default.DirectionsBike
            normalized.contains("taxi") || normalized.contains("cab") -> Icons.Default.LocalTaxi
            normalized.contains("tractor") || normalized.contains("farm") -> Icons.Default.Agriculture
            normalized.contains("rv") || normalized.contains("camper") || normalized.contains("trailer") -> Icons.Default.RvHookup
            normalized.contains("pickup") -> Icons.Default.LocalShipping
            normalized.contains("truck") -> Icons.Default.LocalShipping
            normalized.contains("van") || normalized.contains("shuttle") -> Icons.Default.AirportShuttle
            normalized.contains("suv") -> Icons.Default.DirectionsCar
            normalized.contains("jeep") -> Icons.Default.DirectionsCar
            normalized.contains("electric") || normalized.contains("ev") || normalized.contains("hybrid") -> Icons.Default.ElectricCar
            else -> Icons.Default.DirectionsCar
        }
    }

    /**
     * Get the localized display name for a vehicle type.
     */
    fun getDisplayName(type: String, lang: AppLanguage): String {
        val matched = ALL_TYPES.find { it.code.equals(type, ignoreCase = true) }
        return if (matched != null) {
            AppStrings.get(matched.stringKey, lang)
        } else {
            type
        }
    }
}
