package it.gov.pagopa.payment.service.payment.idpayCode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
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
    protected IdpayCodeAuthPaymentServiceImpl(TransactionInProgressRepository transactionInProgressRepository, RewardCalculatorConnector rewardCalculatorConnector, TransactionNotifierService notifierService, PaymentErrorNotifierService paymentErrorNotifierService, AuditUtilities auditUtilities, WalletConnector walletConnector, IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService) {
        super(transactionInProgressRepository, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities, walletConnector);
        this.idpayCodeAuthorizationExpiredService = idpayCodeAuthorizationExpiredService;
    }
        @Override
    public AuthPaymentDTO authPayment(String trxId, String acquirerId, String merchantFiscalCode, PinBlockDTO pinBlockBody) {
        TransactionInProgress trx = idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(trxId.toLowerCase());
        boolean pinBlock = checkIfthePinBlockIsCorrect(pinBlockBody);
        if (!pinBlock){
            throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN,
                    PaymentConstants.ExceptionCode.REJECTED,
                    "The Pinblock is incorrect");
        }else {
            return super.authPayment(trx,trx.getUserId(), trxId);
        }

    }

    private boolean checkIfthePinBlockIsCorrect(PinBlockDTO pinBlockBody) {
        return pinBlockBody.getEncryptedPinBlock().equals("21341");//TODO gestire pinblock
    }
}
