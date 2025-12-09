package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.connector.rest.merchant.dto.PointOfSaleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "merchant",
        url = "${rest-client.merchant.baseUrl}")
public interface MerchantRestClient {
    @GetMapping(
            value = "/idpay/merchant/portal/initiatives/{initiativeId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    MerchantDetailDTO merchantDetail(@RequestHeader("x-merchant-id") String merchantId,
                                     @PathVariable("initiativeId") String initiativeId);

    @GetMapping(
        value = "/idpay/merchant/portal/{merchantId}/point-of-sales/{pointOfSaleId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    PointOfSaleDTO getPointOfSale(
        @PathVariable("merchantId") String merchantId,
        @PathVariable("pointOfSaleId") String pointOfSaleId
    );
}
