package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val vehicleName: String,
    val reminderTitle: String,
    val reminderType: String = "Oil Change",
    val dueDate: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
