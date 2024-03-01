package it.gov.pagopa.payment.service.payment.expired.idpaycode;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredServiceImpl;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpayCodeAuthorizationExpiredServiceImplTest {

    private final static long EXPIRATION_MINUTES=15;

    @Mock private TransactionInProgressRepository transactionInProgressRepositoryMock;
    @Mock private RewardCalculatorConnector rewardCalculatorConnectorMock;

    private final AuditUtilities auditUtilities = new AuditUtilities();

    private IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService;


    @BeforeEach
    void setUp() {
        idpayCodeAuthorizationExpiredService = new IdpayCodeAuthorizationExpiredServiceImpl(EXPIRATION_MINUTES, transactionInProgressRepositoryMock, auditUtilities,rewardCalculatorConnectorMock);
    }

    @Test
    void findByTrxCodeAndAuthorizationNotExpired(){

        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1, SyncTrxStatus.IDENTIFIED);
        transaction.setUserId("USERID1");


        when(transactionInProgressRepositoryMock.findByTrxIdAndAuthorizationNotExpired(transaction.getTrxCode(),EXPIRATION_MINUTES))
                .thenReturn(transaction);
        idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(transaction.getTrxCode());

        Mockito.verify(transactionInProgressRepositoryMock).findByTrxIdAndAuthorizationNotExpired(transaction.getTrxCode(),EXPIRATION_MINUTES);

    }

}