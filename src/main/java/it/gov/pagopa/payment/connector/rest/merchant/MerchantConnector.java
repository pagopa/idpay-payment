package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import org.springframework.web.bind.annotation.PathVariable;

public interface MerchantConnector {
    MerchantDetailDTO merchantDetail(@PathVariable("merchantId") String merchantId,
                                      @PathVariable("initiativeId") String initiativeId);
}
