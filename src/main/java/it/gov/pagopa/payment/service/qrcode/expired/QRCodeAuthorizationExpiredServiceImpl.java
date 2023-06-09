package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeAuthorizationExpiredServiceImpl extends BaseQRCodeExpiration implements QRCodeAuthorizationExpiredService {
    private final TransactionInProgressRepository transactionInProgressRepository;
    private final RewardCalculatorConnector rewardCalculatorConnector;

    public QRCodeAuthorizationExpiredServiceImpl(TransactionInProgressRepository transactionInProgressRepository, RewardCalculatorConnector rewardCalculatorConnector, AuditUtilities auditUtilities) {
        super(auditUtilities);
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.rewardCalculatorConnector = rewardCalculatorConnector;
    }

    @Override
    protected TransactionInProgress findExpiredTransaction() {
        return transactionInProgressRepository.findAuthorizationExpiredTransaction();
    }

    @Override
    protected TransactionInProgress handleExpiredTransaction(TransactionInProgress trx) {
        if(trx.getStatus().equals(SyncTrxStatus.IDENTIFIED)){
            try{
                rewardCalculatorConnector.cancelTransaction(trx);
            } catch (ClientException e) {
                if(e.getHttpStatus() != HttpStatus.NOT_FOUND){
                    throw new IllegalStateException("An error occurred in the microservice reward-calculator while handling transaction with id %s".formatted(trx.getId()),e);
                }
            }
        }
        transactionInProgressRepository.deleteById(trx.getId());
        return trx;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_AUTHORIZATION_EXPIRED";
    }
}
