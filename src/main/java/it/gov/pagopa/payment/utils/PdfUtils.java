package it.gov.pagopa.payment.utils;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import it.gov.pagopa.payment.exception.custom.PdfGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility comuni per la generazione di PDF con iText.
 * Include metodi per separatori, label, celle, formattazione, immagini e font.
 */
@Slf4j
public final class PdfUtils {

    private PdfUtils() {}

    /**
     * Stile per la cella "step" (font, colori e loader risorse).
     */
    public record StepStyle(
            PdfFont regular,
            PdfFont bold,
            com.itextpdf.kernel.colors.Color textPrimary,
            com.itextpdf.kernel.colors.Color textSecondary,
            ResourceLoader loader
    ) {}

    /**
     * Crea un separatore orizzontale solido con spessore e colore opzionale.
     *
     * @param width spessore della linea (es. 0.8f)
     * @param color colore della linea (può essere null)
     * @return LineSeparator configurato
     */
    public static LineSeparator newSolidSeparator(float width, Color color) {
        var line = new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(width);
        if (color != null) line.setColor(color);
        return new LineSeparator(line);
    }

    /**
     * Crea una piccola etichetta (in MAIUSCOLO) per campi descrittivi.
     *
     * @param text testo della label
     * @param font font da usare
     * @param color colore del testo
     * @return Paragraph formattato
     */
    public static Paragraph smallLabel(String text, PdfFont font, Color color) {
        return new Paragraph(text.toUpperCase())
                .setFont(font)
                .setFontSize(9)
                .setFontColor(color)
                .setMarginBottom(2);
    }

    /**
     * Restituisce una cella senza bordo contenente l'elemento fornito.
     *
     * @param element IBlockElement da inserire nella cella
     * @return Cell senza bordo
     */
    public static Cell noBorderCell(IBlockElement element) {
        Cell c = new Cell().setBorder(Border.NO_BORDER);
        c.add(element);
        return c;
    }

    /**
     * Formatta una data in formato italiano (dd/MM/yyyy).
     *
     * @param date data
     * @return stringa formattata
     */
    public static String formatDateIt(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Converte un importo in formato valuta italiana (es. € 123,45).
     *
     * @param amount importo
     * @return stringa in valuta italiana
     */
    public static String formatCurrencyIt(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return nf.format(amount);
    }

    /**
     * Carica un'immagine da file system, classpath o URL (se supportato dal ResourceLoader).
     * <p>
     * Se il path inizia con "classpath:", prova come risorsa Spring; altrimenti tenta come file locale.
     * In caso di errore, restituisce null senza lanciare eccezioni.
     *
     * @param path           percorso immagine ("classpath:...", "/opt/app/logo.png", ecc.)
     * @param maxWidth       larghezza massima (0 per ignorare)
     * @param resourceLoader ResourceLoader Spring per il classpath/URL
     * @return Image oppure null se non caricabile
     */
    public static Image loadImageOrNull(String path, float maxWidth, ResourceLoader resourceLoader) {
        if (path == null || path.isBlank()) return null;

        try {
            String p = path.trim();

            Resource resource = resourceLoader.getResource(p);
            try (InputStream is = resource.getInputStream()) {
                byte[] data = is.readAllBytes();
                Image img = new Image(ImageDataFactory.create(data));
                if (maxWidth > 0) img.setMaxWidth(maxWidth);
                return img;
            }
        } catch (Exception ignored) {
            try {
                File file = new File(path);
                if (!file.exists()) return null;
                Image img = new Image(ImageDataFactory.create(file.toURI().toURL()));
                if (maxWidth > 0) img.setMaxWidth(maxWidth);
                return img;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Carica un font TTF/OTF da classpath o filesystem.
     * <ul>
     *     <li>Se {@code configuredFont} è vuoto o "Helvetica", usa Helvetica di default (embedded=false).</li>
     *     <li>Se inizia con "classpath:", carica i bytes come risorsa e crea il font embedded.</li>
     *     <li>Altrimenti tenta sul filesystem; in caso di fallimento torna ad Helvetica.</li>
     * </ul>
     *
     * @param configuredFont percorso font (classpath o file) oppure "Helvetica"
     * @param bold           se true usa la variante bold (se Helvetica di fallback)
     * @param loader         ResourceLoader per il classpath
     * @return PdfFont pronto all'uso
     * @throws PdfException  in caso di errori iText nella creazione del font
     */
    public static PdfFont loadPdfFont(String configuredFont, boolean bold, ResourceLoader loader) {
        String f = configuredFont == null ? "" : configuredFont.trim();

        try {
            if (f.isEmpty() || "Helvetica".equalsIgnoreCase(f)) {
                log.debug("[PdfUtils] Font non specificato o 'Helvetica': uso font di default ({})",
                        bold ? "HELVETICA_BOLD" : "HELVETICA");
                return helvetica(bold);
            }

            if (f.startsWith("classpath:")) {
                Resource res = loader.getResource(f);
                if (res.exists()) {
                    try (InputStream ignored = res.getInputStream()) {
                        log.debug("[PdfUtils] Font caricato da classpath: {}", f);
                        return PdfFontFactory.createFont();
                    }
                } else {
                    log.warn("[PdfUtils] Font non trovato nel classpath: {}. Fallback su Helvetica.", f);
                    return helvetica(bold);
                }
            }

            File file = new File(f);
            if (file.exists()) {
                log.debug("[PdfUtils] Font caricato dal filesystem: {}", file.getAbsolutePath());
                return PdfFontFactory.createFont();
            }

            log.warn("[PdfUtils] Font non trovato in alcun percorso ({}). Fallback su Helvetica.", f);
            return helvetica(bold);

        } catch (IOException | PdfException ex) {
            log.error("[PdfUtils] Errore durante il caricamento del font '{}': {}. Fallback su Helvetica.",
                    f, ex.getMessage(), ex);
            return helvetica(bold);
        } catch (Exception ex) {
            log.error("[PdfUtils] Errore inatteso durante il caricamento del font '{}'", f, ex);
            throw new PdfGenerationException("Errore durante il caricamento del font " + f, true, ex);
        }
    }

    private static PdfFont helvetica(boolean bold) {
        try {
            return PdfFontFactory.createFont(
                    bold ? StandardFonts.HELVETICA_BOLD : StandardFonts.HELVETICA);
        } catch (IOException e) {
            log.error("[PdfUtils] ERRORE CRITICO: impossibile caricare anche il font Helvetica", e);
            throw new PdfGenerationException(
                    "Impossibile caricare il font Helvetica", true, e);
        }
    }

    /**
     * Crea una cella "step" con icona (se disponibile), titolo e descrizione centrati.
     * Usata nel box "COME USARE IL BONUS".
     *
     * @param iconPath percorso icona (classpath o file)
     * @param heading  titolo breve dello step
     * @param desc     descrizione testuale dello step
     * @param style    stile (font, colori, loader)
     * @return Cell pronta da inserire in tabella
     */
    public static Cell stepCell(String iconPath, String heading, String desc, StepStyle style) {
        Cell c = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);

        Div d = new Div()
                .setWidth(UnitValue.createPercentValue(100))
                .setTextAlignment(TextAlignment.CENTER);

        // icona o placeholder rotondo
        Image icon = loadImageOrNull(iconPath, 0, style.loader());
        if (icon != null) {
            icon.setAutoScale(false)
                    .scaleToFit(24, 24)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginBottom(6);
            d.add(icon);
        } else {
            d.add(new Div()
                    .setWidth(24)
                    .setHeight(24)
                    .setBackgroundColor(new DeviceGray(0.9f))
                    .setBorderRadius(new BorderRadius(12))
                    .setMarginBottom(6)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));
        }

        d.add(new Paragraph(heading)
                .setFont(style.bold())
                .setFontSize(10)
                .setFontColor(style.textPrimary())
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        d.add(new Paragraph(desc)
                .setFont(style.regular())
                .setFontSize(9)
                .setFontColor(style.textSecondary())
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0));

        c.add(d);
        return c;
    }
}
