package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.dto.SecondFactorDTO;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.exception.custom.IdpaycodeNotFoundException;
import it.gov.pagopa.payment.exception.custom.PaymentInstrumentInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        IdpaycodeNotFoundException exception = assertThrows(IdpaycodeNotFoundException.class, () -> paymentInstrumentConnector.getSecondFactor(USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(ExceptionCode.IDPAYCODE_NOT_FOUND, exception.getCode());
        assertEquals("There is not a IDPay Code for the current user", exception.getMessage());

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
        PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class, () -> paymentInstrumentConnector.getSecondFactor(USER_ID));

        // Then
        Assertions.assertNotNull(exception);

        verify(paymentInstrumentRestClient, times(1)).getSecondFactor(USER_ID);
    }
}