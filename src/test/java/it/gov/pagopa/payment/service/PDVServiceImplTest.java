package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.dto.DecryptCfDTO;
import it.gov.pagopa.payment.dto.EncryptedCfDTO;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PDVServiceImplTest {

    @Mock
    DecryptRestConnector decryptRestConnector;

    @Mock
    EncryptRestConnector encryptRestConnector;

    PDVServiceImpl pdvService;

    @BeforeEach
    void setup() {
        pdvService = new PDVServiceImpl(decryptRestConnector, encryptRestConnector);
    }

    @Test
    void encryptCF_success() {
        String fiscalCode = "ABCDEF12G34H567I";
        String expectedToken = "encryptedToken123";

        EncryptedCfDTO encryptedCfDTO = mock(EncryptedCfDTO.class);
        when(encryptedCfDTO.getToken()).thenReturn(expectedToken);
        when(encryptRestConnector.upsertToken(any(CFDTO.class))).thenReturn(encryptedCfDTO);

        String actualToken = pdvService.encryptCF(fiscalCode);

        assertEquals(expectedToken, actualToken);

        ArgumentCaptor<CFDTO> captor = ArgumentCaptor.forClass(CFDTO.class);
        verify(encryptRestConnector).upsertToken(captor.capture());
        assertEquals(fiscalCode, captor.getValue().getPii());
    }

    @Test
    void encryptCF_throwsPDVInvocationException() {
        when(encryptRestConnector.upsertToken(any(CFDTO.class)))
                .thenThrow(new RuntimeException("Fail encryption"));

        PDVInvocationException ex = assertThrows(PDVInvocationException.class,
                () -> pdvService.encryptCF("someFiscalCode"));

        assertTrue(ex.getMessage().contains("An error occurred during encryption"));

        ArgumentCaptor<CFDTO> captor = ArgumentCaptor.forClass(CFDTO.class);
        verify(encryptRestConnector).upsertToken(captor.capture());
        assertEquals("someFiscalCode", captor.getValue().getPii());
    }

    @Test
    void decryptCF_success() {
        String userId = "user123";
        String expectedPii = "decryptedFiscalCode";

        DecryptCfDTO decryptCfDTO = mock(DecryptCfDTO.class);
        when(decryptCfDTO.getPii()).thenReturn(expectedPii);
        when(decryptRestConnector.getPiiByToken(anyString())).thenReturn(decryptCfDTO);

        String actualPii = pdvService.decryptCF(userId);

        assertEquals(expectedPii, actualPii);
        verify(decryptRestConnector).getPiiByToken(userId);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(decryptRestConnector).getPiiByToken(captor.capture());
        assertEquals(userId, captor.getValue());
    }

    @Test
    void decryptCF_throwsPDVInvocationException() {
        when(decryptRestConnector.getPiiByToken(anyString()))
                .thenThrow(new RuntimeException("Fail decryption"));

        PDVInvocationException ex = assertThrows(PDVInvocationException.class,
                () -> pdvService.decryptCF("user123"));

        assertTrue(ex.getMessage().contains("An error occurred during decryption"));
    }
}
