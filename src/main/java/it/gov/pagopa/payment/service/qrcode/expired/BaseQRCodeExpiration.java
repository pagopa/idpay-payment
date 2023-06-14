package it.gov.pagopa.payment.service.qrcode.expired;

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
         TransactionInProgress expiredTransaction;
         while((expiredTransaction = findExpiredTransaction()) != null ){
             log.info("[{}] [{}] Starting to manage the expired transaction with trxId {}, status {} and trxChargeDate {}",
                     EXPIRED_QR_CODE,
                     getFlowName(),
                     expiredTransaction.getId(),
                     expiredTransaction.getStatus(),
                     expiredTransaction.getTrxChargeDate());
             try{
                long startTime=System.currentTimeMillis();
                handleExpiredTransaction(expiredTransaction);
                log.info("[PERFORMANCE_LOG] [{}] [{}] Time occurred to perform business logic: {} ms. Transaction evaluated ({})", EXPIRED_QR_CODE, getFlowName(), System.currentTimeMillis()-startTime, expiredTransaction.getId());

                auditUtilities.logExpiredTransaction(expiredTransaction.getInitiativeId(), expiredTransaction.getId(), expiredTransaction.getIdTrxAcquirer(), expiredTransaction.getUserId(), getFlowName());
             } catch (Exception e){
                 log.error("[{}] [{}] An error occurred while handling transaction {}", EXPIRED_QR_CODE, getFlowName(), expiredTransaction.getId());
                 auditUtilities.logErrorExpiredTransaction(expiredTransaction.getInitiativeId(), expiredTransaction.getId(), expiredTransaction.getIdTrxAcquirer(), expiredTransaction.getUserId(), getFlowName());
             }
         }
     }

     /**The invoked function to retrieve lapsed transactions*/
     protected abstract TransactionInProgress findExpiredTransaction();

     /** The invoked function to manage lapsed transactions */
     protected abstract void handleExpiredTransaction(TransactionInProgress trx);

     protected abstract String getFlowName();
}
