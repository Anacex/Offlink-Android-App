package com.offlinepayment.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineUserDao {
    @Query("SELECT * FROM offline_users WHERE email = :email")
    suspend fun getUserByEmail(email: String): OfflineUser?
    
    @Query("SELECT * FROM offline_users WHERE email = :email AND isEmailVerified = 1")
    suspend fun getVerifiedUserByEmail(email: String): OfflineUser?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: OfflineUser)
    
    @Query("DELETE FROM offline_users WHERE email = :email")
    suspend fun deleteUser(email: String)
    
    @Query("SELECT * FROM offline_users")
    suspend fun getAllUsers(): List<OfflineUser>
}

