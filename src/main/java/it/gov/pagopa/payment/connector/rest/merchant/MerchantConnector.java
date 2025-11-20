package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;

public interface MerchantConnector {
    MerchantDetailDTO merchantDetail(String merchantId, String initiativeId);

    // posType
}
