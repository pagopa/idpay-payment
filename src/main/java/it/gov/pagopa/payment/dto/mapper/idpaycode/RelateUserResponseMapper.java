package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.model.TransactionInProgress;
import org.springframework.stereotype.Service;

@Service
public class RelateUserResponseMapper {
    public RelateUserResponse transactionMapper(TransactionInProgress transaction) {
        return RelateUserResponse.builder()
                .id(transaction.getId())
                .status(transaction.getStatus())
                .build();
    }
}
