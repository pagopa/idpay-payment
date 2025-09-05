package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.TransactionRejectedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.RewardConstants;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class IdpayCodePreviewServiceImplTest {
    private static final String USER_ID = "userId";
    private static final String MERCHANTID = "MERCHANTID";
    private static final String SECOND_FACTOR = "SECOND_FACTOR";
    @Mock private PaymentInstrumentConnector paymentInstrumentConnectorMock;
    @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;

    private IdpayCodePreviewService idpayCodePreviewService;

    @BeforeEach
    void setUp() {
        idpayCodePreviewService = new IdpayCodePreviewServiceImpl(transactionInProgressRepositoryMock,
                paymentInstrumentConnectorMock,
                commonPreAuthServiceMock,
                new AuthPaymentMapper(),
                new AuthPaymentIdpayCodeMapper(),
                auditUtilitiesMock);
    }

    @Test
    void previewPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);
        trx.setTrxChargeDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("description", "abc 1234");
        trx.setAdditionalProperties(additionalProperties);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        System.out.println(authPaymentDTO.getAdditionalProperties());
        when(paymentInstrumentConnectorMock.getSecondFactor(trx.getUserId()))
                .thenReturn(new SecondFactorDTO(SECOND_FACTOR));

        when(commonPreAuthServiceMock.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenReturn(authPaymentDTO);

        //When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons","splitPayment","residualAmountCents");

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPaymentNotFound() {
        //Given
        String trxId = "trxId";

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());

        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }

    @Test
    void previewPayment_notRelateUser() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setMerchantId(MERCHANTID);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

         //When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result,
                "rewardCents",
                "counters",
                "residualBudgetCents",
                "secondFactor",
                "splitPayment",
                "residualAmountCents"
        );

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_RejectedStatusRE() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        when(commonPreAuthServiceMock.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenThrow(new TransactionRejectedException("DUMMY_EXCEPTION"));

        //When
        TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, MERCHANTID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }


    @Test
    void previewPayment_differentMerchantId() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);

        String trxId = trx.getId();
        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        MerchantOrAcquirerNotAllowedException result = Assertions.assertThrows(MerchantOrAcquirerNotAllowedException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, "DUMMYMERCHANT")
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }
}