package ia.ankherth.grease

import ia.ankherth.grease.data.room.PdfHistoryEntity
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

/**
 * Unit tests for PDF progress calculation and entity operations
 */
class PdfProgressTest {

    @Test
    fun `calculate progress percentage correctly`() {
        // Test normal progress calculation
        val pdf1 = PdfHistoryEntity(
            uri = "test://pdf1",
            fileName = "Test PDF 1",
            totalPages = 100,
            lastPageRead = 25
        )
        assertEquals(25.0f, pdf1.progressPercentage, 0.1f)

        // Test edge case: first page
        val pdf2 = PdfHistoryEntity(
            uri = "test://pdf2",
            fileName = "Test PDF 2",
            totalPages = 50,
            lastPageRead = 0
        )
        assertEquals(0.0f, pdf2.progressPercentage, 0.1f)

        // Test edge case: last page
        val pdf3 = PdfHistoryEntity(
            uri = "test://pdf3",
            fileName = "Test PDF 3",
            totalPages = 20,
            lastPageRead = 20
        )
        assertEquals(100.0f, pdf3.progressPercentage, 0.1f)

        // Test edge case: zero pages (should not happen but handle gracefully)
        val pdf4 = PdfHistoryEntity(
            uri = "test://pdf4",
            fileName = "Test PDF 4",
            totalPages = 0,
            lastPageRead = 0
        )
        assertEquals(0.0f, pdf4.progressPercentage, 0.1f)
    }

    @Test
    fun `pdf entity creation with default values`() {
        val currentTime = Date()
        val pdf = PdfHistoryEntity(
            uri = "test://example.pdf",
            fileName = "Example.pdf",
            totalPages = 120
        )

        assertEquals("test://example.pdf", pdf.uri)
        assertEquals("Example.pdf", pdf.fileName)
        assertEquals(120, pdf.totalPages)
        assertEquals(0, pdf.lastPageRead)
        assertEquals(0.0f, pdf.progressPercentage, 0.1f)

        // Check that lastReadDate is recent (within last minute)
        val timeDifference = kotlin.math.abs(currentTime.time - Date(pdf.lastReadDate).time)
        assertTrue("Last read date should be recent", timeDifference < 60000) // 1 minute
    }

    @Test
    fun `pdf progress calculation with various scenarios`() {
        val testCases = listOf(
            Triple(10, 1, 10.0f),      // 1 out of 10 pages
            Triple(100, 33, 33.0f),    // 33 out of 100 pages
            Triple(7, 7, 100.0f),      // Complete reading
            Triple(1, 0, 0.0f),        // Single page, not started
            Triple(1, 1, 100.0f),      // Single page, completed
            Triple(200, 150, 75.0f)    // Large document, 3/4 complete
        )

        testCases.forEach { (totalPages, lastPage, expectedPercentage) ->
            val pdf = PdfHistoryEntity(
                uri = "test://pdf",
                fileName = "Test PDF",
                totalPages = totalPages,
                lastPageRead = lastPage
            )
            assertEquals(
                "Failed for $lastPage/$totalPages pages",
                expectedPercentage,
                pdf.progressPercentage,
                0.1f
            )
        }
    }

    @Test
    fun `pdf entity uri and filename validation`() {
        val pdf = PdfHistoryEntity(
            uri = "content://com.android.providers.media.documents/document/document%3A12345",
            fileName = "My Document with Special Characters & Symbols.pdf",
            totalPages = 45,
            lastPageRead = 10
        )

        assertNotNull(pdf.uri)
        assertTrue(pdf.uri.isNotEmpty())
        assertTrue(pdf.fileName.contains("Special Characters"))
        assertEquals(45, pdf.totalPages)
        assertEquals(10, pdf.lastPageRead)
        assertEquals(22.22f, pdf.progressPercentage, 0.1f)
    }
}
