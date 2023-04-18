package it.gov.pagopa.payment.model.error;

import it.gov.pagopa.payment.dto.ErrorDTO;
import java.util.List;
import lombok.Data;

@Data
public class Problem {
    private Integer status;
    private List<ErrorDTO> errors;
}
