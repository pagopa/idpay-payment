package it.gov.pagopa.payment.service.payment.idpayCode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.paymentInstrument.PaymentInstrumentRestConnectorImpl;
import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnector;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpayCode.expired.IdpayCodeAuthorizationExpiredService;
import it.gov.pagopa.payment.utils.AuditUtilities;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdpayCodeAuthPaymentServiceImpl extends CommonAuthServiceImpl implements IdpayCodeAuthPaymentService {
    private final IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService;
    private final PaymentInstrumentRestConnectorImpl paymentInstrumentConnector;
    protected IdpayCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository, RewardCalculatorConnector rewardCalculatorConnector, TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService, AuditUtilities auditUtilities, WalletConnector walletConnector, IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService, PaymentInstrumentRestConnectorImpl paymentInstrumentConnector) {
        super(transactionInProgressRepository, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities, walletConnector);
        this.idpayCodeAuthorizationExpiredService = idpayCodeAuthorizationExpiredService;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
    }
        @Override
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody) {
        TransactionInProgress trx = idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(trxId.toLowerCase());
       // payment-instrument call to check pinBlock
        paymentInstrumentConnector.checkPinBlock(pinBlockBody,trx.getUserId());

        if (!merchantId.equals(trx.getMerchantId())){
            throw new ClientExceptionWithBody(HttpStatus.NOT_FOUND,
                    PaymentConstants.ExceptionCode.REJECTED,
                    "The merchant id [%s] of the trx , is not equals to the merchant id [%s]".formatted(trx.getMerchantId(),merchantId));
        }
        return super.authPayment(trx,trx.getUserId(), trxId);

    }
}
