package it.gov.pagopa.payment.service.payment.idpaycode;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentConnectorImpl;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.payment.exception.custom.TransactionNotFoundOrExpiredException;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.payment.common.CommonAuthServiceImpl;
import it.gov.pagopa.payment.service.payment.idpaycode.expired.IdpayCodeAuthorizationExpiredService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class IdpayCodeAuthPaymentServiceImpl implements IdpayCodeAuthPaymentService {
    private final IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService;
    private final PaymentInstrumentConnectorImpl paymentInstrumentConnector;
    private final CommonAuthServiceImpl commonAuthService;
    protected IdpayCodeAuthPaymentServiceImpl(IdpayCodeAuthorizationExpiredService idpayCodeAuthorizationExpiredService,
                                              PaymentInstrumentConnectorImpl paymentInstrumentConnector, CommonAuthServiceImpl commonAuthService) {
        this.idpayCodeAuthorizationExpiredService = idpayCodeAuthorizationExpiredService;
        this.paymentInstrumentConnector = paymentInstrumentConnector;
        this.commonAuthService = commonAuthService;
    }
    @Override
    public AuthPaymentDTO authPayment(String trxId, String merchantId, PinBlockDTO pinBlockBody) {
        TransactionInProgress trx = idpayCodeAuthorizationExpiredService.findByTrxIdAndAuthorizationNotExpired(trxId);

        if(trx == null){
            throw new TransactionNotFoundOrExpiredException("Cannot find transaction with transactionId [%s]".formatted(trxId));
        }

        if(trx.getUserId() == null){
            throw new OperationNotAllowedException(ExceptionCode.TRX_USER_NOT_ASSOCIATED, "User not associated to transaction with transactionId [%s]".formatted(trxId));
        }

        // payment-instrument call to check pinBlock
        paymentInstrumentConnector.checkPinBlock(pinBlockBody,trx.getUserId());

        if (!merchantId.equals(trx.getMerchantId())){
            throw new MerchantOrAcquirerNotAllowedException("The merchant with id [%s] associated to the transaction is not equal to the merchant with id [%s]".formatted(trx.getMerchantId(),merchantId));
        }
        return commonAuthService.authPayment(trx,trx.getUserId(),trx.getTrxCode());

    }
}
