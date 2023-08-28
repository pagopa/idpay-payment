package it.gov.pagopa.payment.connector.rest.wallet;

import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "wallet",
        url = "${rest-client.wallet.baseUrl}")
public interface WalletRestClient {

    @GetMapping(
            value = "/idpay/wallet/{initiativeId}/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    WalletDTO getWallet(@PathVariable("initiativeId") String initiativeId,
                             @PathVariable("userId") String userId);

}
