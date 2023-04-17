package it.gov.pagopa.payment.model.error;

import lombok.Builder;


@Builder
public class ProblemError {
    private String code;
    private String detail;
}
