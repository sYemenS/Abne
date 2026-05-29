package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallets")
data class UserWallet(
    @PrimaryKey val role: String, // "donor", "contractor"
    val balance: Double
)
