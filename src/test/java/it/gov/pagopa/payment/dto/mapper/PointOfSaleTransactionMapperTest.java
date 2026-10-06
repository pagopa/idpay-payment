package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.service.PDVService;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.RewardConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class PointOfSaleTransactionMapperTest {

    private PointOfSaleTransactionMapper mapper;

    private final String qrCodeImgUrl  = "QRCODE_IMGURL";
    private final String qrCodeTxtUrl  = "QRCODE_TXTURL";

    private TransactionMapper transactionMapper;
    private PDVService pdvService;

    @BeforeEach
    void setup() {
        transactionMapper = mock(TransactionMapper.class);
        pdvService = mock(PDVService.class);
        mapper = new PointOfSaleTransactionMapper(pdvService,0, qrCodeImgUrl, qrCodeTxtUrl);
    }

    @Test
    void testToPointOfSaleTransactionDTO_WithFiscalCodeInputAndQrCodeChannel() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setChannel(RewardConstants.TRX_CHANNEL_QRCODE);
        trx.setRewardCents(500L);

        when(transactionMapper.generateTrxCodeImgUrl(trx.getTrxCode())).thenReturn(qrCodeImgUrl);
        when(transactionMapper.generateTrxCodeTxtUrl(trx.getTrxCode())).thenReturn(qrCodeTxtUrl);

        String fiscalCodeInput = "fiscalCode";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());
        verifyNoInteractions(pdvService);
    }

    @Test
    void testToPointOfSaleTransactionDTO_NoFiscalCodeInputAndNoChannel() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setChannel(null);
        trx.setUserId("USERID1");
        trx.setTrxCode("TRX123");

        when(pdvService.decryptCF("USERID1")).thenReturn("fiscalCode");

        when(transactionMapper.generateTrxCodeImgUrl("TRX123")).thenReturn(qrCodeImgUrl);
        when(transactionMapper.generateTrxCodeTxtUrl("TRX123")).thenReturn(qrCodeTxtUrl);

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, null);

        assertNotNull(result);
        assertEquals("fiscalCode", result.getFiscalCode());

    }

    @Test
    void testToPointOfSaleTransactionDTO_WithNullChannel_ShouldGenerateQrCodeUrls() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setChannel(null);
        trx.setRewardCents(500L);

        when(transactionMapper.generateTrxCodeImgUrl(trx.getTrxCode())).thenReturn(qrCodeImgUrl);
        when(transactionMapper.generateTrxCodeTxtUrl(trx.getTrxCode())).thenReturn(qrCodeTxtUrl);

        String fiscalCodeInput = "fiscalCode";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());

    }

    @Test
    void testToPointOfSaleTransactionDTO_WithNonQrCodeChannel_ShouldNotGenerateQrCodeUrls() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        trx.setChannel("OTHER_CHANNEL");
        trx.setRewardCents(300L);

        String fiscalCodeInput = "fiscalCode";

        PointOfSaleTransactionDTO result = mapper.toPointOfSaleTransactionDTO(trx, fiscalCodeInput);

        assertNotNull(result);
        assertEquals(fiscalCodeInput, result.getFiscalCode());

        verifyNoInteractions(transactionMapper);
        verifyNoInteractions(pdvService);
    }
}
