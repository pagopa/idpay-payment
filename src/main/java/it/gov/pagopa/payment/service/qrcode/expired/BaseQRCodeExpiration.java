package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BaseQRCodeExpiration {

    @Autowired
    protected AuditUtilities auditUtilities;

    protected static final String EXPIRED_QR_CODE = "EXPIRED_QR_CODE";

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
                handleExpiredTransaction(expiredTransaction);
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
