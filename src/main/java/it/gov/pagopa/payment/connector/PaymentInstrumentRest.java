package it.gov.pagopa.payment.connector;

import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "${rest-client.payment-instrument.serviceCode}", url = "${rest-client.payment-instrument.baseUrl}")
public interface PaymentInstrumentRest {
    @GetMapping(value = "/code/verify/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    VerifyPinBlockDTO verifyPinBlock(@RequestBody PinBlockDTO pinBlockDTO,
                                     @PathVariable("userId") String userId);
}
