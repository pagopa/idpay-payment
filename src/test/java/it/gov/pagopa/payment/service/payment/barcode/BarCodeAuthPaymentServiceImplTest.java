package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.payment.configuration.BarCodeAdditionalPropertiesValidationProperties;
import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.PreviewPaymentResultDTO;
import it.gov.pagopa.payment.dto.barcode.AuthBarCodePaymentDTO;
import it.gov.pagopa.payment.entity.Transaction;
import it.gov.pagopa.payment.enums.PointOfSaleTypeEnum;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionInvalidException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.repository.TransactionRepository;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import it.gov.pagopa.payment.service.payment.barcode.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.barcode.validation.BarCodeAdditionalPropertiesValidationResolver;
import it.gov.pagopa.payment.service.payment.barcode.validation.NoOpBarCodeAdditionalPropertiesValidationStrategy;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarCodeAuthPaymentServiceImplTest {

    private static final String USER_ID = "USERID1";
    private static final String MERCHANT_ID = "MERCHANT_ID";
    private static final String POINTOFSALE_ID = "POS_ID";
    private static final String TRX_CODE1 = "trxcode1";
    private static final String ACQUIRER_ID = "ACQUIRER_ID";
    private static final long AMOUNT_CENTS = 1000L;

    @Mock private PaymentCheckService paymentCheckService;
    @Mock private BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private MerchantConnector merchantConnector;
    @Mock private CommonAuthServiceImpl commonAuthServiceMock;
    @Mock private DecryptRestConnector decryptRestConnector;
    @Mock private TransactionRepository transactionRepository;

    private BarCodeAuthPaymentServiceImpl barCodeAuthPaymentService;

    @BeforeEach
    void setup() {
        BarCodeAdditionalPropertiesValidationProperties validationProperties = new BarCodeAdditionalPropertiesValidationProperties();
        BarCodeAdditionalPropertiesValidationResolver validationResolver = new BarCodeAdditionalPropertiesValidationResolver(
                List.of(new NoOpBarCodeAdditionalPropertiesValidationStrategy()),
                validationProperties);
        barCodeAuthPaymentService = new BarCodeAuthPaymentServiceImpl(
                barCodeAuthorizationExpiredServiceMock,
                merchantConnector,
                transactionRepository,
                commonAuthServiceMock,
                decryptRestConnector,
                validationResolver,
                auditUtilitiesMock);
    }

    @Test
    void barCodeAuthPayment_ok() {
        Transaction transaction = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZATION_REQUESTED);
        transaction.setUserId(USER_ID);
        AuthBarCodePaymentDTO authBarCodePaymentDTO = AuthBarCodePaymentDTO.builder()
                .amountCents(AMOUNT_CENTS)
                .idTrxAcquirer("ID_TRX_ACQUIRER")
                .additionalProperties(Map.of())
                .build();
        AuthPaymentDTO authPaymentDTO = new AuthPaymentDTO();
        authPaymentDTO.setId(transaction.getId());
        authPaymentDTO.setInitiativeId(transaction.getInitiativeId());
        authPaymentDTO.setTrxCode(transaction.getTrxCode());
        authPaymentDTO.setRewardCents(100L);
        authPaymentDTO.setRewards(Map.of(transaction.getInitiativeId(), new it.gov.pagopa.payment.dto.Reward(transaction.getInitiativeId(), "ORG", 100L)));
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        PointOfSaleDTO pointOfSaleDTO = PointOfSaleDTO.builder()
                .type(PointOfSaleTypeEnum.PHYSICAL)
                .franchiseName("Test Franchise")
                .businessName("Test Business")
                .fiscalCode("FISCALCODE123")
                .vatNumber("12345678901")
                .build();
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setFamilyId("FAMILY");

        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNot(TRX_CODE1)).thenReturn(transaction);
        when(merchantConnector.getPointOfSale(MERCHANT_ID, POINTOFSALE_ID)).thenReturn(pointOfSaleDTO);
        when(commonAuthServiceMock.checkWalletStatusAndReturn(transaction.getInitiativeId(), USER_ID)).thenReturn(walletDTO);
        when(commonAuthServiceMock.invokeRuleEngine(transaction)).thenReturn(authPaymentDTO);

        AuthPaymentDTO result = barCodeAuthPaymentService.authPayment(TRX_CODE1, authBarCodePaymentDTO, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID);

        assertNotNull(result);
        verify(commonAuthServiceMock).checkTrxStatusToInvokePreAuth(transaction);
    }

    @Test
    void barCodeAuthPayment_invalidAmount() {
        AuthBarCodePaymentDTO dto = AuthBarCodePaymentDTO.builder().amountCents(0L).build();
        TransactionInvalidException ex = assertThrows(TransactionInvalidException.class,
                () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, dto, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));
        assertEquals(PaymentConstants.ExceptionCode.AMOUNT_NOT_VALID, ex.getCode());
    }

    @Test
    void barCodeAuthPayment_trxNotFound() {
        when(barCodeAuthorizationExpiredServiceMock.findByTrxCodeAndTrxEndDateGreaterThanEqualAndStatusNot(TRX_CODE1)).thenReturn(null);
        doThrow(new TransactionNotFoundOrExpiredException("DUMMY")).when(commonAuthServiceMock).checkAuth(TRX_CODE1, null);
        AuthBarCodePaymentDTO dto = AuthBarCodePaymentDTO.builder().amountCents(1L).build();

        assertThrows(TransactionNotFoundOrExpiredException.class,
                () -> barCodeAuthPaymentService.authPayment(TRX_CODE1, dto, MERCHANT_ID, POINTOFSALE_ID, ACQUIRER_ID));
    }

    @Test
    void previewPayment_ok() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCodeAndStatusNot(anyString(),any())).thenReturn(Optional.of(trx));
        AuthPaymentDTO authPaymentDTO = new AuthPaymentDTO();
        authPaymentDTO.setTrxCode(trx.getTrxCode());
        authPaymentDTO.setRewardCents(100L);
        authPaymentDTO.setTrxDate(OffsetDateTime.now());
        when(commonAuthServiceMock.previewPayment(any(), any())).thenReturn(authPaymentDTO);
        when(decryptRestConnector.getPiiByToken(any())).thenReturn(new DecryptCfDTO("Pii"));

        PreviewPaymentResultDTO result = barCodeAuthPaymentService.previewPayment("trxCode", Map.of(), 90000L);
        assertNotNull(result);
    }

    @Test
    void previewPayment_negativeReward() {
        Transaction trx = TransactionFaker.mockInstance(1, SyncTrxStatus.AUTHORIZED);
        when(transactionRepository.findByTrxCodeAndStatusNot(anyString(),any())).thenReturn(Optional.of(trx));
        AuthPaymentDTO authPaymentDTO = new AuthPaymentDTO();
        authPaymentDTO.setTrxCode(trx.getTrxCode());
        authPaymentDTO.setRewardCents(-1L);
        when(commonAuthServiceMock.previewPayment(any(), any())).thenReturn(authPaymentDTO);

        assertThrows(TransactionInvalidException.class,
                () -> barCodeAuthPaymentService.previewPayment("trxCode", Map.of(), 90000L));
        verify(decryptRestConnector, never()).getPiiByToken(any());
    }
}
