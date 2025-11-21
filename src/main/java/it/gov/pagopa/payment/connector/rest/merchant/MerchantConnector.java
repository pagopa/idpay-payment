package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;

public interface MerchantConnector {
    MerchantDetailDTO merchantDetail(String merchantId, String initiativeId);
    PointOfSaleDTO getPointOfSale(String merchantId, String pointOfSaleId);
}
