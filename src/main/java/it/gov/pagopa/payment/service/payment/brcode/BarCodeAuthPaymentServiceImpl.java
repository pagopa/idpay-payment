package it.gov.pagopa.payment.service.payment.brcode;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.qrcode.expired.QRCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements BarCodeAuthPaymentService {

    public BarCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                         QRCodeAuthorizationExpiredService authorizationExpiredService,
                                         RewardCalculatorConnector rewardCalculatorConnector,
                                         TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
                                         AuditUtilities auditUtilities,
                                         WalletConnector walletConnector){
        super(transactionInProgressRepository, authorizationExpiredService, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities, walletConnector);
    }

    @Override
    public void logAuthorizedPayment(String initiativeId, String id, String trxCode, String userId, Long reward, List<String> rejectionReasons, String merchantId){
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, reward, rejectionReasons);
    }

    @Override
    public void logErrorAuthorizedPayment(String trxCode, String userId, String merchantId){
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    @Override
    public SyncTrxStatus getSyncTrxStatus(){
        return SyncTrxStatus.CREATED;
    }

    @Override
    public boolean evaluateUserId(TransactionInProgress trx, String userId){
        return false;
    }
}
