package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.common.web.exception.custom.badrequest.OperationNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.forbidden.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.common.web.exception.custom.notfound.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class IdpayCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements IdpayCodeAuthPaymentService {
    private final IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService;
    private final PaymentInstrumentConnectorImpl paymentInstrumentConnector;
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    protected IdpayCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository,
                                              RewardCalculatorConnector rewardCalculatorConnector,
                                              TransactionNotifierService notifierService,
                                              PaymentErrorNotifierService paymentErrorNotifierService,
                                              AuditUtilities auditUtilities,
                                              WalletConnector walletConnector,
                                              IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService,
                                              PaymentInstrumentConnectorImpl paymentInstrumentConnector) {
        super(transactionInProgressRepository, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities, walletConnector);
        this.idpayCodeAuthorizationExpiredService = idpayCodeAuthorizationExpiredService;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
    }
        @Override
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody) {
        TransactionInProgress trx = idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(trxId);

        if(trx == null){
            throw new TransactionNotFoundOrExpiredException(
                    PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                    "Cannot find transaction with transactionId [%s]".formatted(trxId));
        }

        if(trx.getUserId() == null){
            throw new OperationNotAllowedException(ExceptionCode.TRX_STATUS_NOT_VALID,
                    "Unexpected status for transaction with transactionId [%s]".formatted(trxId));
        }

        // payment-instrument call to check pinBlock
        paymentInstrumentConnector.checkPinBlock(pinBlockBody,trx.getUserId());

        if (!merchantId.equals(trx.getMerchantId())){
            throw new MerchantOrAcquirerNotAllowedException(
                ExceptionCode.PAYMENT_MERCHANT_NOT_ALLOWED,
                    "The merchant id [%s] of the trx , is not equal to the merchant id [%s]".formatted(trx.getMerchantId(),merchantId));
        }
        return super.authPayment(trx,trx.getUserId(),trx.getTrxCode());

    }
}
