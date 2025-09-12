package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.PDVService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointOfSaleTransactionMapperTest {

    private PointOfSaleTransactionMapper mapper;

    private final String QRCODE_IMGURL = "QRCODE_IMGURL";
    private final String QRCODE_TXTURL = "QRCODE_TXTURL";

    private TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper;
    private PDVService pdvService;

    @BeforeEach
    void setup() {
        transactionInProgress2TransactionResponseMapper = Mockito.mock(TransactionInProgress2TransactionResponseMapper.class);
        pdvService = Mockito.mock(PDVService.class);
        mapper = new PointOfSaleTransactionMapper(10, transactionInProgress2TransactionResponseMapper, pdvService);
    }

    @Test
    void testToPointOfSaleTransactionDTO_WithFiscalCodeInputAndQrCodeChannel() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        trx.setRewardCents(500L);

        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trx.getTrxCode())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trx.getTrxCode())).thenReturn(QRCODE_TXTURL);

        String fiscalCodeInput = "FISCALCODE1";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());
        assertEquals(QRCODE_IMGURL, result.getQrcodePngUrl());
        assertEquals(QRCODE_TXTURL, result.getQrcodeTxtUrl());
        assertEquals(CommonUtilities.minutesToSeconds(10), result.getTrxExpirationSeconds());
        assertEquals(Boolean.TRUE, result.getSplitPayment());
        assertEquals(trx.getAmountCents() - trx.getRewardCents(), result.getResidualAmountCents());
        verifyNoInteractions(pdvService);
    }

    @Test
    void testToPointOfSaleTransactionDTO_NoFiscalCodeInputAndNoChannel() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setChannel(null);
        trx.setUserId("USERID1");
        trx.setTrxCode("TRX123");

        when(pdvService.decryptCF("USERID1")).thenReturn("DECRYPTED_FISCAL_CODE");

        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl("TRX123")).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl("TRX123")).thenReturn(QRCODE_TXTURL);

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, null);

        assertNotNull(result);
        assertEquals("DECRYPTED_FISCAL_CODE", result.getFiscalCode());
        assertEquals(QRCODE_IMGURL, result.getQrcodePngUrl());
        assertEquals(QRCODE_TXTURL, result.getQrcodeTxtUrl());
        assertEquals(CommonUtilities.minutesToSeconds(10), result.getTrxExpirationSeconds());


        verify(pdvService).decryptCF("USERID1");
        verify(transactionInProgress2TransactionResponseMapper).generateTrxCodeImgUrl("TRX123");
        verify(transactionInProgress2TransactionResponseMapper).generateTrxCodeTxtUrl("TRX123");
    }

    @Test
    void testToPointOfSaleTransactionDTO_WithNullChannel_ShouldGenerateQrCodeUrls() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setChannel(null);
        trx.setRewardCents(500L);

        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeImgUrl(trx.getTrxCode())).thenReturn(QRCODE_IMGURL);
        when(transactionInProgress2TransactionResponseMapper.generateTrxCodeTxtUrl(trx.getTrxCode())).thenReturn(QRCODE_TXTURL);

        String fiscalCodeInput = "FISCALCODE1";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());
        assertEquals(QRCODE_IMGURL, result.getQrcodePngUrl());
        assertEquals(QRCODE_TXTURL, result.getQrcodeTxtUrl());
        assertEquals(CommonUtilities.minutesToSeconds(10), result.getTrxExpirationSeconds());
        assertEquals(Boolean.TRUE, result.getSplitPayment());
        assertEquals(trx.getAmountCents() - trx.getRewardCents(), result.getResidualAmountCents());

        verify(transactionInProgress2TransactionResponseMapper).generateTrxCodeImgUrl(trx.getTrxCode());
        verify(transactionInProgress2TransactionResponseMapper).generateTrxCodeTxtUrl(trx.getTrxCode());
        verifyNoInteractions(pdvService);
    }

    @Test
    void testToPointOfSaleTransactionDTO_WithNonQrCodeChannel_ShouldNotGenerateQrCodeUrls() {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setChannel("OTHER_CHANNEL");
        trx.setRewardCents(300L);

        String fiscalCodeInput = "FISCALCODE1";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());
        assertNull(result.getQrcodePngUrl());
        assertNull(result.getQrcodeTxtUrl());
        assertEquals(Boolean.TRUE, result.getSplitPayment());
        assertEquals(trx.getAmountCents() - trx.getRewardCents(), result.getResidualAmountCents());

        verifyNoInteractions(transactionInProgress2TransactionResponseMapper);
        verifyNoInteractions(pdvService);
    }
}
