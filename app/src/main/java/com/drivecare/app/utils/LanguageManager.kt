package com.drivecare.app.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    URDU("اردو (Urdu)", "ur"),
    ROMAN_URDU("Roman Urdu", "ur_roman")
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

object AppStrings {
    fun get(key: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.URDU -> urduMap[key] ?: englishMap[key] ?: key
            AppLanguage.ROMAN_URDU -> romanUrduMap[key] ?: englishMap[key] ?: key
            AppLanguage.ENGLISH -> englishMap[key] ?: key
        }
    }

    private val englishMap = mapOf(
        "app_name" to "DriveCare",
        "tab_summary" to "Summary",
        "tab_garage" to "Garage",
        "tab_fuel" to "Fuel",
        "tab_service" to "Service",
        "tab_reminders" to "Reminders",
        
        "add_vehicle" to "Add Vehicle",
        "edit_vehicle" to "Edit Vehicle",
        "delete_vehicle" to "Delete Vehicle",
        "no_vehicles_title" to "No Vehicles Added Yet",
        "no_vehicles_desc" to "Tap the button below to add your first vehicle.",
        "vehicle_name" to "Vehicle Name",
        "brand" to "Brand",
        "model" to "Model",
        "year" to "Year",
        "plate_no" to "Plate No.",
        "fuel_type" to "Fuel Type",
        "odometer" to "Odometer Reading",
        "type" to "Type",
        "fuel" to "Fuel",
        "plate" to "Plate",
        
        "log_fuel" to "Log Fuel",
        "select_vehicle" to "Select Vehicle",
        "all_vehicles" to "All Vehicles",
        "total_spent" to "Total Spent",
        "total_quantity" to "Total Quantity",
        "litres" to "Litres/Gals",
        "cost" to "Cost ($)",
        "gas_station" to "Gas Station (Optional)",
        "no_fuel_entries" to "No fuel entries found for selected vehicle.",
        
        "add_service_log" to "Add Service Log",
        "service_title" to "Service Title (e.g. Oil Change)",
        "workshop" to "Workshop / Garage",
        "no_service_logs" to "No Service Logs Yet",
        "no_service_desc" to "Track oil changes, repairs, and tire rotations.",
        
        "add_reminder" to "Add Reminder",
        "reminder_title" to "Reminder Title (e.g. Insurance)",
        "due_date" to "Due Date",
        "no_reminders" to "No Service Reminders",
        "no_reminders_desc" to "Set reminders for insurance renewal, oil change, or inspection.",
        
        "save" to "Save",
        "cancel" to "Cancel",
        "delete" to "Delete",
        "confirm_delete_title" to "Delete Vehicle",
        "confirm_delete_msg" to "Are you sure you want to delete this vehicle? All associated logs will be removed.",
        "change_language" to "Change Language",
        
        "overview_title" to "DriveCare Overview",
        "pending_tasks" to "Pending Tasks",
        "fuel_spent" to "Fuel Spent",
        "service_spent" to "Service Spent",
        "fleet_summary" to "Quick Fleet Summary"
    )

    private val urduMap = mapOf(
        "app_name" to "ڈرائیو کیئر",
        "tab_summary" to "خلاصہ",
        "tab_garage" to "گیراج",
        "tab_fuel" to "ایندھن",
        "tab_service" to "سروس",
        "tab_reminders" to "یاد دہانی",
        
        "add_vehicle" to "گاڑی شامل کریں",
        "edit_vehicle" to "گاڑی تبدیل کریں",
        "delete_vehicle" to "گاڑی ختم کریں",
        "no_vehicles_title" to "ابھی تک کوئی گاڑی شامل نہیں کی گئی",
        "no_vehicles_desc" to "اپنی پہلی گاڑی شامل کرنے کے لیے نیچے دیے گئے بٹن پر کلک کریں۔",
        "vehicle_name" to "گاڑی کا نام",
        "brand" to "کمپنی / برانڈ",
        "model" to "ماڈل",
        "year" to "سال",
        "plate_no" to "نمبر پلیٹ",
        "fuel_type" to "ایندھن کی قسم",
        "odometer" to "میٹر ریڈنگ (کلو میٹر)",
        "type" to "قسم",
        "fuel" to "ایندھن",
        "plate" to "پلیٹ",
        
        "log_fuel" to "ایندھن درج کریں",
        "select_vehicle" to "گاڑی منتخب کریں",
        "all_vehicles" to "تمام گاڑیاں",
        "total_spent" to "کل خرچ",
        "total_quantity" to "کل مقدار (لیٹر)",
        "litres" to "لیٹر",
        "cost" to "قیمت ($)",
        "gas_station" to "پیٹرول پمپ",
        "no_fuel_entries" to "منتخب کردہ گاڑی کے لیے کوئی ایندھن کا ریکارڈ نہیں ملا۔",
        
        "add_service_log" to "سروس درج کریں",
        "service_title" to "کام کی تفصیل (مثلاً آئل چینج)",
        "workshop" to "ورکشاپ / گیراج",
        "no_service_logs" to "کوئی سروس ریکارڈ موجود نہیں",
        "no_service_desc" to "آئل چینج، مرمت اور پرزوں کا حساب رکھیں۔",
        
        "add_reminder" to "یاد دہانی لگائیں",
        "reminder_title" to "عنوان (مثلاً انشورنس / آئل)",
        "due_date" to "تاریخ",
        "no_reminders" to "کوئی یاد دہانی موجود نہیں",
        "no_reminders_desc" to "انشورنس اور مرمت کے لیے وقت پر یاد دہانی سیٹ کریں۔",
        
        "save" to "محفوظ کریں",
        "cancel" to "منسوخ کریں",
        "delete" to "ختم کریں",
        "confirm_delete_title" to "گاڑی ختم کریں",
        "confirm_delete_msg" to "کیا آپ واقعی اس گاڑی کو ختم کرنا چاہتے ہیں؟ اس کا تمام ریکارڈ بھی ختم ہو جائے گا۔",
        "change_language" to "زبان تبدیل کریں",
        
        "overview_title" to "ڈرائیو کیئر خلاصہ",
        "pending_tasks" to "باقی کام",
        "fuel_spent" to "ایندھن کا خرچ",
        "service_spent" to "سروس کا خرچ",
        "fleet_summary" to "گاڑیوں کی صورتحال"
    )

    private val romanUrduMap = mapOf(
        "app_name" to "DriveCare",
        "tab_summary" to "Khulasa",
        "tab_garage" to "Gariyan",
        "tab_fuel" to "Fuel",
        "tab_service" to "Service",
        "tab_reminders" to "Yaad Dehani",
        
        "add_vehicle" to "Gari Shamil Karen",
        "edit_vehicle" to "Gari Change Karen",
        "delete_vehicle" to "Gari Delete Karen",
        "no_vehicles_title" to "Koi Gari Shamil Nahi Hai",
        "no_vehicles_desc" to "Apni pehli gari add karne k liye neeche button dabaen.",
        "vehicle_name" to "Gari Ka Naam",
        "brand" to "Brand",
        "model" to "Model",
        "year" to "Year",
        "plate_no" to "Number Plate",
        "fuel_type" to "Fuel Type",
        "odometer" to "Meter Reading (km)",
        "type" to "Type",
        "fuel" to "Fuel",
        "plate" to "Plate",
        
        "log_fuel" to "Fuel Log Karen",
        "select_vehicle" to "Gari Select Karen",
        "all_vehicles" to "Tamam Gariyan",
        "total_spent" to "Kul Kharcha",
        "total_quantity" to "Kul Fuel (L)",
        "litres" to "Litres",
        "cost" to "Qeemat ($)",
        "gas_station" to "Petrol Pump",
        "no_fuel_entries" to "Iss gari ka koi fuel record nahi hai.",
        
        "add_service_log" to "Service Log Karen",
        "service_title" to "Kaam ki Tafseel (e.g. Oil Change)",
        "workshop" to "Workshop / Garage",
        "no_service_logs" to "Koi Service Record Nahi Hai",
        "no_service_desc" to "Oil change, repair aur tuning ka hisab rakhen.",
        
        "add_reminder" to "Reminder Lagayen",
        "reminder_title" to "Title (e.g. Insurance)",
        "due_date" to "Tareekh",
        "no_reminders" to "Koi Reminder Nahi Hai",
        "no_reminders_desc" to "Insurance renewal aur tuning k liye reminder lagayen.",
        
        "save" to "Save Karen",
        "cancel" to "Cancel",
        "delete" to "Delete",
        "confirm_delete_title" to "Gari Delete Karen",
        "confirm_delete_msg" to "Kya aap is gari ko delete karna chahte hain? Tamam record bhi delete ho jayega.",
        "change_language" to "Language Badlen",
        
        "overview_title" to "DriveCare Khulasa",
        "pending_tasks" to "Baqi Kaam",
        "fuel_spent" to "Fuel Kharcha",
        "service_spent" to "Service Kharcha",
        "fleet_summary" to "Gariyon Ka Record"
    )
}
