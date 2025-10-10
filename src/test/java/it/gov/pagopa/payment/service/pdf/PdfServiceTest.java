package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private BarCodePaymentService barCodePaymentService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private TransactionBarCodeResponse trxResp;

    private PdfServiceImpl newService() {
        return new PdfServiceImpl(
                barCodePaymentService,        // mock
                resourceLoader,               // mock
                "DejaVuSans.ttf",             // se non presente, PdfUtils fa fallback a Helvetica
                null,                         // logoMimit
                null,                         // logoPari
                null,                         // iconWasher
                null,                         // iconHealthcard
                null                          // iconBarcode
        );
    }

    private PdfServiceImpl newServiceWithFont(String fontPath) {
        return new PdfServiceImpl(
                barCodePaymentService,
                resourceLoader,
                fontPath,     // simula font inesistente per testare il fallback
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void create_shouldReturnValidPdfBase64_andCallBarcodeService() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(trxResp.getVoucherAmountCents()).thenReturn(10_00L);
        when(barCodePaymentService.retriveVoucher("INIT1", "TRX1", "USER1")).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INIT1", "TRX1", "USER1", "Giovanna Beltramin", "BLTGVN78A52C409X");

        assertNotNull(report);
        byte[] pdfBytes = Base64.getDecoder().decode(report.getData());
        assertTrue(pdfBytes.length > 0);

        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith("%PDF-"));

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            assertTrue(pdf.getNumberOfPages() >= 1);
        }

        var initCap = ArgumentCaptor.forClass(String.class);
        var trxCap  = ArgumentCaptor.forClass(String.class);
        var usrCap  = ArgumentCaptor.forClass(String.class);
        verify(barCodePaymentService).retriveVoucher(initCap.capture(), trxCap.capture(), usrCap.capture());
        assertEquals("INIT1", initCap.getValue());
        assertEquals("TRX1",  trxCap.getValue());
        assertEquals("USER1", usrCap.getValue());
    }

    @Test
    void create_shouldContainExpectedTexts() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(trxResp.getVoucherAmountCents()).thenReturn(25_00L); // €25.00
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INIT1", "TRX1", "USER1",
                "Giovanna Beltramin", "BLTGVN78A52C409X");
        byte[] bytes = Base64.getDecoder().decode(report.getData());

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String page1Text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            String norm = normalize(page1Text);

            // Header + titolo
            assertTrue(norm.contains("BONUS ELETTRODOMESTICI"),
                    () -> "Missing 'BONUS ELETTRODOMESTICI' in:\n" + norm);
            // Ministero: verifichiamo per parole chiave per evitare problemi di spazi/punteggiatura
            assertTrue(norm.contains("MINISTERO") && norm.contains("IMPRESE") && norm.contains("MADE") && norm.contains("ITALY"),
                    () -> "Missing ministero line keywords in:\n" + norm);

            // Intestatario/CF
            assertTrue(norm.contains("GIOVANNA") && norm.contains("BELTRAMIN"),
                    () -> "Missing name in:\n" + norm);
            assertTrue(norm.contains("BLTGVN78A52C409X"),
                    () -> "Missing CF in:\n" + norm);
            assertTrue(norm.contains("DETTAGLI DEL BONUS".replaceAll("[^A-Z0-9]+"," ")),
                    () -> "Missing 'DETTAGLI DEL BONUS' in:\n" + norm);

            // Sezione barcode
            assertTrue(norm.contains("CODICE A BARRE".replaceAll("[^A-Z0-9]+"," ")),
                    () -> "Missing 'CODICE A BARRE' in:\n" + norm);

            // Box "Come usare il bonus" + step (controllo per parole chiave)
            assertTrue(norm.contains("COME USARE IL BONUS".replaceAll("[^A-Z0-9]+"," ")),
                    () -> "Missing 'COME USARE IL BONUS' in:\n" + norm);
            assertTrue(norm.contains("SCEGLI") && norm.contains("ELETTRODOMESTICO") && norm.contains("SOSTITUIRE"),
                    () -> "Missing step 1 keywords in:\n" + norm);
            assertTrue(norm.contains("PORTA") && norm.contains("TESSERA") && norm.contains("SANITARIA"),
                    () -> "Missing step 2 keywords in:\n" + norm);
            assertTrue(norm.contains("MOSTRA") && norm.contains("CODICE") && norm.contains("BARRE"),
                    () -> "Missing step 3 keywords in:\n" + norm);

            // Powered by + Pari/PagoPA (gestiamo S.P.A. che spesso esce come 'S P A')
            boolean hasPowered = norm.contains("POWERED BY");
            boolean hasPari = norm.contains("PARI");
            boolean hasPagoPA = norm.contains("PAGOPA") && norm.contains("SPA"); // S.P.A → "SPA"
            assertTrue(hasPowered && (hasPari || hasPagoPA),
                    () -> "Missing 'Powered by' and brand in:\n" + norm);
        }
    }

    @Test
    void create_withMissingFont_shouldFallbackAndGeneratePdf() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("87654321");
        when(trxResp.getVoucherAmountCents()).thenReturn(45_50L);
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        PdfServiceImpl svc = newServiceWithFont("FONT-CHE-NON-ESISTE.ttf");

        ReportDTO report = svc.create("I", "T", "U", "Mario Rossi", "RSSMRA80A01H501Z");

        byte[] pdfBytes = Base64.getDecoder().decode(report.getData());
        assertTrue(pdfBytes.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            assertTrue(pdf.getNumberOfPages() >= 1);
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage()).toUpperCase();

            assertTrue(text.contains("MARIO ROSSI"));
            assertTrue(text.contains("RSSMRA80A01H501Z"));
        }
    }

    @Test
    void create_shouldRenderDatesAndAmountReasonably() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-01-02T08:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-31T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("ABCDEF12");
        when(trxResp.getVoucherAmountCents()).thenReturn(123_45L); // 123,45 €
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INITZ", "TRXZ", "USERZ", "Laura Bianchi", "BNCLRA80A01H501X");

        byte[] bytes = Base64.getDecoder().decode(report.getData());
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());

            assertTrue(text.contains("2025"));

            String upper = text.toUpperCase();
            boolean hasCurrency = upper.contains("€") || upper.contains(" EUR") || upper.contains("EURO");
            assertTrue(hasCurrency, "Importo non riconosciuto in pagina (manca simbolo €/EUR/EURO)");
        }
    }

    @Test
    void create_whenBarcodeServiceThrows_shouldWrapAndRethrow() {
        when(barCodePaymentService.retriveVoucher(any(), any(), any()))
                .thenThrow(new IllegalStateException("Backend down"));

        PdfServiceImpl svc = newService();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                svc.create("INIT1", "TRX1", "USER1", "X", "Y"));

        assertTrue(ex.getMessage().toUpperCase().contains("ERRORE DURANTE LA GENERAZIONE DEL PDF"));
        verify(barCodePaymentService).retriveVoucher("INIT1", "TRX1", "USER1");
    }

    private static String normalize(String s) {
        String up = s.toUpperCase();
        up = up.replace('’', '\'')
                .replace('‘', '\'')
                .replace('`', '\'');
        up = java.text.Normalizer.normalize(up, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        up = up.replaceAll("[^A-Z0-9]+", " ");
        up = up.trim().replaceAll("\\s+", " ");
        return up;
    }
}
