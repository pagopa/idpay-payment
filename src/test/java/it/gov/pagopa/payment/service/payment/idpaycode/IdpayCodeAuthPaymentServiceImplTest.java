package it.gov.pagopa.payment.service.payment.idpaycode;

import static org.mockito.Mockito.when;

import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdpayCodeAuthPaymentServiceImplTest {
    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;
    @Mock private TransactionNotifierService notifierServiceMock;
    @Mock private PaymentErrorNotifierService paymentErrorNotifierServiceMock;
    @Mock private AuditUtilities auditUtilitiesMock;
    @Mock private WalletConnector walletConnectorMock;
    @Mock private IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredServiceMock;
    @Mock private PaymentInstrumentConnectorImpl paymentInstrumentConnectorMock;
    private IdpayCodeAuthPaymentService idpayCodeAuthPaymentService;


        @BeforeEach
    void setUp() {
        long authorizationExpirationMinutes = 4350;
            idpayCodeAuthPaymentService = new IdpayCodeAuthPaymentServiceImpl(
                transactionInProgressRepositoryMock,
                    rewardCalculatorConnectorMock,
                    notifierServiceMock,
                    paymentErrorNotifierServiceMock,
                    auditUtilitiesMock,
                    walletConnectorMock,
                    idpayCodeAuthorizationExpiredServiceMock,
                    paymentInstrumentConnectorMock);
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
    void authTrx_anotherMerchant() {
        //Given
        String trxId = "trxId".toLowerCase();
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setUserId("USERID");
        trx.setMerchantId("MERCHANTID");

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