package it.gov.pagopa.payment.connector.rest.merchant;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "merchant",
        url = "${rest-client.merchant.baseUrl}") //TODO set in application yml
public interface MerchantRestClient {
    //TODO
    @PostMapping(
            value = "/{merchantId}/initiative/{initiativeId}", //TODO TBV
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    MerchantDetailsDTO merchantDetails(@PathVariable("merchantId") String merchantId,
                                        @PathVariable("initiativeId") String initiativeId);
}
