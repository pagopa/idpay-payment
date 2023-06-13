package it.gov.pagopa.payment.service.qrcode.expired;

import it.gov.pagopa.payment.model.TransactionInProgress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class QRCodeExpirationBase {

     public final void execute(){
         TransactionInProgress expiredTransaction;
         while((expiredTransaction = findExpiredTransaction()) != null ){
             infoLog(expiredTransaction);
             handleExpiredTransaction(expiredTransaction);
         }
     }

     /**The invoked function to retrieve lapsed transactions*/
     protected abstract TransactionInProgress findExpiredTransaction();

     /** The invoked function to manage lapsed transactions */
     protected abstract void handleExpiredTransaction(TransactionInProgress trx);

     protected abstract String infoLog(TransactionInProgress trx);
}
