package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpayCodeAuthPaymentServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredServiceMock;
    @Mock private PaymentInstrumentConnectorImpl paymentInstrumentConnectorMock;
    @Mock private CommonPreAuthServiceImpl commonPreAuthServiceMock;
    private IdpayCodeAuthPaymentService idpayCodeAuthPaymentService;
    private static final String WALLET_STATUS_REFUNDABLE = "REFUNDABLE";


        @BeforeEach
    void setUp() {
        long authorizationExpirationMinutes = 4350;
            idpayCodeAuthPaymentService = new IdpayCodeAuthPaymentServiceImpl(
                transactionInProgressRepositoryMock,
                    rewardCalculatorConnectorMock,
                    auditUtilitiesMock,
                    walletConnectorMock,
                    idpayCodeAuthorizationExpiredServiceMock,
                    paymentInstrumentConnectorMock,
                    commonPreAuthServiceMock);
    }

    @Test
    void authTrxNotFound() {
        //Given
        String trxId = "trxId".toLowerCase();
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trxId))
                .thenReturn(null);


        //When
        TransactionNotFoundOrExpiredException result = Assertions.assertThrows(TransactionNotFoundOrExpiredException.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED, result.getCode());

    }

    @Test
    void authTrxNotIDENTIFIED() {
        //Given
        String trxId = "trxId".toLowerCase();
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.CREATED);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trxId))
                .thenReturn(trx);


        //When
        OperationNotAllowedException result = Assertions.assertThrows(OperationNotAllowedException.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);

    }
    @Test
    void givenAuthTrxWithStatusIdentifiedAndRewardNull() {
        //Given
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setReward(null);
        trx.setUserId("USERID");

        AuthPaymentDTO paymentDTO = AuthPaymentDTOFaker.mockInstance(1,trx);
        paymentDTO.setStatus(SyncTrxStatus.AUTHORIZATION_REQUESTED);

        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,trx);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        WalletDTO wallet = WalletDTOFaker.mockInstance(1,WALLET_STATUS_REFUNDABLE);

        when(walletConnectorMock.getWallet(trx.getInitiativeId(), trx.getUserId()))
                .thenReturn(wallet);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trx.getId()))
                .thenReturn(trx);

        when(commonPreAuthServiceMock.previewPayment(trx,trx.getChannel(),SyncTrxStatus.AUTHORIZATION_REQUESTED)).thenReturn(paymentDTO);

        when(rewardCalculatorConnectorMock.authorizePayment(trx)).thenReturn(authPaymentDTO);

        //When
       AuthPaymentDTO result = idpayCodeAuthPaymentService.authPayment(trx.getId(),trx.getMerchantId(),pinBlockDTO);

        //Then
        Assertions.assertNotNull(result);
        Mockito.verifyNoMoreInteractions(
                walletConnectorMock,
                idpayCodeAuthorizationExpiredServiceMock,
                commonPreAuthServiceMock,
                rewardCalculatorConnectorMock
        );

    }

    @Test
    void authTrx_anotherMerchant() {
        //Given
        String trxId = "trxId".toLowerCase();
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId("USERID");
        trx.setMerchantId("MERCHANTID");

        WalletDTO walletDTO = WalletDTOFaker.mockInstance(1, WALLET_STATUS_REFUNDABLE);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trxId))
                .thenReturn(trx);



        //When
        MerchantOrAcquirerNotAllowedException result = Assertions.assertThrows(MerchantOrAcquirerNotAllowedException.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "DUMMY_MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED, result.getCode());

    }
}