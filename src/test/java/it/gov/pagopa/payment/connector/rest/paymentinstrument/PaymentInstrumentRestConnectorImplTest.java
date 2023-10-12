package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import it.gov.pagopa.payment.test.fakers.PinBlockDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentRestConnectorImplTest {
    @InjectMocks
    PaymentInstrumentRestConnectorImpl paymentInstrumentRestConnectorImplMock;
    @Mock
    private PaymentInstrumentRestClient paymentInstrumentRestClientMock;
    private static final String USER_ID = "USERID1";
    private static PinBlockDTO  pinBlockDTO;
    @Test
    void checkPinBlock(){
        //Given
        VerifyPinBlockDTO verifyPinBlockDTO = new VerifyPinBlockDTO(true);
        pinBlockDTO = PinBlockDTOFaker.mockInstanceBuilder().build();

        Mockito.when(paymentInstrumentRestClientMock.verifyPinBlock(pinBlockDTO,USER_ID)).thenReturn(verifyPinBlockDTO);

        //when
        VerifyPinBlockDTO result= paymentInstrumentRestConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID);

        //Then
        Assertions.assertNotNull(result);
        assertEquals(verifyPinBlockDTO.isPinBlockVerified(), result.isPinBlockVerified());

        Mockito.verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }

    @Test
    void checkPinBlock_feignException403(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.Forbidden("", request, null, null);

        when(paymentInstrumentRestClientMock.verifyPinBlock(pinBlockDTO,USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, () ->   paymentInstrumentRestConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals("INVALID_PIN", exception.getCode());
        assertEquals(("The Pinblock is incorrect"), exception.getMessage());

        verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }

    @Test
    void checkPinBlock_feignException(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.BadRequest("", request, null, null);

        when(paymentInstrumentRestClientMock.verifyPinBlock(pinBlockDTO,USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionNoBody exception = assertThrows(ClientExceptionNoBody.class, () -> paymentInstrumentRestConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());

        verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }
}