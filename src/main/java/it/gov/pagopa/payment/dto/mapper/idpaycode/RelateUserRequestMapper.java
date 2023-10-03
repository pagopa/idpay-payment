package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class RelateUserRequestMapper {
    public UserRelateResponse transactionMapper(TransactionInProgress transaction) {
        return UserRelateResponse.builder()
                .id(transaction.getId())
                .initiativeId(transaction.getInitiativeId())
                .initiativeName(transaction.getInitiativeName())
                .businessName(transaction.getBusinessName())
                .status(transaction.getStatus())
                .trxCode(transaction.getTrxCode())
                .trxDate(transaction.getTrxDate())
                .build();
    }
}
