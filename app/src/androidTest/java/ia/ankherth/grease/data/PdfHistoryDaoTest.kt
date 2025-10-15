package ia.ankherth.grease.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.Date

/**
 * Instrumented tests for PdfHistoryDao database operations
 */
@RunWith(AndroidJUnit4::class)
class PdfHistoryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: PdfDatabase
    private lateinit var dao: PdfHistoryDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PdfDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.pdfHistoryDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrievePdf() = runBlocking {
        val pdf = PdfHistoryEntity(
            uri = "test://sample.pdf",
            fileName = "Sample.pdf",
            totalPages = 100,
            lastPageRead = 25,
            lastReadDate = Date()
        )

        dao.insertOrUpdatePdf(pdf)
        val retrieved = dao.getPdfByUri("test://sample.pdf")

        assertNotNull(retrieved)
        assertEquals(pdf.uri, retrieved!!.uri)
        assertEquals(pdf.fileName, retrieved.fileName)
        assertEquals(pdf.totalPages, retrieved.totalPages)
        assertEquals(pdf.lastPageRead, retrieved.lastPageRead)
    }

    @Test
    fun updatePdfProgress() = runBlocking {
        val pdf = PdfHistoryEntity(
            uri = "test://progress.pdf",
            fileName = "Progress.pdf",
            totalPages = 50,
            lastPageRead = 10
        )

        dao.insertOrUpdatePdf(pdf)

        // Update progress
        val newDate = Date()
        dao.updateProgress("test://progress.pdf", 30, newDate)

        val updated = dao.getPdfByUri("test://progress.pdf")
        assertNotNull(updated)
        assertEquals(30, updated!!.lastPageRead)
        assertEquals(60.0f, updated.progressPercentage, 0.1f)
    }

    @Test
    fun getAllPdfsOrderedByDate() = runBlocking {
        val pdf1 = PdfHistoryEntity(
            uri = "test://old.pdf",
            fileName = "Old.pdf",
            totalPages = 100,
            lastReadDate = Date(System.currentTimeMillis() - 86400000) // 1 day ago
        )

        val pdf2 = PdfHistoryEntity(
            uri = "test://recent.pdf",
            fileName = "Recent.pdf",
            totalPages = 80,
            lastReadDate = Date() // Now
        )

        dao.insertOrUpdatePdf(pdf1)
        dao.insertOrUpdatePdf(pdf2)

        val allPdfs = dao.getAllPdfs()
        assertEquals(2, allPdfs.size)
        assertEquals("Recent.pdf", allPdfs[0].fileName) // Most recent first
        assertEquals("Old.pdf", allPdfs[1].fileName)
    }

    @Test
    fun getMostRecentPdf() = runBlocking {
        val pdf1 = PdfHistoryEntity(
            uri = "test://first.pdf",
            fileName = "First.pdf",
            totalPages = 100,
            lastReadDate = Date(System.currentTimeMillis() - 3600000) // 1 hour ago
        )

        val pdf2 = PdfHistoryEntity(
            uri = "test://second.pdf",
            fileName = "Second.pdf",
            totalPages = 80,
            lastReadDate = Date() // Now
        )

        dao.insertOrUpdatePdf(pdf1)
        dao.insertOrUpdatePdf(pdf2)

        val mostRecent = dao.getMostRecentPdf()
        assertNotNull(mostRecent)
        assertEquals("Second.pdf", mostRecent!!.fileName)
    }

    @Test
    fun deletePdfByUri() = runBlocking {
        val pdf = PdfHistoryEntity(
            uri = "test://delete.pdf",
            fileName = "Delete.pdf",
            totalPages = 60
        )

        dao.insertOrUpdatePdf(pdf)

        val beforeDelete = dao.getPdfByUri("test://delete.pdf")
        assertNotNull(beforeDelete)

        dao.deletePdfByUri("test://delete.pdf")

        val afterDelete = dao.getPdfByUri("test://delete.pdf")
        assertNull(afterDelete)
    }

    @Test
    fun replaceExistingPdf() = runBlocking {
        val originalPdf = PdfHistoryEntity(
            uri = "test://same.pdf",
            fileName = "Original.pdf",
            totalPages = 100,
            lastPageRead = 10
        )

        dao.insertOrUpdatePdf(originalPdf)

        val updatedPdf = PdfHistoryEntity(
            uri = "test://same.pdf",
            fileName = "Updated.pdf",
            totalPages = 120,
            lastPageRead = 50
        )

        dao.insertOrUpdatePdf(updatedPdf)

        val result = dao.getPdfByUri("test://same.pdf")
        assertNotNull(result)
        assertEquals("Updated.pdf", result!!.fileName)
        assertEquals(120, result.totalPages)
        assertEquals(50, result.lastPageRead)

        // Should only have one PDF in database
        val allPdfs = dao.getAllPdfs()
        assertEquals(1, allPdfs.size)
    }
}
