package it.gov.pagopa.payment.connector.event.producer.mapper;

import it.gov.pagopa.payment.connector.event.producer.dto.AuthorizationNotificationDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationNotificationMapper {

    public AuthorizationNotificationDTO map(TransactionInProgress trx, AuthPaymentDTO authPaymentDTO) {
        return AuthorizationNotificationDTO.builder()
                .operationType(PaymentConstants.AUTH_PAYMENT)
                .trxId(trx.getId())
                .initiativeId(trx.getInitiativeId())
                .userId(trx.getUserId())
                .trxDate(trx.getTrxDate())
                .merchantId(trx.getMerchantId())
                .merchantFiscalCode(trx.getMerchantFiscalCode())
                .status(authPaymentDTO.getStatus())
                .reward(authPaymentDTO.getReward())
                .amountCents(trx.getAmountCents())
                .rejectionReasons(authPaymentDTO.getRejectionReasons())
                .authorizationDateTime(trx.getAuthDate())
                .build();
    }
}
