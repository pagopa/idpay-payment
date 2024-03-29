package it.gov.pagopa.payment.service.payment.common;

import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCommonCodeExpiration {

    protected final AuditUtilities auditUtilities;
    private final String flowName;

    protected BaseCommonCodeExpiration(AuditUtilities auditUtilities, String channel) {
        this.auditUtilities = auditUtilities;
        this.flowName = "EXPIRED_"+channel;

    }

    public Long forceExpiration(String initiativeId) {
        return execute(initiativeId, 0);
    }

    public final Long execute(){
        return execute(null, getExpirationMinutes());
    }

    public final Long execute(String initiativeId, long expirationMinutes){
        long count = 0L;
        TransactionInProgress[] expiredTransaction = new TransactionInProgress[]{null} ;
         while((expiredTransaction[0] = findExpiredTransaction(initiativeId, expirationMinutes)) != null ){
             log.info("[{}] [{}] Starting to manage the expired transaction with trxId {}, status {} and trxDate {}",
                     flowName,
                     getFlowName(),
                     expiredTransaction[0].getId(),
                     expiredTransaction[0].getStatus(),
                     expiredTransaction[0].getTrxDate());
             try{
                PerformanceLogger.execute(flowName,
                        () -> handleExpiredTransaction(expiredTransaction[0]),
                        t -> "Evaluated transaction with ID %s due to %s ". formatted(t.getId(), getFlowName()));
                count++;
                auditUtilities.logExpiredTransaction(expiredTransaction[0].getInitiativeId(), expiredTransaction[0].getId(), expiredTransaction[0].getTrxCode(), expiredTransaction[0].getUserId(), getFlowName());
             } catch (Exception e){
                 log.error("[{}] [{}] An error occurred while handling transaction: {}, with message: {}", flowName, getFlowName(), expiredTransaction[0].getId(), e.getMessage());
                 auditUtilities.logErrorExpiredTransaction(expiredTransaction[0].getInitiativeId(), expiredTransaction[0].getId(), expiredTransaction[0].getTrxCode(), expiredTransaction[0].getUserId(), getFlowName());
             }
         }

         return count;
     }

    /** The trx expiration minutes  */
    protected abstract long getExpirationMinutes();

     /** The invoked function to retrieve lapsed transactions */
     protected abstract TransactionInProgress findExpiredTransaction(String initiativeId, long expirationMinutes);

     /** The invoked function to manage lapsed transactions */
     protected abstract TransactionInProgress handleExpiredTransaction(TransactionInProgress trx);

     protected abstract String getFlowName();
}
