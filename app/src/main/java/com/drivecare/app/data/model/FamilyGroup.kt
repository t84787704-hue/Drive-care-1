package com.drivecare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_groups")
data class FamilyGroup(
    @PrimaryKey
    val id: String = "",
    val groupName: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey
    val id: String = "",
    val groupId: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val role: String = "Member", // Father, Mother, Son, Daughter, Member
    val permission: String = "Viewer", // Viewer, Editor, Manager
    val status: String = "Accepted", // Pending, Accepted
    val joinedAt: Long = System.currentTimeMillis()
)
