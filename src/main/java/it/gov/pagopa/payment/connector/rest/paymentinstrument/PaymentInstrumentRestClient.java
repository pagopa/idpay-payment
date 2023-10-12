package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
        name = "payment-instrument",
        url = "${rest-client.payment-instrument.baseUrl}")
public interface PaymentInstrumentRestClient {

    @GetMapping(
            value = "/code/secondFactor/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SecondFactorDTO getSecondFactor(@PathVariable("userId") String userId);

}
