package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "merchant",
        url = "${rest-client.merchant.baseUrl}")
public interface MerchantRestClient {
    @GetMapping(
            value = "merchant/{merchantId}/initiative/{initiativeId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    MerchantDetailDTO merchantDetail(@PathVariable("merchantId") String merchantId,
                                      @PathVariable("initiativeId") String initiativeId);
}
