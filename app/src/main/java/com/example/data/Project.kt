package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "طرق", "مستشفيات", "زراعة", "إسكان", "تعليم", "مساجد"
    val latitude: Double,
    val longitude: Double,
    val requiredAmount: Double,
    val collectedAmount: Double = 0.0,
    val status: String = "قيد_التمويل", // "قيد_التمويل", "مكتمل_التمويل" (جاهز للتنفيذ), "قيد_التنفيذ", "مكتمل_التنفيذ"
    val reporterName: String,
    val contractorName: String? = null,
    val escrowDeposited: Double = 0.0, // 80% guarantee amount deposited
    val imageKey: String? = null, // key for visual illustration
    val creationTime: Long = System.currentTimeMillis()
)
