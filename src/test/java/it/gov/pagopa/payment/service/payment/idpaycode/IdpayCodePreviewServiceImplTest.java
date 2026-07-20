package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnector;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.mapper.AuthPaymentMapper;
import it.gov.pagopa.payment.dto.mapper.idpaycode.AuthPaymentIdpayCodeMapper;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.InvalidProductCategoryException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.exception.custom.TransactionRejectedException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
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
    private static final String INITIATIVE_ID = "INITIATIVE_ID";

    @Mock private PaymentInstrumentConnector paymentInstrumentConnectorMock;
    @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private TransactionRepository transactionRepository;
    @Mock private MerchantConnector merchantConnectorMock;

    private IdpayCodePreviewService idpayCodePreviewService;

    @BeforeEach
    void setUp() {
        idpayCodePreviewService = new IdpayCodePreviewServiceImpl(
                transactionRepository,
                transactionInProgressRepositoryMock,
                paymentInstrumentConnectorMock,
                commonPreAuthServiceMock,
                new AuthPaymentMapper(),
                new AuthPaymentIdpayCodeMapper(),
                auditUtilitiesMock,
                merchantConnectorMock);
    }

    @Test
    void previewPayment() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setPointOfSaleId("POS_123");
        trx.setProductType("DS");
        trx.setAmountCents(10000L);
        trx.setTrxChargeDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("description", "abc 1234");
        trx.setAdditionalProperties(additionalProperties);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        when(paymentInstrumentConnectorMock.getSecondFactor(trx.getUserId()))
                .thenReturn(new SecondFactorDTO(SECOND_FACTOR));

        when(commonPreAuthServiceMock.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenReturn(authPaymentDTO);

        //When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons","splitPayment","residualAmountCents");

        Assertions.assertEquals(7000L, trx.getVoucherAmountCents());

        verify(merchantConnectorMock, times(1)).merchantDetail(MERCHANTID, trx.getInitiativeId());
        verify(merchantConnectorMock, times(1)).getPointOfSaleByInitiativeId(MERCHANTID, "POS_123", INITIATIVE_ID);

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
        verify(transactionInProgressRepositoryMock, times(1)).save(trx);
    }

    @Test
    void previewPayment_withPointOfSaleNull() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setPointOfSaleId(null);
        trx.setProductType("DS");
        trx.setAmountCents(10000L);
        trx.setTrxChargeDate(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("description", "abc 1234");
        trx.setAdditionalProperties(additionalProperties);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, trx);

        when(paymentInstrumentConnectorMock.getSecondFactor(trx.getUserId()))
                .thenReturn(new SecondFactorDTO(SECOND_FACTOR));

        when(commonPreAuthServiceMock.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenReturn(authPaymentDTO);

        //When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, INITIATIVE_ID);

        //Then
        Assertions.assertNotNull(result);

        verify(merchantConnectorMock, times(1)).merchantDetail(MERCHANTID, trx.getInitiativeId());
        verify(merchantConnectorMock, never()).getPointOfSaleByInitiativeId(anyString(), anyString(), anyString());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_initiativeIdMismatch() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When & Then
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, "WRONG_INITIATIVE")
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
    }

    @Test
    void previewPaymentNotFound() {
        //Given
        String trxId = "trxId";

        when(transactionInProgressRepositoryMock.findById(trxId))
                .thenReturn(Optional.empty());

        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, MERCHANTID, INITIATIVE_ID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());
    }

    @Test
    void previewPayment_notRelateUser() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.CREATED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setProductType("DT");
        trx.setAmountCents(2000L);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        AuthPaymentDTO result = idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, INITIATIVE_ID);

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
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setProductType("DTSC");

        String trxId = trx.getId();

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        when(commonPreAuthServiceMock.previewPayment(trx, RewardConstants.TRX_CHANNEL_IDPAYCODE, SyncTrxStatus.IDENTIFIED))
                .thenThrow(new TransactionRejectedException("DUMMY_EXCEPTION"));

        //When
        TransactionRejectedException result = Assertions.assertThrows(TransactionRejectedException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, MERCHANTID, INITIATIVE_ID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_differentMerchantId() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId(USER_ID);
        trx.setChannel(RewardConstants.TRX_CHANNEL_IDPAYCODE);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);

        String trxId = trx.getId();
        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        //When
        MerchantOrAcquirerNotAllowedException result = Assertions.assertThrows(MerchantOrAcquirerNotAllowedException.class, () ->
                idpayCodePreviewService.previewPayment(trxId, "DUMMYMERCHANT", INITIATIVE_ID)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, result.getCode());

        verify(transactionInProgressRepositoryMock, times(1)).findById(anyString());
    }

    @Test
    void previewPayment_invalidProductCategory() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setProductType("INVALID_CAT");

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        Assertions.assertThrows(InvalidProductCategoryException.class, () ->
                idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, INITIATIVE_ID)
        );
    }

    @Test
    void previewPayment_productTypeNull() {
        //Given
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        when(transactionRepository.findById(anyString())).thenReturn(Optional.of(transaction));
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setMerchantId(MERCHANTID);
        trx.setInitiativeId(INITIATIVE_ID);
        trx.setProductType(null);

        when(transactionInProgressRepositoryMock.findById(trx.getId())).thenReturn(Optional.of(trx));

        Assertions.assertThrows(InvalidProductCategoryException.class, () ->
                idpayCodePreviewService.previewPayment(trx.getId(), MERCHANTID, INITIATIVE_ID)
        );
    }
}
