package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.connector.decrypt.DecryptRestConnector;
import it.gov.pagopa.payment.connector.encrypt.EncryptRestConnector;
import it.gov.pagopa.payment.dto.CFDTO;
import it.gov.pagopa.payment.exception.custom.PDVInvocationException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class PDVServiceImpl implements PDVService {

    private final DecryptRestConnector decryptRestConnector;
    private final EncryptRestConnector encryptRestConnector;

    public PDVServiceImpl(DecryptRestConnector decryptRestConnector, EncryptRestConnector encryptRestConnector) {
        this.decryptRestConnector = decryptRestConnector;
        this.encryptRestConnector = encryptRestConnector;
    }

    @Override
    public String encryptCF(String fiscalCode) {
        return wrapPDVCall(() -> encryptRestConnector.upsertToken(new CFDTO(fiscalCode)).getToken(),
                "An error occurred during encryption");
    }

    @Override
    public String decryptCF(String userId) {
        return wrapPDVCall(() -> decryptRestConnector.getPiiByToken(userId).getPii(),
                "An error occurred during decryption");
    }

    private <T> T wrapPDVCall(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            throw new PDVInvocationException(errorMessage, true, e);
        }
    }
}
