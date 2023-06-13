package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class QRCodeExpirationBase {

    protected static final String EXPIRED_QR_CODE = "[EXPIRED_QR_CODE]";

     public final void execute(){
         TransactionInProgress expiredTransaction;
         while((expiredTransaction = findExpiredTransaction()) != null ){
             log.info("{} {} Starting to manage the expired transaction with trxId {}, status {} and trxChargeDate {}",
                     EXPIRED_QR_CODE,
                     getFlow(),
                     expiredTransaction.getId(),
                     expiredTransaction.getStatus(),
                     expiredTransaction.getTrxChargeDate());
             try{
                handleExpiredTransaction(expiredTransaction);
             } catch (Exception e){
                 log.info("{} {} An error occurred while handling transaction {}", EXPIRED_QR_CODE, getFlow(), expiredTransaction.getId());
             }
         }
     }

     /**The invoked function to retrieve lapsed transactions*/
     protected abstract TransactionInProgress findExpiredTransaction();

     /** The invoked function to manage lapsed transactions */
     protected abstract void handleExpiredTransaction(TransactionInProgress trx);

     protected abstract String getFlow();
}
