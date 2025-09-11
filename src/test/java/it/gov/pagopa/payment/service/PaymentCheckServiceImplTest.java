package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.rest.register.RegisterConnector;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductListDTO;
import it.gov.pagopa.payment.connector.rest.register.dto.ProductRequestDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.ProductNotValidException;
import it.gov.pagopa.payment.service.payment.PaymentCheckService;
import it.gov.pagopa.payment.service.payment.PaymentCheckServiceImpl;
import it.gov.pagopa.payment.test.fakers.ProductListDTOFaker;
import it.gov.pagopa.payment.test.fakers.ProductRequestDTOFaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCheckServiceImplTest {

    @Mock
    RegisterConnector registerConnector;

    PaymentCheckService paymentCheckService;

    @BeforeEach
    void setup() {
        paymentCheckService = new PaymentCheckServiceImpl(registerConnector);
    }

    @Test
    void validateProduct_ok(){
        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();

        ProductListDTO productListDTO = ProductListDTOFaker.mockInstance();
        when(registerConnector.getProductList(any())).thenReturn(productListDTO);
        assertNotNull(paymentCheckService.validateProduct(productRequestDTOFaker));
    }

    @Test
    void validateProduct_ko(){
        ProductListDTO productListDTO = null;
        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();

        when(registerConnector.getProductList(any())).thenReturn(productListDTO);

        ProductNotValidException exception = assertThrows(ProductNotValidException.class,
                () -> paymentCheckService.validateProduct(productRequestDTOFaker));

        assertEquals(PaymentConstants.ExceptionCode.PRODUCT_NOT_FOUND, exception.getCode());
    }

    @Test
    void validateProduct_ko2(){
        ProductListDTO productListDTO = ProductListDTOFaker.mockInstance();
        productListDTO.setContent(new ArrayList<>());
        ProductRequestDTO productRequestDTOFaker = ProductRequestDTOFaker.mockInstance();

        when(registerConnector.getProductList(any())).thenReturn(productListDTO);

        ProductNotValidException exception = assertThrows(ProductNotValidException.class,
                () -> paymentCheckService.validateProduct(productRequestDTOFaker));

        assertEquals(PaymentConstants.ExceptionCode.PRODUCT_NOT_FOUND, exception.getCode());
    }


}
