package it.gov.pagopa.payment.connector.rest.rewardConnector;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnectorImpl;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentRequestDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.AuthPaymentResponseDTO;
import it.gov.pagopa.payment.connector.rest.reward.dto.PreAuthPaymentRequestDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.exception.custom.TransactionVersionMismatchException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorConnectorTest {
    @InjectMocks
    private RewardCalculatorConnectorImpl rewardCalculatorConnectorMock;

    @Mock
    private RewardCalculatorRestClient rewardCalculatorRestClientMock;

    @Test
    void givenAuthTrxWhenRewardCalulatorThenInvalidCounterException(){
        //Given
        TransactionInProgress trx = TransactionInProgressFaker.mockInstance(1,SyncTrxStatus.AUTHORIZATION_REQUESTED);

        Mockito.when(rewardCalculatorRestClientMock.previewTransaction(trx.getInitiativeId(),new PreAuthPaymentRequestDTO())).thenReturn(new AuthPaymentResponseDTO());
        Mockito.when(rewardCalculatorRestClientMock.authorizePayment(trx.getCounterVersion(), trx.getInitiativeId(),new AuthPaymentRequestDTO() )).thenThrow(TransactionVersionMismatchException.class);
        //when
        TransactionVersionMismatchException exception = assertThrows(TransactionVersionMismatchException.class, ()->
                rewardCalculatorConnectorMock.authorizePayment(trx));
        //then
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(PaymentConstants.ExceptionCode.PAYMENT_TRANSACTION_VERSION_MISMATCH,exception.getCode());
    }

}
