package it.gov.pagopa.payment.connector.rest.register;

import it.gov.pagopa.payment.connector.rest.register.dto.ProductCategories;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductStatus;
import jakarta.validation.constraints.Pattern;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static it.gov.pagopa.payment.connector.rest.register.dto.ValidationPatterns.*;

@FeignClient(
        name = "register",
        url = "${rest-client.register.baseUrl}")
public interface RegisterRestClient {
    @GetMapping(
            value = "/idpay/register/products",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ProductListDTO getProductList(@RequestHeader(value = "x-organization-role", required = false, defaultValue = "operatore") @Pattern(regexp = ROLE_PATTERN) String role,
                                  @RequestParam(required = false) @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
                                  @RequestParam(required = false) @Pattern(regexp = ANY_TEXT) String productName,
                                  @RequestParam(required = false) @Pattern(regexp = OBJECT_ID_PATTERN) String productFileId,
                                  @RequestParam(required = false) @Pattern(regexp = DIGITS_ONLY) String eprelCode,
                                  @RequestParam(required = false) @Pattern(regexp = GTIN_CODE) String gtinCode,
                                  @RequestParam(required = false) ProductStatus status,
                                  @RequestParam(required = false) ProductCategories category,
                                  @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable
    );
}
