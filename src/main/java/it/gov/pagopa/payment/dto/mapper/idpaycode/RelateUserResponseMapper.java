package it.gov.pagopa.payment.dto.mapper.idpaycode;

import it.gov.pagopa.payment.dto.idpaycode.RelateUserResponse;
import it.gov.pagopa.payment.entity.Transaction;
import org.springframework.stereotype.Service;

@Service
public class RelateUserResponseMapper {
    public RelateUserResponse transactionMapper(Transaction transaction) {
        return RelateUserResponse.builder()
                .id(transaction.getId())
                .status(transaction.getStatus())
                .build();
    }
}
