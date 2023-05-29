package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailsDTO;
import org.springframework.web.bind.annotation.PathVariable;

public interface MerchantConnector {
    MerchantDetailsDTO merchantDetails(@PathVariable("merchantId") String merchantId,
                                       @PathVariable("initiativeId") String initiativeId);
}
