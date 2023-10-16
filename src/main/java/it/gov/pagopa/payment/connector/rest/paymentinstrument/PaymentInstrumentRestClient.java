package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "payment-instrument",
        url = "${rest-client.payment-instrument.baseUrl}")
public interface PaymentInstrumentRestClient {

    @GetMapping(
            value = "/idpay/instrument/code/secondFactor/{userId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    SecondFactorDTO getSecondFactor(@PathVariable("userId") String userId);


    @PutMapping(value = "/idpay/instrument/code/verify/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    VerifyPinBlockDTO verifyPinBlock(@RequestBody PinBlockDTO pinBlockDTO,
                                     @PathVariable("userId") String userId);
}
