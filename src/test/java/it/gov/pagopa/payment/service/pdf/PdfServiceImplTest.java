package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceImplTest {

    private static final String DEV_PORTAL_LINK = "https://developer.pagopa.it/pari/overview";

    @Mock
    private BarCodePaymentService barCodePaymentService;

    @Mock
    private TransactionBarCodeResponse trxResp;

    private PdfServiceImpl newService() {
        return new PdfServiceImpl(
                barCodePaymentService,        // mock
                DEV_PORTAL_LINK,              // link dev portal
                "DejaVuSans.ttf",             // font (se non esiste, userà Helvetica)
                null,                         // logoMimit
                null,                         // logoPari
                null,                         // iconWasher
                null,                         // iconHealthcard
                null                          // iconBarcode
        );
    }

    @Test
    void create_shouldReturnValidPdfBase64_andCallBarcodeService() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(barCodePaymentService.retriveVoucher("INIT1", "TRX1", "USER1")).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        // Ora create ritorna Base64
        String base64 = svc.create("INIT1", "TRX1", "USER1");

        assertNotNull(base64);
        assertFalse(base64.isBlank());

        // Decodifica per validare il contenuto PDF
        byte[] pdfBytes = Base64.getDecoder().decode(base64);
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
    void create_shouldContainDevPortalLinkAnnotation() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        String base64 = svc.create("INIT1", "TRX1", "USER1");
        byte[] bytes = Base64.getDecoder().decode(base64);

        boolean found = false;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {

            for (int p = 1; p <= pdf.getNumberOfPages() && !found; p++) {
                for (PdfAnnotation ann : pdf.getPage(p).getAnnotations()) {
                    if (ann instanceof PdfLinkAnnotation link) {
                        PdfDictionary annDict = link.getPdfObject();
                        PdfDictionary actionDict = annDict.getAsDictionary(PdfName.A);
                        if (actionDict == null) continue;
                        if (!PdfName.URI.equals(actionDict.getAsName(PdfName.S))) continue;
                        PdfString uriStr = actionDict.getAsString(PdfName.URI);
                        if (uriStr != null && DEV_PORTAL_LINK.equals(uriStr.toUnicodeString())) {
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(found, "Link annotation al dev portal non trovata nel PDF");
    }

    @Test
    void create_shouldContainExpectedTexts() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        PdfServiceImpl svc = newService();

        String base64 = svc.create("INIT1", "TRX1", "USER1");
        byte[] bytes = Base64.getDecoder().decode(base64);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String page1Text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertTrue(page1Text.toUpperCase().contains("BONUS ELETTRODOMESTICI"));
            assertTrue(page1Text.toUpperCase().contains("CODICE A BARRE"));
        }
    }
}
