package it.gov.pagopa.payment.connector.rest;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.connector.rest.register.RegisterConnectorImpl;
import it.gov.pagopa.payment.connector.rest.register.RegisterRestClient;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductRequestDTO;
import it.gov.pagopa.payment.exception.custom.ProductInvocationException;
import it.gov.pagopa.payment.exception.custom.ProductNotFoundException;
import it.gov.pagopa.payment.test.fakers.ProductListDTOFaker;
import it.gov.pagopa.payment.test.fakers.ProductRequestDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterConnectorImplTest {

    @InjectMocks
    private RegisterConnectorImpl connectorImpl;

    @Mock
    private RegisterRestClient restClient;


    @Test
    void getProductList(){
        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();
        ProductListDTO productListDTO = ProductListDTOFaker.mockInstance();

        when(restClient.getProductList(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(productListDTO);

        assertNotNull(connectorImpl.getProductList(productRequestDTOFaker));
    }

    @Test
    void getProductList_ko404(){
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.NotFound("", request, null, null);

        when(restClient.getProductList(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(feignExceptionMock);

        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> connectorImpl.getProductList(productRequestDTOFaker));

        Assertions.assertNotNull(exception);
    }

    @Test
    void getProductList_ko500(){
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.BadRequest("", request, null, null);

        when(restClient.getProductList(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(feignExceptionMock);
        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();

        ProductInvocationException exception = assertThrows(ProductInvocationException.class,
                () -> connectorImpl.getProductList(productRequestDTOFaker));

        Assertions.assertNotNull(exception);
    }

}
