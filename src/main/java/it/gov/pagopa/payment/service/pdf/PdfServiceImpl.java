package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.barcodes.Barcode128;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.utils.PdfUtils;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class PdfServiceImpl implements PdfService {

    private final BarCodePaymentService barCodePaymentService;

    private final String devPortalLink;
    private final String font;
    private final String logoMimit;
    private final String logoPari;
    private final String iconWasher;
    private final String iconHealthcard;
    private final String iconBarcode;

    public PdfServiceImpl(
            BarCodePaymentService barCodePaymentService,
            @Value("${pdf.devPortalLink:https://developer.pagopa.it/pari/overview}") String devPortalLink,
            @Value("${pdf.font:DejaVuSans.ttf}") String font,
            @Value("${pdf.logoMimit:}") String logoMimit,
            @Value("${pdf.logoPari:}") String logoPari,
            @Value("${pdf.iconWasher:}") String iconWasher,
            @Value("${pdf.iconHealthcard:}") String iconHealthcard,
            @Value("${pdf.iconBarcode:}") String iconBarcode
    ) {
        this.barCodePaymentService = barCodePaymentService;
        this.devPortalLink = devPortalLink;
        this.font = font;
        this.logoMimit = logoMimit;
        this.logoPari = logoPari;
        this.iconWasher = iconWasher;
        this.iconHealthcard = iconHealthcard;
        this.iconBarcode = iconBarcode;
    }

    /**
     * Genera il PDF del bonus e lo restituisce come array di byte.
     * <p>
     * Crea un PdfWriter su un ByteArrayOutputStream, compone il layout con iText (A4, margini, header,
     * sezioni e barcode) e chiude le risorse prima di serializzare i byte.
     *
     * @param initiativeId identificativo iniziativa (opzionale, per logging/telemetria)
     * @param trxCode        identificativo transazione (opzionale, per logging/telemetria)
     * @param userId       identificativo utente (opzionale, per logging/telemetria)
     * @return bytes del PDF generato; mai null
     * @throws RuntimeException se la generazione fallisce per IO/layout
     */
    @Override
    public byte[] create(String initiativeId, String trxCode, String userId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 48, 36);

            PdfFont regular = (new File(font).exists())
                    ? PdfFontFactory.createFont(font, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
                    : PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = (new File(font).exists())
                    ? PdfFontFactory.createFont(font, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
                    : PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            Color textPrimary = new DeviceRgb(33, 37, 41);
            Color textSecondary = new DeviceGray(0.35f);
            Color brandBlue   = new DeviceRgb(0, 92, 230);
            Color lightGrayBg = new DeviceRgb(247, 248, 250);

            // Header
            doc.add(buildHeader(regular, bold, textPrimary, brandBlue));
            doc.add(PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f))
                    .setMarginTop(6).setMarginBottom(18));


            TransactionBarCodeResponse trxBarcode = barCodePaymentService.retriveVoucher(initiativeId,trxCode,userId);
            String intestatario  = "Giovanna Beltramin";// TODO: cablato in attesa di capire da dove recuperarli
            String cf            = "BLTGVN78A52C409X";// TODO: cablato in attesa di capire da dove recuperarlo
            LocalDate emessoIl   = Utilities.getLocalDate(trxBarcode.getTrxDate());
            LocalDate validoFino = Utilities.getLocalDate(trxBarcode.getTrxEndDate());
            BigDecimal importo   = new BigDecimal("100.00"); // TODO: effetuare merge da develop dopo aggiunta voucherAmount e fare diviso cento
            String codice        = trxBarcode.getTrxCode();

            doc.add(buildOwnerRow(intestatario, cf, regular, bold, textPrimary, textSecondary));
            doc.add(PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f))
                    .setMarginTop(12).setMarginBottom(18));

            doc.add(buildDetailsAndAmount(emessoIl, validoFino, importo, regular, bold, textPrimary, textSecondary));
            doc.add(buildBarcodeBlock(pdf, codice, regular, textSecondary));
            doc.add(new Paragraph().setHeight(18));
            doc.add(buildHowToBox(regular, bold, textPrimary, textSecondary, lightGrayBg));

            doc.add(new Paragraph("Pari è un progetto dolor sit amet, consectetur adipiscing elit. Aenean commodo ligula eget dolor. "
                    + "Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes. ")
                    .add(new Link("Link al dev portal", PdfAction.createURI(devPortalLink))
                            .setFontColor(brandBlue)
                            .setUnderline()
                    )
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(textSecondary)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(24));

        } catch (Exception e) {
            throw new RuntimeException("Errore durante la generazione del PDF "
                    + "(initiativeId=" + initiativeId + ", trxCode=" + trxCode + ", userId=" + userId + ")", e);
        }
        return baos.toByteArray();
    }

    /**
     * Costruisce l'header con loghi (sx/dx) e titoli/iniziativa.
     *
     * @param regular     font regolare
     * @param bold        font bold
     * @param textPrimary colore primario del testo
     * @param brandBlue   colore brand per "PARI"
     * @return BlockElement da aggiungere al Document
     */
    private BlockElement<?> buildHeader(PdfFont regular, PdfFont bold, Color textPrimary, Color brandBlue) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

        Div left = new Div();
        Image logoSx = PdfUtils.loadImageOrNull(this.logoMimit, 80);
        if (logoSx != null) left.add(logoSx.setMarginBottom(8));
        left.add(new Paragraph("BONUS ELETTRODOMESTICI").setFont(bold).setFontSize(12).setFontColor(textPrimary).setMarginBottom(2));
        left.add(new Paragraph("Ministero delle Imprese del Made in Italy").setFont(regular).setFontSize(10).setFontColor(textPrimary));

        Div right = new Div().setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("Iniziativa fornita da").setFont(regular).setFontSize(10).setFontColor(textPrimary).setMarginBottom(2));
        right.add(new Paragraph("PARI").setFont(bold).setFontSize(20).setFontColor(brandBlue));
        Image logoDx = PdfUtils.loadImageOrNull(logoPari, 0);
        if (logoDx != null) right.add(logoDx.setAutoScale(true));

        t.addCell(PdfUtils.noBorderCell(left));
        t.addCell(PdfUtils.noBorderCell(right));
        return t.setMarginBottom(6);
    }

    /**
     * Costruisce la riga con intestatario e codice fiscale, allineati a sx/dx.
     */
    private BlockElement<?> buildOwnerRow(String intestatario, String cf,
                                          PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

        Div left = new Div();
        left.add(PdfUtils.smallLabel("Bonus intestato a", regular, textSecondary));
        left.add(new Paragraph(intestatario).setFont(bold).setFontSize(12).setFontColor(textPrimary));

        Div right = new Div().setTextAlignment(TextAlignment.RIGHT);
        right.add(PdfUtils.smallLabel("Codice Fiscale del titolare", regular, textSecondary));
        right.add(new Paragraph(cf).setFont(bold).setFontSize(12).setFontColor(textPrimary));

        t.addCell(PdfUtils.noBorderCell(left));
        t.addCell(PdfUtils.noBorderCell(right));
        return t.setMarginBottom(6);
    }

    /**
     * Costruisce la sezione "Dettagli del bonus" a due colonne (date a sx, importo e descrizione a dx).
     */
    private BlockElement<?> buildDetailsAndAmount(LocalDate emessoIl, LocalDate validoFinoAl, BigDecimal importo,
                                                  PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

        Div left = new Div();
        left.add(new Paragraph("DETTAGLI DEL BONUS").setFont(bold).setFontSize(11).setFontColor(textPrimary).setMarginBottom(10));
        left.add(PdfUtils.smallLabel("Bonus emesso il", regular, textSecondary));
        left.add(new Paragraph(PdfUtils.formatDateIt(emessoIl)).setFont(bold).setFontSize(12).setMarginBottom(10));
        left.add(PdfUtils.smallLabel("Valido fino al", regular, textSecondary));
        left.add(new Paragraph(PdfUtils.formatDateIt(validoFinoAl)).setFont(bold).setFontSize(12));

        Div right = new Div();
        right.add(PdfUtils.smallLabel("Importo massimo disponibile", regular, textSecondary));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(importo)).setFont(bold).setFontSize(26).setFontColor(textPrimary).setMarginBottom(6));
        right.add(new Paragraph("Puoi usare il bonus per ottenere uno sconto ")
                .add(new Text("fino al 30%").setFont(bold))
                .add(" sul prezzo d’acquisto di ")
                .add(new Text("un solo elettrodomestico").setFont(bold))
                .add(", nuovo e ad alta efficienza.")
                .setFont(regular).setFontSize(10).setFontColor(textPrimary)
                .setMarginTop(2).setMarginBottom(12));

        t.addCell(PdfUtils.noBorderCell(left));
        t.addCell(PdfUtils.noBorderCell(right));
        return t.setMarginBottom(6);
    }

    /**
     * Costruisce il blocco con barcode Code128 e numero visibile.
     */
    private BlockElement<?> buildBarcodeBlock(PdfDocument pdf, String code, PdfFont regular, Color textSecondary) {
        Div wrap = new Div().setTextAlignment(TextAlignment.CENTER);

        wrap.add(new Paragraph("CODICE A BARRE")
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(textSecondary)
                .setMarginBottom(6));

        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCodeType(Barcode128.CODE128);
        barcode.setCode(code);

        Image barcodeImg = new Image(barcode.createFormXObject(pdf));
        barcodeImg.setAutoScale(false);
        barcodeImg.setWidth(UnitValue.createPercentValue(36));
        barcodeImg.setHorizontalAlignment(HorizontalAlignment.CENTER);

        wrap.add(barcodeImg.setMarginTop(2));

        return wrap;
    }

    /**
     * Costruisce la card "Come usare il bonus" con tre colonne (icona, titolo, descrizione).
     */
    private BlockElement<?> buildHowToBox(PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary, Color lightGrayBg) {
        Div card = new Div()
                .setBackgroundColor(lightGrayBg)
                .setPadding(16)
                .setBorder(new SolidBorder(new DeviceGray(0.85f), 0.8f))
                .setBorderRadius(new BorderRadius(8))
                .setMarginTop(12);

        card.add(new Paragraph("COME USARE IL BONUS")
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(bold).setFontSize(11).setFontColor(textPrimary).setMarginBottom(14));

        Table grid = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        grid.setBorder(Border.NO_BORDER);

        grid.addCell(stepCell(iconWasher, "Scegli l’elettrodomestico da sostituire",
                "Scegli quale elettrodomestico vuoi smaltire perché ormai vecchio o consuma troppa energia.",
                regular, bold, textPrimary, textSecondary));
        grid.addCell(stepCell(iconHealthcard, "Porta con te la Tessera Sanitaria",
                "Mostrala, se richiesta, per eventuali controlli presso il punto vendita.",
                regular, bold, textPrimary, textSecondary));
        grid.addCell(stepCell(iconBarcode, "Mostra il codice a barre",
                "Stampa questo buono o mostralo direttamente dal tuo dispositivo.",
                regular, bold, textPrimary, textSecondary));

        card.add(grid);
        return card;
    }

    /**
     * Rende una cella del box "Come usare il bonus" con icona (o placeholder), titolo e descrizione centrati.
     */
    private Cell stepCell(String iconPath, String heading, String desc,
                          PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Cell c = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);

        Div d = new Div().setWidth(UnitValue.createPercentValue(100));

        Image icon = PdfUtils.loadImageOrNull(iconPath, 0);
        if (icon != null) {
            icon.setAutoScale(true)
                    .setMaxHeight(32)
                    .setMarginBottom(6)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            d.add(icon);
        } else {
            Div ph = new Div()
                    .setWidth(36).setHeight(36)
                    .setBackgroundColor(new DeviceGray(0.9f))
                    .setBorderRadius(new BorderRadius(18))
                    .setMarginBottom(6)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            d.add(ph);
        }

        d.add(new Paragraph(heading)
                .setFont(bold).setFontSize(10).setFontColor(textPrimary)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        d.add(new Paragraph(desc)
                .setFont(regular).setFontSize(9).setFontColor(textSecondary)
                .setTextAlignment(TextAlignment.CENTER));

        c.add(d);
        return c;
    }
}
