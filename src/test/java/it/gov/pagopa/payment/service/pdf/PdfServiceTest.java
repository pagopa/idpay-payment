package it.gov.pagopa.payment.service.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import freemarker.template.Configuration;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeResponse;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InitiativeNotfoundException;
import it.gov.pagopa.payment.exception.custom.PdfGenerationException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.BarCodePaymentService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private BarCodePaymentService barCodePaymentService;

    @Mock
    private DecryptRestConnector decryptRestConnector;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private TransactionBarCodeResponse trxResp;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RewardRuleRepository rewardRuleRepository;

    private final Configuration freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_32);


    private PdfServiceImpl newService() {
        return new PdfServiceImpl(
                barCodePaymentService,
                transactionRepository,
                rewardRuleRepository,
                decryptRestConnector,
                resourceLoader,
                freemarkerConfiguration,
                "DejaVuSans.ttf",
                null,
                null,
                60L
        );
    }

    private void mockTemplateResources(String templateName) throws Exception {
        String templatePath = "classpath:pdf/voucher/%s/voucher.html".formatted(templateName);
        Resource templateRes = mock(Resource.class);
        when(templateRes.exists()).thenReturn(true);
        when(resourceLoader.getResource(templatePath)).thenReturn(templateRes);

        String basePath = "classpath:pdf/voucher/%s/".formatted(templateName);
        Resource baseRes = mock(Resource.class);
        java.net.URL dummyUrl = getClass().getResource("/pdf/voucher/%s/".formatted(templateName));
        if (dummyUrl == null) {
            dummyUrl = java.net.URI.create("file:///dummy/path/").toURL();
        }
        when(baseRes.getURL()).thenReturn(dummyUrl);
        when(resourceLoader.getResource(basePath)).thenReturn(baseRes);
    }

    private void mockRewardRule(String initiativeId, String templateName, String logoUrl) {
        it.gov.pagopa.payment.model.InitiativeConfig config = it.gov.pagopa.payment.model.InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeName("Bonus Elettrodomestici")
                .pdfVoucherTemplate(templateName)
                .pdfLogoURL(logoUrl)
                .build();
        it.gov.pagopa.payment.model.RewardRule rewardRule = it.gov.pagopa.payment.model.RewardRule.builder()
                .id(initiativeId)
                .initiativeConfig(config)
                .build();
        when(rewardRuleRepository.findById(initiativeId)).thenReturn(Optional.of(rewardRule));
    }

    @Test
    void create_shouldReturnValidPdfBase64_withBel25Template() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(trxResp.getVoucherAmountCents()).thenReturn(10_00L);
        when(trxResp.getInitiativeName()).thenReturn("Bonus Elettrodomestici");
        when(barCodePaymentService.retriveVoucher("INIT1", "TRX1", "USER1")).thenReturn(trxResp);

        mockRewardRule("INIT1", "BEL25", null);
        mockTemplateResources("BEL25");

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
    void create_shouldContainExpectedTextsAndValues() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("12345678");
        when(trxResp.getVoucherAmountCents()).thenReturn(70_00L); // 70,00 €
        when(trxResp.getInitiativeName()).thenReturn("Bonus Elettrodomestici");
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        mockRewardRule("INIT1", "BEL25", null);
        mockTemplateResources("BEL25");

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INIT1", "TRX1", "USER1", "Giovanna Beltramin", "BLTGVN78A52C409X");
        byte[] bytes = Base64.getDecoder().decode(report.getData());

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String page1Text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            String norm = normalize(page1Text);

            assertTrue(norm.contains("BONUS ELETTRODOMESTICI"), () -> "Missing 'BONUS ELETTRODOMESTICI' in:\n" + norm);
            assertTrue(norm.contains("MINISTERO") && norm.contains("IMPRESE") && norm.contains("MADE") && norm.contains("ITALY"),
                    () -> "Missing ministero line keywords in:\n" + norm);

            assertTrue(norm.contains("GIOVANNA") && norm.contains("BELTRAMIN"), () -> "Missing name in:\n" + norm);
            assertTrue(norm.contains("BLTGVN78A52C409X"), () -> "Missing CF in:\n" + norm);
            assertTrue(norm.contains("DETTAGLI DEL BONUS".replaceAll("[^A-Z0-9]+", " ")), () -> "Missing 'DETTAGLI DEL BONUS' in:\n" + norm);

            assertTrue(norm.contains("70 00") || norm.contains("70"), () -> "Missing amount in:\n" + norm);
            assertTrue(norm.contains("12345678"), () -> "Missing barcode code in:\n" + norm);

            assertTrue(norm.contains("COME USARE IL BONUS".replaceAll("[^A-Z0-9]+", " ")), () -> "Missing 'COME USARE IL BONUS' in:\n" + norm);
            assertTrue(norm.contains("SCEGLI") && norm.contains("ELETTRODOMESTICO"), () -> "Missing step 1 in:\n" + norm);
            assertTrue(norm.contains("PORTA") && norm.contains("TESSERA") && norm.contains("SANITARIA"), () -> "Missing step 2 in:\n" + norm);
            assertTrue(norm.contains("MOSTRA") && norm.contains("CODICE") && norm.contains("BARRE"), () -> "Missing step 3 in:\n" + norm);

            assertTrue(norm.contains("PARI"), () -> "Missing 'PARI' in:\n" + norm);
            assertTrue(norm.contains("PAGOPA") && (norm.contains("SPA") || norm.contains("S P A")), () -> "Missing 'PagoPA S.p.A.' in:\n" + norm);
        }
    }

    @Test
    void create_whenVoucherTemplateIsDefault_shouldGeneratePdfWithDefaultTemplate() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-01-02T08:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-31T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("DEF12345");
        when(trxResp.getVoucherAmountCents()).thenReturn(50_00L);
        when(trxResp.getInitiativeName()).thenReturn("Iniziativa Default");
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        mockRewardRule("INIT_DEF", null, null); // pdfVoucherTemplate = null -> fallback a DEFAULT
        mockTemplateResources("DEFAULT");

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INIT_DEF", "TRX_DEF", "USER_DEF", "Mario Rossi", "RSSMRA80A01H501Z");

        assertNotNull(report);
        byte[] bytes = Base64.getDecoder().decode(report.getData());
        assertTrue(bytes.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String norm = normalize(PdfTextExtractor.getTextFromPage(pdf.getFirstPage()));
            assertTrue(norm.contains("INIZIATIVA DEFAULT"));
            assertTrue(norm.contains("MARIO") && norm.contains("ROSSI"));
            assertTrue(norm.contains("RSSMRA80A01H501Z"));
            assertTrue(norm.contains("DEF12345"));
        }
    }

    @Test
    void create_whenFiscalCodeNull_shouldRetrievePiiFromDecryptRestConnector() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("TRXCODE8");
        when(trxResp.getVoucherAmountCents()).thenReturn(10_00L);
        when(trxResp.getInitiativeName()).thenReturn("Bonus Elettrodomestici");
        when(barCodePaymentService.retriveVoucher("INIT1", "TRX1", "USER1")).thenReturn(trxResp);

        mockRewardRule("INIT1", "BEL25", null);
        mockTemplateResources("BEL25");

        when(decryptRestConnector.getPiiByToken("USER1")).thenReturn(new DecryptCfDTO("DECRYPTEDCF99A01H"));

        PdfServiceImpl svc = newService();

        ReportDTO report = svc.create("INIT1", "TRX1", "USER1", "Laura Bianchi", null);

        assertNotNull(report);
        verify(decryptRestConnector).getPiiByToken("USER1");

        byte[] bytes = Base64.getDecoder().decode(report.getData());
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String norm = normalize(PdfTextExtractor.getTextFromPage(pdf.getFirstPage()));
            assertTrue(norm.contains("DECRYPTEDCF99A01H"));
        }
    }

    @Test
    void create_whenInitiativeNotFound_shouldThrowPdfGenerationExceptionWrappingInitiativeNotFoundException() {
        String initiativeId = "INIT_NOT_FOUND";
        String trxCode = "TRX_CODE";
        String userId = "USER_ID";

        TransactionBarCodeResponse trx = TransactionBarCodeResponse.builder()
                .id("transaction-id-123")
                .trxCode(trxCode)
                .initiativeId(initiativeId)
                .initiativeName("Bonus Elettrodomestici")
                .trxDate(OffsetDateTime.parse("2026-11-16T10:30:00+01:00"))
                .status(SyncTrxStatus.CREATED)
                .trxExpirationSeconds(3600L)
                .residualBudgetCents(5000L)
                .trxEndDate(OffsetDateTime.parse("2026-12-16T23:59:59+01:00"))
                .voucherAmountCents(7000L)
                .build();
        when(barCodePaymentService.retriveVoucher(initiativeId, trxCode, userId)).thenReturn(trx);
        when(rewardRuleRepository.findById(initiativeId)).thenReturn(Optional.empty());

        PdfServiceImpl svc = newService();

        PdfGenerationException ex = assertThrows(PdfGenerationException.class,
                () -> svc.create(initiativeId, trxCode, userId, "Mario Rossi", "RSSMRA80A01H501Z"));

        assertEquals("Error while generating PDF HTML", ex.getMessage());
        assertInstanceOf(InitiativeNotfoundException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("INIT_NOT_FOUND"));
    }

    @Test
    void create_whenTemplateFileDoesNotExist_shouldThrowPdfGenerationException() {
        String initiativeId = "INIT1";
        String trxCode = "TRX_CODE";
        String userId = "USER_ID";

        TransactionBarCodeResponse trx = TransactionBarCodeResponse.builder()
                .id("transaction-id-123")
                .trxCode(trxCode)
                .initiativeId(initiativeId)
                .initiativeName("Bonus Elettrodomestici")
                .trxDate(OffsetDateTime.parse("2026-11-16T10:30:00+01:00"))
                .status(SyncTrxStatus.CREATED)
                .trxExpirationSeconds(3600L)
                .residualBudgetCents(5000L)
                .trxEndDate(OffsetDateTime.parse("2026-12-16T23:59:59+01:00"))
                .voucherAmountCents(7000L)
                .build();
        when(barCodePaymentService.retriveVoucher(initiativeId, trxCode, userId)).thenReturn(trx);
        mockRewardRule(initiativeId, "BEL25", null);

        String templatePath = "classpath:pdf/voucher/BEL25/voucher.html";
        Resource templateRes = mock(Resource.class);
        when(templateRes.exists()).thenReturn(false);
        when(resourceLoader.getResource(templatePath)).thenReturn(templateRes);

        PdfServiceImpl svc = newService();

        PdfGenerationException ex = assertThrows(PdfGenerationException.class,
                () -> svc.create(initiativeId, trxCode, userId, "Mario Rossi", "RSSMRA80A01H501Z"));

        assertEquals("Error while generating PDF HTML", ex.getMessage());
        assertInstanceOf(PdfGenerationException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Voucher template voucher.html not found"));
    }

    @Test
    void create_whenBarcodeServiceThrows_shouldWrapInPdfGenerationException() {
        when(barCodePaymentService.retriveVoucher(any(), any(), any()))
                .thenThrow(new IllegalStateException("Barcode service unavailable"));

        PdfServiceImpl svc = newService();

        PdfGenerationException ex = assertThrows(PdfGenerationException.class,
                () -> svc.create("INIT1", "TRX1", "USER1", "Mario Rossi", "RSSMRA80A01H501Z"));

        assertEquals("Error while generating PDF HTML", ex.getMessage());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Barcode service unavailable", ex.getCause().getMessage());
    }

    @Test
    void create_whenLogoUrlProvided_shouldCacheLogoInSubsequentCalls() throws Exception {
        when(trxResp.getTrxDate()).thenReturn(OffsetDateTime.parse("2025-11-23T10:00:00Z"));
        when(trxResp.getTrxEndDate()).thenReturn(OffsetDateTime.parse("2025-12-03T23:59:59Z"));
        when(trxResp.getTrxCode()).thenReturn("TRXCODE8");
        when(trxResp.getVoucherAmountCents()).thenReturn(10_00L);
        when(trxResp.getInitiativeName()).thenReturn("Bonus Elettrodomestici");
        when(barCodePaymentService.retriveVoucher(any(), any(), any())).thenReturn(trxResp);

        String logoClasspath = "classpath:static/icons/test_logo.png";
        mockRewardRule("INIT1", "BEL25", logoClasspath);
        mockTemplateResources("BEL25");

        Resource logoRes = mock(Resource.class);
        when(logoRes.exists()).thenReturn(true);
        when(logoRes.getInputStream()).thenReturn(new ByteArrayInputStream(tinyPng()));
        when(resourceLoader.getResource(logoClasspath)).thenReturn(logoRes);

        PdfServiceImpl svc = newService();

        // Prima chiamata: legge il logo da resourceLoader e lo mette in cache
        ReportDTO report1 = svc.create("INIT1", "TRX1", "USER1", "Laura Bianchi", "BNCLRA80A01H501X");
        assertNotNull(report1);

        // Seconda chiamata: deve usare la cache in memoria, senza invocare di nuovo getInputStream() sulla risorsa logo
        ReportDTO report2 = svc.create("INIT1", "TRX1", "USER1", "Laura Bianchi", "BNCLRA80A01H501X");
        assertNotNull(report2);

        // Verifica che il logo sia stato letto una sola volta dalla risorsa
        verify(logoRes, times(1)).getInputStream();
    }

    @Test
    void createPreauthPdf_happyPath_shouldGeneratePdfAndReturnDto() {
        String initiativeId = "INITIATIVEID1";
        String transactionId = "PREAUTH_TRX_ID_001";
        String trxCode = "PREAUTHCODE001";
        String userId = "USER_ID_FOR_DECRYPT";
        String fiscalCode = "MRARSS80A01H501Z";
        String productGtin = "123456789012";

        Transaction mockTrx = createMockTransactionInProgress(
                transactionId, trxCode, userId, 3000L, 10000L, "Prodotto Test");

        Map<String, String> properties = new HashMap<>(mockTrx.getAdditionalProperties());
        properties.put("productGtin", productGtin);
        mockTrx.setAdditionalProperties(properties);
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTrx));
        when(decryptRestConnector.getPiiByToken(userId)).thenReturn(new DecryptCfDTO(fiscalCode));

        PdfServiceImpl svc = newService();
        ReportDTOWithTrxCode result = svc.createPreauthPdf(initiativeId, transactionId);

        assertNotNull(result);
        assertEquals(trxCode, result.getTrxCode());
        assertNotNull(result.getData());

        byte[] pdfBytes = Base64.getDecoder().decode(result.getData());
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith("%PDF-"));

        verify(transactionRepository).findById(transactionId);
        verify(decryptRestConnector).getPiiByToken(userId);
    }

    @Test
    void createPreauthPdf_whenTransactionNotFound_shouldThrowException() {
        String initiativeId = "INITIATIVEID1";
        String transactionId = "NON_EXISTENT_TRX_ID";
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        PdfServiceImpl svc = newService();

        TransactionNotFoundOrExpiredException ex = assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> svc.createPreauthPdf(initiativeId, transactionId));

        assertEquals("Cannot find transaction with transactionId [%s]".formatted(transactionId), ex.getMessage());
        verify(transactionRepository).findById(transactionId);
        verifyNoInteractions(decryptRestConnector);
    }

    @Test
    void createPreauthPdf_whenInitiativeMismatch_shouldThrowException() {
        String transactionId = "PREAUTH_TRX_ID_MISMATCH";
        Transaction mockTrx = createMockTransactionInProgress(
                transactionId, "TRXCODE", "USER_ID", 1000L, 2000L, "Prodotto Test");
        mockTrx.setInitiativeId("OTHER_INITIATIVE");
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTrx));

        PdfServiceImpl svc = newService();

        assertThrows(InitiativeNotfoundException.class,
                () -> svc.createPreauthPdf("INITIATIVEID1", transactionId));
        verifyNoInteractions(decryptRestConnector);
    }

    @Test
    void createPreauthPdf_shouldContainExpectedTexts() throws Exception {
        String initiativeId = "INITIATIVEID1";
        String transactionId = "PREAUTHTRXID123";
        String trxCode = "PREAUTHCODE";
        String userId = "USER_FOR_DECRYPT";
        String fiscalCode = "MRARSS80A01H501Z";
        String productName = "LAVATRICE SUPER MODELLO X";
        String productGtin = "987654321198";

        Transaction mockTrx = createMockTransactionInProgress(
                transactionId, trxCode, userId, 3000L, 10000L, productName);

        Map<String, String> properties = new HashMap<>(mockTrx.getAdditionalProperties());
        properties.put("productGtin", productGtin);
        mockTrx.setAdditionalProperties(properties);
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTrx));
        when(decryptRestConnector.getPiiByToken(userId)).thenReturn(new DecryptCfDTO(fiscalCode));

        PdfServiceImpl svc = newService();
        ReportDTOWithTrxCode result = svc.createPreauthPdf(initiativeId, transactionId);

        byte[] bytes = Base64.getDecoder().decode(result.getData());
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdf = new PdfDocument(reader)) {
            String page1Text = PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
            String norm = normalize(page1Text);

            assertTrue(norm.contains("BONUS ELETTRODOMESTICI"));
            assertTrue(norm.contains(fiscalCode));
            assertTrue(norm.contains("30 00")); // Sconto
            assertTrue(norm.contains("100 00")); // Importo da scontare (totale)
            assertTrue(norm.contains("70 00")); // Spesa finale
            assertTrue(norm.contains(productName.toUpperCase()));
            assertTrue(norm.contains(trxCode.toUpperCase()));
            System.out.println(norm);
            assertTrue(norm.contains("ID TRANSAZIONE " + transactionId));
            assertTrue(norm.contains("IL BONUS ELETTRODOMESTICI E REALIZZATO TRAMITE"));
        }
    }

    @Test
    void createPreauthPdf_whenPdfGenerationFails_shouldThrowPdfGenerationException() {
        String initiativeId = "INITIATIVEID1";
        String transactionId = "TRX_ID_FAIL";
        Transaction mockTrx = createMockTransactionInProgress(
                transactionId, "CODE", "USER", 1L, 2L, "Prod");
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTrx));
        when(decryptRestConnector.getPiiByToken(anyString())).thenThrow(new RuntimeException("Connector down"));

        PdfServiceImpl svc = newService();

        PdfGenerationException ex = assertThrows(PdfGenerationException.class,
                () -> svc.createPreauthPdf(initiativeId, transactionId));

        assertEquals("Errore durante la generazione del PDF", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("Connector down", ex.getCause().getMessage());
    }

    private Transaction createMockTransactionInProgress(String transactionId, String trxCode, String userId, long rewardCents, long effectiveAmountCents, String productName) {
        Transaction trx = new Transaction();
        trx.setId(transactionId);
        trx.setInitiativeId("INITIATIVEID1");
        trx.setTrxCode(trxCode);
        trx.setUserId(userId);
        trx.setTrxDate(OffsetDateTime.parse("2024-07-15T10:30:00Z"));
        trx.setRewardCents(rewardCents);
        trx.setEffectiveAmountCents(effectiveAmountCents);
        trx.setAdditionalProperties(Map.of("productName", productName));
        return trx;
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

    private static byte[] tinyPng() {
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO4yqekAAAAASUVORK5CYII=";
        return java.util.Base64.getDecoder().decode(b64);
    }
}