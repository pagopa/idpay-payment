package it.gov.pagopa.payment.connector.rest;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.payment.exception.custom.servererror.WalletInvocationException;
import it.gov.pagopa.payment.connector.rest.wallet.WalletConnectorImpl;
import it.gov.pagopa.payment.connector.rest.wallet.WalletRestClient;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.test.fakers.WalletDTOFaker;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class, () -> walletConnectorImpl.getWallet(INITIATIVE_ID, USER_ID));

        // Then
        Assertions.assertNotNull(exception);
        assertEquals(PaymentConstants.ExceptionCode.USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format("The current user is not onboarded on initiative [%s]", INITIATIVE_ID), exception.getMessage());

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
        WalletInvocationException exception = assertThrows(WalletInvocationException.class, () -> walletConnectorImpl.getWallet(INITIATIVE_ID, USER_ID));

        // Then
        Assertions.assertNotNull(exception);

        verify(walletRestClient, times(1)).getWallet(INITIATIVE_ID, USER_ID);
    }

}
