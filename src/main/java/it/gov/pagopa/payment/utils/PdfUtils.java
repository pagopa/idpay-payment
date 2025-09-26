package it.gov.pagopa.payment.utils;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;

import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PdfUtils {

    private PdfUtils() {}

    /**
     * Crea un separatore orizzontale con spessore e colore specifici.
     * Utile per standardizzare i line separator nel documento.
     *
     * @param width spessore linea in punti
     * @param color colore della linea (può essere null per default)
     * @return LineSeparator pronto da aggiungere al Document
     */
    public static LineSeparator newSolidSeparator(float width, Color color) {
        SolidLine line = new SolidLine(width);
        if (color != null) line.setColor(color);
        return new LineSeparator(line);
    }

    /**
     * Restituisce una label piccola in maiuscolo, usata come intestazione di campo.
     *
     * @param text  testo della label
     * @param font  font da usare
     * @param color colore del testo
     * @return Paragraph formattato
     */
    public static Paragraph smallLabel(String text, PdfFont font, Color color) {
        return new Paragraph(text.toUpperCase())
                .setFont(font).setFontSize(9).setFontColor(color)
                .setMarginBottom(2);
    }

    /**
     * Restituisce una cella senza bordo contenente l'elemento passato.
     *
     * @param element contenuto della cella
     * @return Cell senza bordo
     */
    public static Cell noBorderCell(IBlockElement element) {
        Cell c = new Cell().setBorder(Border.NO_BORDER);
        c.add(element);
        return c;
    }

    /**
     * Format "dd/MM/yyyy" locale IT (DateTimeFormatter è thread-safe).
     *
     * @param date data da formattare
     * @return stringa formattata in stile italiano
     */
    public static String formatDateIt(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Format valuta in locale italiano (NumberFormat non è thread-safe → istanza per chiamata).
     *
     * @param amount importo
     * @return stringa formattata come valuta italiana (es: 100,00 €)
     */
    public static String formatCurrencyIt(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return nf.format(amount);
    }

    /**
     * Carica un'immagine da path file system in modo sicuro.
     * Se path è null o il file non esiste, ritorna null. Converte il File in URL (file:///...).
     *
     * @param path     percorso del file immagine; se null o non esiste -> null
     * @param maxWidth larghezza massima (0 per ignorare)
     * @return Image oppure null se non caricabile
     */
    public static Image loadImageOrNull(String path, float maxWidth) {
        if (path == null) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            var data = ImageDataFactory.create(f.toURI().toURL());
            Image img = new Image(data);
            if (maxWidth > 0) img.setMaxWidth(maxWidth);
            return img;
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
