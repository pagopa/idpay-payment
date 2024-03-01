package it.gov.pagopa.payment.connector.decrypt;

import it.gov.pagopa.payment.dto.DecryptCfDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecryptRestConnectorTest {

    @Mock
    private DecryptRest decryptRest;

    @Test
    void testGetPiiByToken() {
        DecryptRestConnectorImpl decryptRestConnectorImpl = new DecryptRestConnectorImpl("apikey",decryptRest);
        DecryptCfDTO decryptCfDTO = new DecryptCfDTO("Pii");
        when(decryptRest.getPiiByToken(any(), any())).thenReturn(decryptCfDTO);
        assertSame(decryptCfDTO, decryptRestConnectorImpl.getPiiByToken("ABC123"));
        verify(decryptRest).getPiiByToken(any(), any());
    }
}

