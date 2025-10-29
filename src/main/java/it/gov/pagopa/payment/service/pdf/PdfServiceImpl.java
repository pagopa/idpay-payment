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
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.exception.custom.PdfGenerationException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.utils.PdfUtils;
import it.gov.pagopa.payment.utils.Utilities;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final DecryptRestConnector decryptRestConnector;
    private final ResourceLoader resourceLoader;

    private final String font;
    private final String logoMimit;
    private final String logoPari;
    private final String iconWasher;
    private final String iconHealthcard;
    private final String iconBarcode;


    public PdfServiceImpl(
            BarCodePaymentService barCodePaymentService,
        TransactionInProgressRepository transactionInProgressRepository, DecryptRestConnector decryptRestConnector,
            ResourceLoader resourceLoader,
            @Value("${pdf.font}") String font,
            @Value("${pdf.logoMimit}") String logoMimit,
            @Value("${pdf.logoPari}") String logoPari,
            @Value("${pdf.iconWasher}") String iconWasher,
            @Value("${pdf.iconHealthcard}") String iconHealthcard,
            @Value("${pdf.iconBarcode}") String iconBarcode
    ) {
        this.barCodePaymentService = barCodePaymentService;
      this.transactionInProgressRepository = transactionInProgressRepository;
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
            doc.add(buildPoweredByPari(regular, brandBlue, "Powered by"));
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

    @Override
    public ReportDTOWithTrxCode createPreauthPdf(String transactionId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String trxCode;
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

            Color textNote = new DeviceRgb(85, 92, 112);
            Color brandBlue     = new DeviceRgb(0, 92, 230);

            // Header + separatore
            doc.add(buildHeader(regular, bold, textPrimary));

            // Dati transazione
            Optional<TransactionInProgress> optionalTransactionInProgress = transactionInProgressRepository.findById(transactionId);
            TransactionInProgress transactionInProgress;

            // Controllo presenza transazione
            if (optionalTransactionInProgress.isEmpty()) {

                throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(transactionId));
            }
            transactionInProgress = optionalTransactionInProgress.get();

            LocalDate createdDate = Utilities.getLocalDate(transactionInProgress.getTrxDate());
            BigDecimal discount     = CommonUtilities.centsToEuro(transactionInProgress.getRewardCents());
            BigDecimal total     = CommonUtilities.centsToEuro(transactionInProgress.getEffectiveAmountCents());

            BigDecimal residualAmount     = total.subtract(discount);

            String prodotto = transactionInProgress.getAdditionalProperties().get("productName");
            String codiceProdotto = transactionInProgress.getAdditionalProperties().get("productGtin");
            trxCode = transactionInProgress.getTrxCode();
            String fiscalCode = decryptRestConnector.getPiiByToken(transactionInProgress.getUserId()).getPii();

            doc.add(buildDiscountRow(discount, regular, bold, textPrimary, textSecondary));

            Table cfAndDiscountBarcodeTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

            // Left
            cfAndDiscountBarcodeTable.addCell(buildBarcodeCell(pdf, fiscalCode,
                "Codice Fiscale del beneficiario", regular, textSecondary));
            // Right
            cfAndDiscountBarcodeTable.addCell(buildBarcodeCell(pdf, trxCode,
                "Codice sconto", regular, textSecondary));

            doc.add(cfAndDiscountBarcodeTable);

            doc.add(PdfUtils.newSolidSeparator(0.8f, new DeviceGray(0.85f))
                .setMarginTop(6).setMarginBottom(18));

            Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

            Cell leftCell = buildProductDetailsLeftCell(prodotto, createdDate, codiceProdotto, regular, bold, textPrimary, textSecondary);
            Cell rightCell = buildProductDetailsRightCell(pdf, total, residualAmount, regular, bold, textPrimary, textSecondary);

            t.addCell(leftCell);
            t.addCell(rightCell);

            doc.add(t.setMarginBottom(2));

            doc.add(new Paragraph().setHeight(10));
            doc.add(buildNotes(transactionInProgress.getId(), regular, textNote));

            doc.add(new Paragraph().setHeight(40));
            doc.add(buildPoweredByPari(regular, brandBlue, "Il Bonus Elettrodomestici è realizzato tramite"));
            doc.add(buildFooter(bold, regular, textSecondary));

        } catch (IOException | RuntimeException e) {

            if (e instanceof TransactionNotFoundOrExpiredException) {

                log.error("Errore durante la generazione del PDF (trxId={})",
                    Utilities.sanitizeString(transactionId), e);
              try {
                throw e;
              } catch (IOException ex) {
                  throw new PdfGenerationException("Errore durante la generazione del PDF",true, e);
              }
            }

            log.error("Errore durante la generazione del PDF (trxId={})",
                Utilities.sanitizeString(transactionId), e);
            throw new PdfGenerationException("Errore durante la generazione del PDF",true, e);
        }

        return ReportDTOWithTrxCode.builder()
            .data(Base64.getEncoder().encodeToString(baos.toByteArray()))
            .trxCode(trxCode)
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

    private BlockElement<?> buildNotes(String transactionId, PdfFont regular, Color textSecondary) {

        Table t = new Table(UnitValue.createPercentArray(new float[]{0.75f, 0.25f})).useAllAvailableWidth();

        Div left = new Div();
        left.add(PdfUtils.smallLabelOriginalCase("Note:", regular, textSecondary).setFontSize(8));
        left.add(PdfUtils.smallLabelOriginalCase("ID transazione: " + transactionId, regular, textSecondary).setFontSize(8));

        t.addCell(PdfUtils.noBorderCell(left));
        return t.setMarginBottom(6);
    }

    /**
     * Crea la riga con codice sconto.
     */
    private BlockElement<?> buildDiscountRow(BigDecimal importoSconto,
        PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

        Div left = new Div();

        Div right = new Div();
        right.add(PdfUtils.smallLabelOriginalCase("Sconto", regular, textSecondary).setMarginTop(25f));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(importoSconto)).setFont(bold).setFontSize(20).setFontColor(textPrimary).setMarginBottom(6).setMarginTop(-5));

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
     * Costruisce la cella sinistra del dettaglio prodotto
     */
    private Cell buildProductDetailsLeftCell(String prodotto, LocalDate dataDiEmissione, String productGtin,
        PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Div left = new Div();
        left.add(new Paragraph("COSA STAI ACQUISTANDO").setFont(bold).setFontSize(11).setFontColor(textPrimary).setMarginBottom(10));
        left.add(PdfUtils.smallLabelOriginalCase("Prodotto", regular, textSecondary));
        left.add(new Paragraph(prodotto).setFont(bold).setFontSize(12).setMarginBottom(10));
        left.add(PdfUtils.smallLabelOriginalCase("Data di emissione", regular, textSecondary));
        left.add(new Paragraph(PdfUtils.formatDateIt(dataDiEmissione)).setFont(bold).setFontSize(12));
        left.add(PdfUtils.smallLabelOriginalCase("GTIN prodotto", regular, textSecondary));
        left.add(new Paragraph(productGtin.toUpperCase()).setFont(bold).setFontSize(12));

        return PdfUtils.noBorderCell(left);
    }

    /**
     * Costruisce la cella destra del dettaglio prodotto
     */
    private Cell buildProductDetailsRightCell(PdfDocument pdf, BigDecimal importo, BigDecimal spesaFinale,
        PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Div right = new Div();
        right.add(new Paragraph().setHeight(15));
        right.add(PdfUtils.smallLabelOriginalCase("Importo da scontare", regular, textSecondary));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(importo)).setFont(bold).setFontSize(20).setFontColor(textPrimary).setMarginBottom(-5).setMarginTop(-5));

        // Adjust decimal to italian currency format and add barcode
        addProductBarcodeDiv(pdf, importo.toString().replace(".", ","), right, textSecondary, regular);

        right.add(PdfUtils.smallLabelOriginalCase("Spesa finale", regular, textSecondary).setMarginTop(18));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(spesaFinale)).setFont(bold).setFontSize(20).setFontColor(textPrimary).setMarginBottom(-5).setMarginTop(-5));

        // Adjust decimal to italian currency format and add barcode
        addProductBarcodeDiv(pdf, spesaFinale.toString().replace(".", ","), right, textSecondary, regular);

        return PdfUtils.noBorderCell(right);
    }

    /**
     * Crea il blocco con i dettagli del prodotto (data) e l'importo dello sconto.
     */
    private BlockElement<?> buildProductDetailsAndDiscount(PdfDocument pdf, String productGtin, String prodotto,
        LocalDate dataDiEmissione, BigDecimal importo, BigDecimal spesaFinale,
        PdfFont regular, PdfFont bold, Color textPrimary, Color textSecondary) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

        Div left = new Div();
        left.add(new Paragraph("COSA STAI ACQUISTANDO").setFont(bold).setFontSize(11).setFontColor(textPrimary).setMarginBottom(10));
        left.add(PdfUtils.smallLabelOriginalCase("Prodotto", regular, textSecondary));
        left.add(new Paragraph(prodotto).setFont(bold).setFontSize(12).setMarginBottom(10));
        left.add(PdfUtils.smallLabelOriginalCase("Data di emissione", regular, textSecondary));
        left.add(new Paragraph(PdfUtils.formatDateIt(dataDiEmissione)).setFont(bold).setFontSize(12));

        left.add(PdfUtils.smallLabelOriginalCase("GTIN prodotto", regular, textSecondary));
        left.add(new Paragraph(productGtin.toUpperCase()).setFont(bold).setFontSize(12));

        Div right = new Div();

        right.add(new Paragraph().setHeight(15));
        right.add(PdfUtils.smallLabelOriginalCase("Importo da scontare", regular, textSecondary));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(importo)).setFont(bold).setFontSize(20).setFontColor(textPrimary).setMarginBottom(-5).setMarginTop(-5));

        // Adjust decimal to italian currency format and add barcode
        addProductBarcodeDiv(pdf, importo.toString().replace(".", ","), right, textSecondary, regular);

        right.add(PdfUtils.smallLabelOriginalCase("Spesa finale", regular, textSecondary).setMarginTop(18));
        right.add(new Paragraph(PdfUtils.formatCurrencyIt(spesaFinale)).setFont(bold).setFontSize(20).setFontColor(textPrimary).setMarginBottom(-5).setMarginTop(-5));

        // Adjust decimal to italian currency format and add barcode
        addProductBarcodeDiv(pdf, spesaFinale.toString().replace(".", ","), right, textSecondary, regular);

        t.addCell(PdfUtils.noBorderCell(left));
        t.addCell(PdfUtils.noBorderCell(right));
        return t.setMarginBottom(2);
    }

    /**
     * Aggiunge ad un blocco il codice a barre del prodotto
     */
    private void addProductBarcodeDiv(PdfDocument pdf, String productGtin, Div div, Color textSecondary, PdfFont regular) {

        div.add(new Paragraph("Codice a barre")
            .setFont(regular)
            .setFontSize(8)
            .setFontColor(textSecondary)
            .setTextAlignment(TextAlignment.LEFT)
            .setMarginTop(10)
            .setMarginBottom(6));

        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCodeType(Barcode128.CODE128);
        barcode.setCode(productGtin);
        barcode.setFont(null);
        barcode.setSize(0f);
        barcode.setBaseline(0f);
        barcode.setBarHeight(25.5F);

        Image barcodeImg = new Image(barcode.createFormXObject(pdf));
        barcodeImg.setAutoScale(false);
        barcodeImg.setHorizontalAlignment(HorizontalAlignment.LEFT);
        div.add(barcodeImg.setMarginTop(2));
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
     * Crea una cella codice a barre
     */
    private Cell buildBarcodeCell(PdfDocument pdf, String code, String title, PdfFont regular, Color textSecondary) {

        Div div = new Div();

        div.add(PdfUtils.smallLabelOriginalCase(title, regular, textSecondary));

        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCodeType(Barcode128.CODE128);
        barcode.setCode(code);
        barcode.setFont(null);
        barcode.setSize(0f);
        barcode.setBaseline(0f);
        barcode.setBarHeight(25.5F);

        Image barcodeImg = new Image(barcode.createFormXObject(pdf));
        barcodeImg.setAutoScale(false);
        barcodeImg.setHorizontalAlignment(HorizontalAlignment.LEFT);
        div.add(barcodeImg.setMarginTop(2));

        div.add(new Paragraph(code.toUpperCase()).setFont(regular).setFontSize(12)).setMarginTop(20);

        return PdfUtils.noBorderCell(div);
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
    private BlockElement<?> buildPoweredByPari(PdfFont regular, Color brandBlue, String text) {
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

        left.add(new Paragraph(text).setFont(regular).setFontSize(10).setMargin(0));

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
        return StringUtils.isNotBlank(fiscalCode) ? fiscalCode : decryptRestConnector.getPiiByToken(userId).getPii();
    }

    /**
     * Crea il paragrafo informativo finale su Pari/PagoPA.
     */
    private BlockElement<?> buildFooter(PdfFont bold, PdfFont regular, Color textSecondary) {
        return new Paragraph()
                .add(new Text("PARI ").setFont(bold))
                .add(new Text("è la piattaforma digitale, sviluppata da "))
                .add(new Text("PagoPA S.p.A").setFont(bold))
                .add(new Text(", che semplifica l'accesso a bonus e incentivi pubblici. La \npiattaforma permette di gestire tutti gli incentivi in un unico posto e di utilizzarli presso i commercianti convenzionati."))
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(textSecondary)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(24);
    }
}
