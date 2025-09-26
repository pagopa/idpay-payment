package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceImplTest {

    private static final String DEV_PORTAL_LINK = "https://developer.pagopa.it/pari/overview";

    @Test
    void create_shouldReturnValidPdfBytes() throws Exception {
        PdfServiceImpl svc = new PdfServiceImpl(DEV_PORTAL_LINK);

        byte[] bytes = svc.create(null, "trx-123", "user-abc");

        assertNotNull(bytes, "I bytes del PDF non devono essere null");
        assertTrue(bytes.length > 0, "Il PDF non deve essere vuoto");
        // header %PDF-
        String header = new String(bytes, 0, Math.min(5, bytes.length), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith("%PDF-"), "Header PDF mancante");

        // Il documento deve essere apribile e avere almeno 1 pagina
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            assertTrue(pdf.getNumberOfPages() >= 1, "Il PDF deve avere almeno una pagina");
        }
    }

    @Test
    void create_shouldContainDevPortalLinkAnnotation() throws Exception {
        PdfServiceImpl svc = new PdfServiceImpl(DEV_PORTAL_LINK);

        byte[] bytes = svc.create(null, "trx-123", "user-abc");

        boolean found = false;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {

            // Scorri le annotazioni di tutte le pagine (o solo la prima, se preferisci)
            for (int p = 1; p <= pdf.getNumberOfPages() && !found; p++) {
                for (PdfAnnotation ann : pdf.getPage(p).getAnnotations()) {
                    if (ann instanceof PdfLinkAnnotation link) {
                        // Dizionario dell'annotazione
                        PdfDictionary annDict = link.getPdfObject();

                        // /A = action dictionary
                        PdfDictionary actionDict = annDict.getAsDictionary(PdfName.A);
                        if (actionDict == null) continue;

                        // /S deve essere /URI
                        PdfName s = actionDict.getAsName(PdfName.S);
                        if (!PdfName.URI.equals(s)) continue;

                        // /URI contiene l'URL
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
        PdfServiceImpl svc = new PdfServiceImpl(DEV_PORTAL_LINK);

        byte[] bytes = svc.create(null, "trx-123", "user-abc");

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {

            String page1Text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());

            // Verifiche "soft" sul testo estratto
            assertTrue(page1Text.toUpperCase().contains("BONUS ELETTRODOMESTICI"),
                    "Testo 'BONUS ELETTRODOMESTICI' non trovato");
            assertTrue(page1Text.toUpperCase().contains("CODICE A BARRE"),
                    "Testo 'CODICE A BARRE' non trovato");
            assertTrue(page1Text.contains("12345678"),
                    "Numero del barcode '12345678' non trovato");
        }
    }
}
