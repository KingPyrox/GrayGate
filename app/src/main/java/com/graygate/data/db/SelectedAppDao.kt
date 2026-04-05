package com.graygate.data.db

import androidx.room.*
import com.graygate.data.model.SelectedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedAppDao {

    @Query("SELECT * FROM selected_apps ORDER BY appName ASC")
    fun getAllSelectedApps(): Flow<List<SelectedApp>>

    @Query("SELECT packageName FROM selected_apps")
    suspend fun getAllSelectedPackageNames(): List<String>

    @Query("SELECT packageName FROM selected_apps")
    fun observeSelectedPackageNames(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM selected_apps WHERE packageName = :packageName)")
    suspend fun isSelected(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SelectedApp)

    @Delete
    suspend fun delete(app: SelectedApp)

    @Query("DELETE FROM selected_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) FROM selected_apps")
    fun getSelectedCount(): Flow<Int>
}
