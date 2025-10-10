package it.gov.pagopa.payment.utils;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.UnitValue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfUtilsTest {

    @Test
    void formatDateIt_shouldFormat_ddMMyyyy() {
        String s = PdfUtils.formatDateIt(LocalDate.of(2025, 10, 9));
        assertEquals("09/10/2025", s);
    }

    @Test
    void formatCurrencyIt_shouldFormat_italianLocale() {
        String s = PdfUtils.formatCurrencyIt(new BigDecimal("1234.5"));
        assertTrue(s.contains("1.234"));
        assertTrue(s.contains("50"));
        assertTrue(s.contains("€"));
    }

    @Test
    void smallLabel_shouldUppercase_andRender() throws Exception {
        PdfFont dummy = com.itextpdf.kernel.font.PdfFontFactory.createFont(StandardFonts.HELVETICA);
        Paragraph p = PdfUtils.smallLabel("Label di test", dummy, new DeviceGray(0.5f));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter w = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(w);
             Document doc = new Document(pdf)) {
            doc.add(p);
        }
        try (PdfReader r = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
             PdfDocument pdf = new PdfDocument(r)) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertTrue(text.contains("LABEL DI TEST"));
        }
    }

    @Test
    void noBorderCell_shouldContainElement_andRender() throws Exception {
        Paragraph inner = new Paragraph("X");
        Cell c = PdfUtils.noBorderCell(inner);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter w = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(w);
             Document doc = new Document(pdf)) {
            Table t = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            t.addCell(c);
            doc.add(t);
        }
        try (PdfReader r = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
             PdfDocument pdf = new PdfDocument(r)) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertTrue(text.contains("X"));
        }
    }

    @Test
    void newSolidSeparator_shouldCreateLineSeparator() {
        LineSeparator ls = PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f));
        assertNotNull(ls);
    }

    @Test
    void loadImageOrNull_shouldReturnNull_whenBlankPath() {
        ResourceLoader rl = mock(ResourceLoader.class);
        Image img = PdfUtils.loadImageOrNull("   ", 0, rl);
        assertNull(img);
    }

    @Test
    void loadPdfFont_shouldFallbackToHelvetica_whenEmptyOrHelvetica() {
        ResourceLoader rl = mock(ResourceLoader.class);

        PdfFont f1 = PdfUtils.loadPdfFont("", false, rl);
        PdfFont f2 = PdfUtils.loadPdfFont("Helvetica", true, rl);

        assertNotNull(f1);
        assertNotNull(f2);
        assertDoesNotThrow(f1::getFontProgram);
        assertDoesNotThrow(f2::getFontProgram);
    }

    @Test
    void loadPdfFont_shouldLoadFromClasspath_ifExists() throws Exception {
        ResourceLoader rl = mock(ResourceLoader.class);
        Resource res = mock(Resource.class);
        when(rl.getResource("classpath:/fonts/DejaVuSans.ttf")).thenReturn(res);
        when(res.exists()).thenReturn(true);
        when(res.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{0x00, 0x01}));

        PdfFont loaded = PdfUtils.loadPdfFont("classpath:/fonts/DejaVuSans.ttf", false, rl);
        assertNotNull(loaded);
        assertDoesNotThrow(loaded::getFontProgram);
    }

    @Test
    void stepCell_shouldRenderWithPlaceholder_whenIconMissing() throws Exception {
        ResourceLoader rl = Mockito.mock(ResourceLoader.class);
        Resource res = Mockito.mock(Resource.class);
        when(rl.getResource(anyString())).thenReturn(res);
        when(res.getInputStream()).thenThrow(new RuntimeException("not found"));

        PdfFont regular = com.itextpdf.kernel.font.PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        PdfUtils.StepStyle style = new PdfUtils.StepStyle(
                regular, bold, new DeviceGray(0.2f), new DeviceGray(0.6f), rl
        );

        Cell cell = PdfUtils.stepCell(
                "classpath:/icons/missing.png",
                "Titolo Step",
                "Descrizione dello step",
                style
        );

        // Render e verifica che i testi compaiano
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            Table t = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            t.addCell(cell);
            doc.add(t);
        }
        try (PdfReader r = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
             PdfDocument pdf = new PdfDocument(r)) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertTrue(text.contains("Titolo Step"));
            assertTrue(text.contains("Descrizione dello step"));
        }
    }

    @Test
    void stepCell_shouldRenderWithIcon_whenResourcePresent() throws Exception {
        String tinyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg==";
        byte[] pngBytes = Base64.getDecoder().decode(tinyPngBase64);

        ResourceLoader rl = mock(ResourceLoader.class);
        Resource res = mock(Resource.class);
        when(rl.getResource("classpath:/icons/ok.png")).thenReturn(res);
        when(res.getInputStream()).thenReturn(new ByteArrayInputStream(pngBytes));

        PdfFont regular = com.itextpdf.kernel.font.PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont bold = com.itextpdf.kernel.font.PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        PdfUtils.StepStyle style = new PdfUtils.StepStyle(
                regular, bold, new DeviceGray(0.2f), new DeviceGray(0.6f), rl
        );

        Cell cell = PdfUtils.stepCell(
                "classpath:/icons/ok.png",
                "Titolo",
                "Descrizione",
                style
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            Table t = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            t.addCell(cell);
            doc.add(t);
        }
        assertTrue(baos.size() > 0);
        try (PdfReader r = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
             PdfDocument pdf = new PdfDocument(r)) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            assertTrue(text.contains("Titolo"));
            assertTrue(text.contains("Descrizione"));
        }
    }
}
