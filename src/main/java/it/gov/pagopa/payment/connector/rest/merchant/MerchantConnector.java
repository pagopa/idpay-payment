package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface MerchantConnector {
    MerchantDetailDTO merchantDetail(@RequestHeader("x-merchant-id") String merchantId,
                                     @PathVariable("initiativeId") String initiativeId);
}
