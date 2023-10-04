package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.UserRelateResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class RelateUserResponseMapper {
    public UserRelateResponse transactionMapper(TransactionInProgress transaction) {
        return UserRelateResponse.builder()
                .id(transaction.getId())
                .status(transaction.getStatus())
                .build();
    }
}
