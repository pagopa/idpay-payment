package it.gov.pagopa.payment.service.payment.barcode;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.expired.BarCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BarCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements BarCodeAuthPaymentService {

    private final BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService;

    public BarCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                         BarCodeAuthorizationExpiredService barCodeAuthorizationExpiredService,
                                         RewardCalculatorConnector rewardCalculatorConnector,
                                         TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService,
                                         AuditUtilities auditUtilities,
                                         WalletConnector walletConnector){
        super(transactionInProgressRepository, rewardCalculatorConnector, notifierService,
                paymentErrorNotifierService, auditUtilities, walletConnector);
        this.barCodeAuthorizationExpiredService = barCodeAuthorizationExpiredService;
    }

    @Override
    public AuthPaymentDTO authPayment(String trxCode, String merchantId, long amountCents){
        try {
            TransactionInProgress trx = barCodeAuthorizationExpiredService.findByTrxCodeAndAuthorizationNotExpired(trxCode.toLowerCase());

            if (trx == null) {
                throw new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                        "Cannot find transaction with trxCode [%s]".formatted(trxCode));
            }

            checkWalletStatus(trx.getInitiativeId(), trx.getUserId());

            trx.setAmountCents(amountCents);
            trx.setMerchantId(merchantId);
            trx.setAmountCurrency(PaymentConstants.CURRENCY_EUR);

            AuthPaymentDTO authPaymentDTO = invokeRuleEngine(trx.getUserId(), trxCode, trx);

            logAuthorizedPayment(authPaymentDTO.getInitiativeId(), authPaymentDTO.getId(), trxCode, merchantId,authPaymentDTO.getReward(), authPaymentDTO.getRejectionReasons());
            authPaymentDTO.setResidualBudget(CommonUtilities.calculateResidualBudget(trx.getRewards()));
            authPaymentDTO.setRejectionReasons(null);
            return authPaymentDTO;
        } catch (RuntimeException e) {
            logErrorAuthorizedPayment(trxCode, merchantId);
            throw e;
        }
    }

    @Override
    protected void logAuthorizedPayment(String initiativeId, String id, String trxCode, String merchantId,Long reward, List<String> rejectionReasons){
        auditUtilities.logBarCodeAuthorizedPayment(initiativeId, id, trxCode, merchantId, reward, rejectionReasons);
    }

    @Override
    protected void logErrorAuthorizedPayment(String trxCode, String merchantId){
        auditUtilities.logBarCodeErrorAuthorizedPayment(trxCode, merchantId);
    }

    @Override
    public SyncTrxStatus getSyncTrxStatus(){
        return SyncTrxStatus.CREATED;
    }
}
