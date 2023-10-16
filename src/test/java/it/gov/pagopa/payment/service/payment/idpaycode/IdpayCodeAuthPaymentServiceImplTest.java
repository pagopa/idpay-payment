package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
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
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

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
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());
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
        ClientExceptionNoBody result = Assertions.assertThrows(ClientExceptionNoBody.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getHttpStatus());

    }

    @Test
    void authTrx_anotherMerchant() {
        //Given
        String trxId = "trxId".toLowerCase();
        PinBlockDTO pinBlockDTO = new PinBlockDTO("PINBLOCK", "KEY");

        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        trx.setMerchantId("MERCHANTID");

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trxId))
                .thenReturn(trx);



        //When
        ClientExceptionWithBody result = Assertions.assertThrows(ClientExceptionWithBody.class, () ->
                idpayCodeAuthPaymentService.authPayment(trxId, "DUMMY_MERCHANTID", pinBlockDTO)
        );

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.getHttpStatus());
        Assertions.assertEquals(PaymentConstants.ExceptionCode.REJECTED, result.getCode());

    }
}