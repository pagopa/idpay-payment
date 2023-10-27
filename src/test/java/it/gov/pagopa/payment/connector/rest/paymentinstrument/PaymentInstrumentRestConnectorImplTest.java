package it.gov.pagopa.payment.connector.rest.paymentinstrument;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.exception.custom.forbidden.PinBlockInvalidException;
import it.gov.pagopa.payment.exception.custom.servererror.PaymentInstrumentInvocationException;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.constants.PaymentConstants.ExceptionCode;
import it.gov.pagopa.payment.dto.PinBlockDTO;
import it.gov.pagopa.payment.dto.VerifyPinBlockDTO;
import it.gov.pagopa.payment.test.fakers.PinBlockDTOFaker;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentRestConnectorImplTest {
    @InjectMocks
    PaymentInstrumentConnectorImpl paymentInstrumentConnectorImplMock;
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
        VerifyPinBlockDTO result= paymentInstrumentConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID);

        //Then
        Assertions.assertNotNull(result);
        assertEquals(verifyPinBlockDTO.isPinBlockVerified(), result.isPinBlockVerified());

        Mockito.verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }

    @Test
    void checkPinBlock_feignException403(){
        // Given
        when(paymentInstrumentRestClientMock.verifyPinBlock(pinBlockDTO,USER_ID))
                .thenReturn(new VerifyPinBlockDTO(false));

        // When
        PinBlockInvalidException exception = assertThrows(PinBlockInvalidException.class, () ->
            paymentInstrumentConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(PaymentConstants.ExceptionCode.INVALID_PIN, exception.getCode());
        assertEquals("The Pinblock is incorrect", exception.getMessage());

        verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }

    @Test
    void checkPinBlock_feignException(){
        // Given
        Request request = Request.create(Request.HttpMethod.PUT, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.BadRequest("", request, null, null);

        when(paymentInstrumentRestClientMock.verifyPinBlock(pinBlockDTO,USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class, () -> paymentInstrumentConnectorImplMock.checkPinBlock(pinBlockDTO,USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(ExceptionCode.GENERIC_ERROR, exception.getCode());

        verify(paymentInstrumentRestClientMock).verifyPinBlock(pinBlockDTO,USER_ID);
    }
}