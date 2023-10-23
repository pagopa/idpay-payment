package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentConnectorImplTest {
    private static final String USER_ID = "USERID1";
    private static final String SECOND_FACTOR = "SECONDFACTOR";

    @InjectMocks
    private PaymentInstrumentConnectorImpl paymentInstrumentConnector;

    @Mock
    private PaymentInstrumentRestClient paymentInstrumentRestClient;

    @Test
    void getSecondFactor() {
        // Given
        SecondFactorDTO secondFactorDTO = new SecondFactorDTO(SECOND_FACTOR);

        when(paymentInstrumentRestClient.getSecondFactor(USER_ID))
                .thenReturn(secondFactorDTO);

        // When
        SecondFactorDTO result = paymentInstrumentConnector.getSecondFactor(USER_ID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(secondFactorDTO, result);

        verify(paymentInstrumentRestClient, times(1)).getSecondFactor(anyString());

    }

    @Test
    void getSecondFactor_feignException404(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.NotFound("", request, null, null);

        when(paymentInstrumentRestClient.getSecondFactor(USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, () -> paymentInstrumentConnector.getSecondFactor(USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals("PAYMENT_INSTRUMENT", exception.getCode());
        assertEquals(String.format("There is not a idpaycode for the userId %s", USER_ID), exception.getMessage());

        verify(paymentInstrumentRestClient, times(1)).getSecondFactor(USER_ID);
    }

    @Test
    void getSecondFactor_feignException(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.BadRequest("", request, null, null);

        when(paymentInstrumentRestClient.getSecondFactor(USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionNoBody exception = assertThrows(ClientExceptionNoBody.class, () -> paymentInstrumentConnector.getSecondFactor(USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());

        verify(paymentInstrumentRestClient, times(1)).getSecondFactor(USER_ID);
    }
}