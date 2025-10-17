package ia.ankherth.grease.data.room

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * DAO (Data Access Object) para manejar operaciones de base de datos relacionadas con el historial de PDFs
 */
@Dao
interface PdfHistoryDao {
    @Query("SELECT * FROM pdf_history ORDER BY lastReadDate DESC")
    fun getAllPdfs(): LiveData<List<PdfHistoryEntity>>

    @Query("SELECT * FROM pdf_history ORDER BY lastReadDate DESC LIMIT 1")
    suspend fun getMostRecentPdf(): PdfHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: PdfHistoryEntity)

    @Update
    suspend fun update(pdf: PdfHistoryEntity)

    @Delete
    suspend fun delete(pdf: PdfHistoryEntity)

    @Query("DELETE FROM pdf_history WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM pdf_history")
    suspend fun clearAllHistory()

    @Query("UPDATE pdf_history SET lastPageRead = :lastPageRead, scrollOffset = :scrollOffset, lastReadDate = :lastReadDate WHERE uri = :uri")
    suspend fun updateProgress(uri: String, lastPageRead: Int, scrollOffset: Float = 0f, lastReadDate: Long = System.currentTimeMillis())

    @Query("SELECT * FROM pdf_history WHERE uri = :uri")
    suspend fun getPdfByUri(uri: String): PdfHistoryEntity?

    @Query("SELECT * FROM pdf_history WHERE fileName LIKE '%' || :query || '%'")
    fun searchPdfsByName(query: String): LiveData<List<PdfHistoryEntity>>

    @Query("UPDATE pdf_history SET isAccessible = :isAccessible WHERE uri = :uri")
    suspend fun updateAccessibility(uri: String, isAccessible: Boolean)

    @Query("UPDATE pdf_history SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, isFavorite: Boolean)

    @Query("SELECT * FROM pdf_history WHERE isFavorite = 1 ORDER BY lastReadDate DESC")
    fun getFavoritePdfs(): LiveData<List<PdfHistoryEntity>>

    @Query("SELECT * FROM pdf_history ORDER BY lastReadDate DESC")
    suspend fun getAllPdfsList(): List<PdfHistoryEntity>
}
