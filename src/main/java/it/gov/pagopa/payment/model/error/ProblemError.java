package it.gov.pagopa.payment.model.error;

import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class ProblemError {
    private String field;
    private String defaultMessage;
}
