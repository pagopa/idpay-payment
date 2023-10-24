package it.gov.pagopa.payment.connector.rest;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnectorImpl;
import it.gov.pagopa.payment.connector.rest.wallet.WalletRestClient;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletConnectorImplTest {

    @InjectMocks
    private WalletConnectorImpl walletConnectorImpl;

    @Mock
    private WalletRestClient walletRestClient;

    private static final String USER_ID = "USERID1";
    private static final String INITIATIVE_ID = "INITIATIVEID1";

    @Test
    void getWallet(){
        // Given
        WalletDTO walletDTOMock = WalletDTOFaker.mockInstance(1, "REFUNDABLE");

        when(walletRestClient.getWallet(INITIATIVE_ID, USER_ID))
                .thenReturn(walletDTOMock);

        // When
        WalletDTO result = walletConnectorImpl.getWallet(INITIATIVE_ID, USER_ID);

        // Then
        Assertions.assertNotNull(result);
        assertEquals(walletDTOMock.getInitiativeId(), result.getInitiativeId());
        assertEquals(walletDTOMock.getInitiativeName(), result.getInitiativeName());
        assertEquals(walletDTOMock.getStatus(), result.getStatus());

        verify(walletRestClient, times(1)).getWallet(INITIATIVE_ID, USER_ID);
    }

    @Test
    void getWallet_feignException404(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.NotFound("", request, null, null);

        when(walletRestClient.getWallet(INITIATIVE_ID, USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, () -> walletConnectorImpl.getWallet(INITIATIVE_ID, USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals(PaymentConstants.ExceptionCode.USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format("The user is not onboarded on initiative [%s].", INITIATIVE_ID), exception.getMessage());

        verify(walletRestClient, times(1)).getWallet(INITIATIVE_ID, USER_ID);
    }

    @Test
    void getWallet_feignException(){
        // Given
        Request request = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        FeignException feignExceptionMock = new FeignException.BadRequest("", request, null, null);

        when(walletRestClient.getWallet(INITIATIVE_ID, USER_ID))
                .thenThrow(feignExceptionMock);

        // When
        ClientExceptionNoBody exception = assertThrows(ClientExceptionNoBody.class, () -> walletConnectorImpl.getWallet(INITIATIVE_ID, USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());

        verify(walletRestClient, times(1)).getWallet(INITIATIVE_ID, USER_ID);
    }

}
