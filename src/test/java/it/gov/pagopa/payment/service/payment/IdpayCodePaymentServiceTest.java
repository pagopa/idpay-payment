package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodeAuthPaymentService;
import it.gov.pagopa.payment.service.payment.idpaycode.IdpayCodePreAuthService;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpayCodePaymentServiceTest {
    private static final String MERCHANTID = "MERCHANTID";

    @Mock private IdpayCodePreAuthService idpayCodePreAuthServiceMock;
    @Mock private IdpayCodeAuthPaymentService idpayCodeAuthPaymentServiceMock;
    private IdpayCodePaymentService idpayCodePaymentService;

    @BeforeEach
    void setUp(){
        idpayCodePaymentService = new IdpayCodePaymentServiceImpl(idpayCodePreAuthServiceMock, idpayCodeAuthPaymentServiceMock);
    }

    @Test
    void previewPayment() {
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        AuthPaymentDTO preview = AuthPaymentDTOFaker.mockInstance(1,trx);
        when(idpayCodePreAuthServiceMock.previewPayment(trx.getId(), MERCHANTID))
                .thenReturn(preview);
        //When
        AuthPaymentDTO result = idpayCodePaymentService.previewPayment(trx.getId(), MERCHANTID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(preview, result);
    }


}