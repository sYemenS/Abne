package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BonyanDao {
    @Query("SELECT * FROM projects ORDER BY creationTime DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<Donation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: Donation)

    @Query("SELECT * FROM user_wallets WHERE role = :role")
    suspend fun getWalletByRole(role: String): UserWallet?

    @Query("SELECT * FROM user_wallets")
    fun getAllWalletsFlow(): Flow<List<UserWallet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: UserWallet)
}
