package it.gov.pagopa.payment.utils;

import it.gov.pagopa.payment.exception.custom.InvalidInvoiceFormatException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilitiesTest {

    @Mock
    private MultipartFile multipartFileMock;

    // ------------------------------------------------------------------------------------------------
    // Test: sanitizeString
    // ------------------------------------------------------------------------------------------------
    @Nested
    class SanitizeStringTest {

        @Test
        void testSanitizeString_NullInput_ReturnsNull() {
            assertNull(Utilities.sanitizeString(null));
        }

        @Test
        void testSanitizeString_RemovesNewLinesAndSpecialCharacters() {
            String input = "Hello\r\n World!@#$%^&*()_+-=[]{}|;:',.<>/?";
            // Deve mantenere alfanumerici, spazi e trattini
            String expected = "Hello World_-";

            String result = Utilities.sanitizeString(input);

            assertEquals(expected, result);
        }

        @Test
        void testSanitizeString_ValidString_Unchanged() {
            String input = "Valid String-123";
            assertEquals(input, Utilities.sanitizeString(input));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Test: sanitizeForLog
    // ------------------------------------------------------------------------------------------------
    @Nested
    class SanitizeForLogTest {

        @Test
        void testSanitizeForLog_NullInput_ReturnsNullString() {
            assertEquals("null", Utilities.sanitizeForLog(null));
        }

        @Test
        void testSanitizeForLog_ReplacesNewLinesWithUnderscore() {
            String input = "Log\rLine1\nLine2\r\nEnd";
            String expected = "Log_Line1_Line2__End";

            assertEquals(expected, Utilities.sanitizeForLog(input));
        }

        @Test
        void testSanitizeForLog_CleanString_Unchanged() {
            String input = "Clean log message 123";
            assertEquals(input, Utilities.sanitizeForLog(input));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Test: getLocalDate
    // ------------------------------------------------------------------------------------------------
    @Nested
    class GetLocalDateTest {

        @Test
        void testGetLocalDate_ConvertsCorrectly() {
            LocalDateTime offsetDateTime = LocalDateTime.of(2026, 7, 24, 12, 0, 0);
            LocalDate expectedDate = offsetDateTime.toLocalDate();

            LocalDate result = Utilities.getLocalDate(offsetDateTime);

            assertEquals(expectedDate, result);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Test: checkFileExtensionOrThrow
    // ------------------------------------------------------------------------------------------------
    @Nested
    class CheckFileExtensionOrThrowTest {

        @Test
        void testCheckFileExtensionOrThrow_NullFile_ThrowsException() {
            InvalidInvoiceFormatException exception = assertThrows(
                    InvalidInvoiceFormatException.class,
                    () -> Utilities.checkFileExtensionOrThrow(null)
            );

            assertEquals("File is required", exception.getMessage());
        }

        @Test
        void testCheckFileExtensionOrThrow_NullFilename_ThrowsException() {
            when(multipartFileMock.getOriginalFilename()).thenReturn(null);

            InvalidInvoiceFormatException exception = assertThrows(
                    InvalidInvoiceFormatException.class,
                    () -> Utilities.checkFileExtensionOrThrow(multipartFileMock)
            );

            assertEquals("File must be a PDF or XML", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"invoice.txt", "doc.docx", "image.png", "invoice_pdf", "xml_file", ""})
        void testCheckFileExtensionOrThrow_InvalidExtensions_ThrowsException(String filename) {
            when(multipartFileMock.getOriginalFilename()).thenReturn(filename);

            InvalidInvoiceFormatException exception = assertThrows(
                    InvalidInvoiceFormatException.class,
                    () -> Utilities.checkFileExtensionOrThrow(multipartFileMock)
            );

            assertEquals("File must be a PDF or XML", exception.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "invoice.pdf",
                "invoice.xml",
                "DOCUMENT.PDF",
                "DATA.XML",
                "Invoice.Pdf",
                "factura.Xml"
        })
        void testCheckFileExtensionOrThrow_ValidExtensions_Success(String filename) {
            when(multipartFileMock.getOriginalFilename()).thenReturn(filename);

            assertDoesNotThrow(() -> Utilities.checkFileExtensionOrThrow(multipartFileMock));
        }
    }
}