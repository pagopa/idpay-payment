package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.common.performancelogger.PerformanceLogger;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseQRCodeExpiration {

    protected final AuditUtilities auditUtilities;

    protected static final String EXPIRED_QR_CODE = "EXPIRED_QR_CODE";

    protected BaseQRCodeExpiration(AuditUtilities auditUtilities) {
        this.auditUtilities = auditUtilities;
    }

    public final void execute(){
         TransactionInProgress[] expiredTransaction = new TransactionInProgress[]{null} ;
         while((expiredTransaction[0] = findExpiredTransaction()) != null ){
             log.info("[{}] [{}] Starting to manage the expired transaction with trxId {}, status {} and trxDate {}",
                     EXPIRED_QR_CODE,
                     getFlowName(),
                     expiredTransaction[0].getId(),
                     expiredTransaction[0].getStatus(),
                     expiredTransaction[0].getTrxDate());
             try{
                PerformanceLogger.execute(EXPIRED_QR_CODE,
                        () -> handleExpiredTransaction(expiredTransaction[0]),
                        t -> "Evaluated transaction with ID %s due to %s ". formatted(t.getId(), getFlowName()));
                auditUtilities.logExpiredTransaction(expiredTransaction[0].getInitiativeId(), expiredTransaction[0].getId(), expiredTransaction[0].getTrxCode(), expiredTransaction[0].getUserId(), getFlowName());
             } catch (Exception e){
                 log.error("[{}] [{}] An error occurred while handling transaction: {}, with message: {}", EXPIRED_QR_CODE, getFlowName(), expiredTransaction[0].getId(), e.getMessage());
                 auditUtilities.logErrorExpiredTransaction(expiredTransaction[0].getInitiativeId(), expiredTransaction[0].getId(), expiredTransaction[0].getTrxCode(), expiredTransaction[0].getUserId(), getFlowName());
             }
         }
     }

     /**The invoked function to retrieve lapsed transactions*/
     protected abstract TransactionInProgress findExpiredTransaction();

     /** The invoked function to manage lapsed transactions */
     protected abstract TransactionInProgress handleExpiredTransaction(TransactionInProgress trx);

     protected abstract String getFlowName();
}
