package it.gov.pagopa.payment.connector.encrypt;

import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptRestConnectorTest {
    @Mock
    private EncryptRest encryptRest;

    @Test
    void testUpsertToken() {
        EncryptRestConnectorImpl encryptRestConnectorImpl = new EncryptRestConnectorImpl("apikey", encryptRest);
        EncryptedCfDTO encryptedCfDTO = new EncryptedCfDTO("ABC123");
        when(encryptRest.upsertToken(any(), any())).thenReturn(encryptedCfDTO);
        assertSame(encryptedCfDTO, encryptRestConnectorImpl.upsertToken(new CFDTO("Pii")));
        verify(encryptRest).upsertToken(any(), any());
    }
}

