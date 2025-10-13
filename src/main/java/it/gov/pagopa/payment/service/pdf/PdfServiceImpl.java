package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.exception.custom.PdfGenerationException;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.utils.PdfUtils;
import it.gov.pagopa.payment.utils.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;

@Slf4j
@Service
public class PdfServiceImpl implements PdfService {

    private final BarCodePaymentService barCodePaymentService;
    private final DecryptRestConnector decryptRestConnector;
    private final ResourceLoader resourceLoader;

    private final String font;
    private final String logoMimit;
    private final String logoPari;
    private final String iconWasher;
    private final String iconHealthcard;
    private final String iconBarcode;


    public PdfServiceImpl(
            BarCodePaymentService barCodePaymentService, DecryptRestConnector decryptRestConnector,
            ResourceLoader resourceLoader,
            @Value("${pdf.font}") String font,
            @Value("${pdf.logoMimit}") String logoMimit,
            @Value("${pdf.logoPari}") String logoPari,
            @Value("${pdf.iconWasher}") String iconWasher,
            @Value("${pdf.iconHealthcard}") String iconHealthcard,
            @Value("${pdf.iconBarcode}") String iconBarcode
    ) {
        this.barCodePaymentService = barCodePaymentService;
        this.decryptRestConnector = decryptRestConnector;
        this.resourceLoader = resourceLoader;
        this.font = font;
        this.logoMimit = logoMimit;
        this.logoPari = logoPari;
        this.iconWasher = iconWasher;
        this.iconHealthcard = iconHealthcard;
        this.iconBarcode = iconBarcode;
    }

    /**
     * Genera il PDF del bonus elettrodomestici con intestazione, dati del beneficiario,
     * dettagli del bonus, codice a barre e istruzioni d'uso.
     *
     * @param initiativeId id dell'iniziativa
     * @param trxCode      codice della transazione
     * @param userId       id utente
     * @param username     nominativo intestatario
     * @param fiscalCode   codice fiscale intestatario
     * @return ReportDTO con il PDF codificato Base64
     */
    @Override
    public ReportDTO create(String initiativeId, String trxCode, String userId, String username, String fiscalCode) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            // Margini
            doc.setMargins(36, 36, 48, 36);

            // Font (con fallback gestito in PdfUtils)
            PdfFont regular = PdfUtils.loadPdfFont(font, false, resourceLoader);
            PdfFont bold = PdfUtils.loadPdfFont(font, true, resourceLoader);

            // Palette
            Color textPrimary   = new DeviceRgb(33, 37, 41);
            Color textSecondary = new DeviceGray(0.35f);
            Color brandBlue     = new DeviceRgb(0, 92, 230);

            // Header + separatore
            doc.add(buildHeader(regular, bold, textPrimary));
            doc.add(PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f))
                    .setMarginTop(6).setMarginBottom(18));

            // Dati transazione
            TransactionBarCodeResponse trxBarcode = barCodePaymentService.retriveVoucher(initiativeId, trxCode, userId);
            LocalDate createdDate = Utilities.getLocalDate(trxBarcode.getTrxDate());
            LocalDate validUntil  = Utilities.getLocalDate(trxBarcode.getTrxEndDate());
            BigDecimal amount     = CommonUtilities.centsToEuro(trxBarcode.getVoucherAmountCents());
            String barcodeTrxCode = trxBarcode.getTrxCode();
            String cf = getCf(userId, fiscalCode);

            // Sezioni principali
            doc.add(buildOwnerRow(username, cf, regular, bold, textPrimary, textSecondary));
            doc.add(PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f))
                    .setMarginTop(12).setMarginBottom(18));

            doc.add(buildDetailsAndAmount(createdDate, validUntil, amount, regular, bold, textPrimary, textSecondary));
            doc.add(buildBarcodeBlock(pdf, barcodeTrxCode, regular, textSecondary));
            doc.add(new Paragraph().setHeight(10));
            doc.add(buildHowToBox(regular, bold, textPrimary, textSecondary));
            doc.add(new Paragraph().setHeight(2));
            doc.add(buildPoweredByPari(regular, brandBlue));
            doc.add(buildFooter(bold, regular, textSecondary));

        } catch (IOException | RuntimeException e) {
            log.error("Errore durante la generazione del PDF (initiativeId={}, trxCode={}, userId={})",
                    Utilities.sanitizeString(initiativeId),
                    Utilities.sanitizeString(trxCode),
                    Utilities.sanitizeString(userId), e);
            throw new PdfGenerationException("Errore durante la generazione del PDF",true, e);
        }

        return ReportDTO.builder()
                .data(Base64.getEncoder().encodeToString(baos.toByteArray()))
                .build();
    }

    /**
     * Crea l'header compatto con logo MIMIT e testo ministeriale.
     */
    private BlockElement<?> buildHeader(PdfFont regular, PdfFont bold, Color textPrimary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{0.55f, 5.45f}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Image logo = PdfUtils.loadImageOrNull(this.logoMimit, 0, resourceLoader);
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        if (logo != null) {
            logo.setAutoScale(false).scaleToFit(45, 45).setHorizontalAlignment(HorizontalAlignment.LEFT);
            logoCell.add(logo);
        }
        t.addCell(logoCell);

        Div textWrap = new Div().setMargin(0).setPadding(0);
        textWrap.add(new Paragraph("BONUS ELETTRODOMESTICI")
                .setFont(bold).setFontSize(12).setFontColor(textPrimary)
                .setMarginTop(0).setMarginBottom(2));
        textWrap.add(new Paragraph("Ministero delle Imprese e del Made in Italy")
                .setFont(regular).setFontSize(10).setFontColor(textPrimary)
                .setMargin(0));

        Cell textCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(0).setPaddingLeft(6);
        textCell.add(textWrap);
        t.addCell(textCell);

        return t.setMarginBottom(6);
    }

    /**
     * Crea la riga con intestatario e codice fiscale.
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
     * Crea il blocco con i dettagli del bonus (date) e l'importo massimo.
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
        right.add(new Paragraph()
                .add("Puoi usare il bonus per ottenere uno sconto ")
                .add(new Text("fino al 30% sul prezzo d’acquisto di un solo elettrodomestico").setFont(bold))
                .add(", nuovo e ad alta efficienza.")
                .setFont(regular).setFontSize(10).setFontColor(textPrimary)
                .setMarginTop(2).setMarginBottom(12));

        t.addCell(PdfUtils.noBorderCell(left));
        t.addCell(PdfUtils.noBorderCell(right));
        return t.setMarginBottom(6);
    }

    /**
     * Crea il blocco del codice a barre (2 colonne: sinistra vuota, destra contenuto).
     */
    private BlockElement<?> buildBarcodeBlock(PdfDocument pdf, String code, PdfFont regular, Color textSecondary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        t.addCell(PdfUtils.noBorderCell(new Div()));

        Div right = new Div();

        right.add(new Paragraph("Codice a barre")
                .setFont(regular)
                .setFontSize(12)
                .setFontColor(textSecondary)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(10)
                .setMarginBottom(6));

        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCodeType(Barcode128.CODE128);
        barcode.setCode(code);
        barcode.setFont(regular);
        barcode.setSize(14);
        barcode.setBaseline(18);
        barcode.setBarHeight(42);
        barcode.setX(1.55f);

        Image barcodeImg = new Image(barcode.createFormXObject(pdf));
        barcodeImg.setAutoScale(false);
        barcodeImg.setHorizontalAlignment(HorizontalAlignment.LEFT);
        right.add(barcodeImg.setMarginTop(2));

        t.addCell(PdfUtils.noBorderCell(right));
        return t;
    }

    /**
     * Crea il box con le tre istruzioni su come usare il bonus.
     */
    private BlockElement<?> buildHowToBox(PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Div card = new Div()
                .setPadding(16)
                .setBorder(new SolidBorder(new DeviceGray(0.85f), 1f))
                .setBorderRadius(new BorderRadius(8))
                .setMarginTop(12)
                .setMarginBottom(30);

        card.add(new Paragraph("COME USARE IL BONUS")
                .setTextAlignment(TextAlignment.CENTER)
                .setFont(bold)
                .setFontSize(11)
                .setFontColor(textPrimary)
                .setMarginBottom(10));

        Table grid = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        var stepStyle = new PdfUtils.StepStyle(regular, bold, textPrimary, textSecondary, resourceLoader);

        grid.addCell(PdfUtils.stepCell(iconWasher, "Scegli l’elettrodomestico da sostituire",
                "Scegli quale elettrodomestico vuoi smaltire perché ormai vecchio o consuma troppa energia.",
                stepStyle));

        grid.addCell(PdfUtils.stepCell(iconHealthcard, "Porta con te la Tessera Sanitaria",
                "Mostrala, se richiesta, per eventuali controlli presso il punto vendita.",
                stepStyle));

        grid.addCell(PdfUtils.stepCell(iconBarcode, "Mostra il codice a barre",
                "Stampa questo buono o mostralo direttamente dal tuo dispositivo.",
                stepStyle));

        card.add(grid);
        return card;
    }

    /**
     * Crea la riga centrata "Powered by" con logo PARI.
     */
    private BlockElement<?> buildPoweredByPari(PdfFont regular, Color brandBlue) {
        Div box = new Div().setTextAlignment(TextAlignment.CENTER).setMarginTop(20);

        Image pari = PdfUtils.loadImageOrNull(logoPari, 0, resourceLoader);

        Table inline = new Table(new float[]{1, 1})
                .setBorder(Border.NO_BORDER)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        Cell left = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(0).setPaddingRight(8);

        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(0);

        left.add(new Paragraph("Powered by").setFont(regular).setFontSize(10).setMargin(0));

        if (pari != null) {
            pari.setAutoScale(false).scaleToFit(80, 20);
            right.add(pari);
        } else {
            right.add(new Paragraph("PARI").setFont(regular).setFontSize(12).setFontColor(brandBlue).setMargin(0));
        }

        inline.addCell(left);
        inline.addCell(right);

        box.add(inline);
        return box;
    }

    /**
     * Recupero il cf tramite chiamata al decrypt
     */
    private String getCf(String userId, String fiscalCode) {
        return fiscalCode == null ? decryptRestConnector.getPiiByToken(userId).getPii() : fiscalCode;
    }

    /**
     * Crea il paragrafo informativo finale su Pari/PagoPA.
     */
    private BlockElement<?> buildFooter(PdfFont bold, PdfFont regular, Color textSecondary) {
        return new Paragraph()
                .add(new Text("Pari ").setFont(bold))
                .add(new Text("è la piattaforma digitale, sviluppata da "))
                .add(new Text("PagoPA S.p.A").setFont(bold))
                .add(new Text(", che semplifica l'accesso a bonus e incentivi pubblici. La piattaforma permette di gestire tutti gli incentivi in un unico posto e di utilizzarli presso i commercianti convenzionati."))
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(textSecondary)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24);
    }
}
