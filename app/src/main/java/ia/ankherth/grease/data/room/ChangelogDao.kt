package ia.ankherth.grease.data.room

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * DAO para el manejo del registro interno de cambios (changelog)
 * Permite agregar, consultar y manipular las entradas del registro de cambios
 */
@Dao
interface ChangelogDao {
    @Query("SELECT * FROM changelog_entries ORDER BY changeDate DESC")
    fun getAllChanges(): LiveData<List<ChangelogEntry>>

    @Query("SELECT * FROM changelog_entries WHERE versionName = :versionName")
    fun getChangesByVersion(versionName: String): LiveData<List<ChangelogEntry>>

    @Insert
    suspend fun insert(entry: ChangelogEntry)

    @Insert
    suspend fun insertAll(entries: List<ChangelogEntry>)

    @Update
    suspend fun update(entry: ChangelogEntry)

    @Delete
    suspend fun delete(entry: ChangelogEntry)

    @Query("SELECT * FROM changelog_entries WHERE isUserVisible = 1 ORDER BY changeDate DESC")
    fun getUserVisibleChanges(): LiveData<List<ChangelogEntry>>

    @Query("SELECT DISTINCT versionName FROM changelog_entries ORDER BY versionCode DESC")
    fun getAllVersions(): LiveData<List<String>>
}
