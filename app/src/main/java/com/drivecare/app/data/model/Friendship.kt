package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friendships")
data class Friendship(
    @PrimaryKey
    val id: String = "", // uid1_uid2 (sorted alphabetically)
    val user1Uid: String = "",
    val user2Uid: String = "",
    val user1Name: String = "",
    val user1Email: String = "",
    val user1Photo: String = "",
    val user2Name: String = "",
    val user2Email: String = "",
    val user2Photo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
