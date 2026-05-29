package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val projectTitle: String,
    val donorName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
