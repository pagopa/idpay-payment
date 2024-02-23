package it.gov.pagopa.payment.connector.rest.merchant;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.exception.custom.MerchantInvocationException;
import it.gov.pagopa.payment.exception.custom.MerchantOrAcquirerNotAllowedException;
import it.gov.pagopa.payment.test.fakers.MerchantDetailDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantConnectorImplTest {

    @InjectMocks
    private MerchantConnectorImpl merchantConnector;

    @Mock
    private MerchantRestClient restClient;

    private static final String MERCHANT_ID = "MERCHANTID1";
    private static final String INITIATIVEID = "INITIATIVEID1";

    @Test
    void getMerchantDetail(){
        // Given
        MerchantDetailDTO merchantDetailDTO = MerchantDetailDTOFaker.mockInstance(1);

        when(restClient.merchantDetail(MERCHANT_ID,INITIATIVEID))
                .thenReturn(merchantDetailDTO);

        // When
        MerchantDetailDTO result = merchantConnector.merchantDetail(MERCHANT_ID,INITIATIVEID);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(merchantDetailDTO, result);

        verify(restClient, times(1)).merchantDetail(anyString(),anyString());
    }

    @Test
    void getMerchantDetailMerchantOrAcquirerNotAllowedException(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.NotFound("", request, null, null);

        when(restClient.merchantDetail(MERCHANT_ID,INITIATIVEID))
                .thenThrow(feignExceptionMock);

        // When
        MerchantOrAcquirerNotAllowedException exception = assertThrows(MerchantOrAcquirerNotAllowedException.class, () -> merchantConnector.merchantDetail(MERCHANT_ID,INITIATIVEID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(PaymentConstants.ExceptionCode.MERCHANT_NOT_ONBOARDED, exception.getCode());

        verify(restClient, times(1)).merchantDetail(MERCHANT_ID,INITIATIVEID);
    }


    @Test
    void getMerchantDetailInvocationException(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.InternalServerError("", request, null, null);

        when(restClient.merchantDetail(MERCHANT_ID,INITIATIVEID))
                .thenThrow(feignExceptionMock);

        // When
        MerchantInvocationException exception = assertThrows(MerchantInvocationException.class, () -> merchantConnector.merchantDetail(MERCHANT_ID,INITIATIVEID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(PaymentConstants.ExceptionCode.GENERIC_ERROR, exception.getCode());

        verify(restClient, times(1)).merchantDetail(MERCHANT_ID,INITIATIVEID);
    }


}
