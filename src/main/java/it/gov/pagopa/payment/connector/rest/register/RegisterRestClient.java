package it.gov.pagopa.payment.connector.rest.register;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import jakarta.validation.constraints.Pattern;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static it.gov.pagopa.payment.connector.rest.register.dto.ValidationPatterns.GTIN_CODE;

@FeignClient(
        name = "register",
        url = "${rest-client.register.baseUrl}")
public interface RegisterRestClient {
    @GetMapping(
            value = "/idpay/register/products",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProductListDTO getProductList(
                                  @RequestParam(required = false) @Pattern(regexp = GTIN_CODE) String gtinCode,
                                  @RequestParam(required = false) ProductStatus status
    );
}
