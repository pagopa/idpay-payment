package it.gov.pagopa.payment.service.qrcode;

import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.TransactionNotifierService;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QRCodeConfirmServiceTest {

    @Mock private TransactionInProgressRepository repositoryMock;
    @Mock private TransactionNotifierService notifierServiceMock;

    private final TransactionInProgress2TransactionResponseMapper mapper = new TransactionInProgress2TransactionResponseMapper();

    private QRCodeConfirmationServiceImpl service;

    @BeforeEach
    void init(){
        service = new QRCodeConfirmationServiceImpl(repositoryMock, mapper, notifierServiceMock);
    }

    @Test
    void testTrxNotFound(){
        try{
            service.confirmPayment("TRXID", "MERCHID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e){
            Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
        }
    }

    @Test
    void testMerchantIdNotValid(){
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED));

        try{
            service.confirmPayment("TRXID", "MERCHID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e){
            Assertions.assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        }
    }

    @Test
    void testStatusNotValid(){
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.CREATED);
        trx.setMerchantId("MERCHID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        try{
            service.confirmPayment("TRXID", "MERCHID");
            Assertions.fail("Expected exception");
        } catch (ClientExceptionNoBody e){
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        }
    }

    @Test
    void testSuccess(){
        testSuccessful(true);
    }

    @Test
    void testSuccessNotNotified(){
        testSuccessful(false);
    }

    private void testSuccessful(boolean notificationOutcome) {
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(0, SyncTrxStatus.AUTHORIZED);
        trx.setMerchantId("MERCHID");
        when(repositoryMock.findByIdThrottled("TRXID")).thenReturn(trx);

        when(notifierServiceMock.notify(trx)).thenReturn(notificationOutcome);

        TransactionResponse result = service.confirmPayment("TRXID", "MERCHID");

        Assertions.assertEquals(result, mapper.apply(trx));
        Assertions.assertEquals(SyncTrxStatus.REWARDED, result.getStatus());
    }

}
