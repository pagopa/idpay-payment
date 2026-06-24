package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
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
    @Mock private IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredServiceMock;
    @Mock private PaymentInstrumentConnectorImpl paymentInstrumentConnectorMock;
    @Mock private CommonAuthServiceImpl commonAuthServiceMock;

    private IdpayCodeAuthPaymentService idpayCodeAuthPaymentService;

        @BeforeEach
    void setUp() {
            idpayCodeAuthPaymentService = new IdpayCodeAuthPaymentServiceImpl(
                    idpayCodeAuthorizationExpiredServiceMock,
                    paymentInstrumentConnectorMock,
                    commonAuthServiceMock);
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
        trx.setRewardCents(null);
        trx.setUserId("USERID");


        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1,trx);
        authPaymentDTO.setStatus(SyncTrxStatus.REWARDED);

        when(idpayCodeAuthorizationExpiredServiceMock.findByTrxIdAndAuthorizationNotExpired(trx.getId()))
                .thenReturn(trx);

        when(commonAuthServiceMock.authPayment(trx, trx.getUserId(), trx.getTrxCode()))
                .thenReturn(authPaymentDTO);
        //When
       AuthPaymentDTO result = idpayCodeAuthPaymentService.authPayment(trx.getId(),trx.getMerchantId(),pinBlockDTO);

        //Then
        Assertions.assertNotNull(result);
        Mockito.verifyNoMoreInteractions(
                idpayCodeAuthorizationExpiredServiceMock,
                commonAuthServiceMock
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