package it.gov.pagopa.payment.service;

public interface PDVService {
    String encryptCF(String fiscalCode);
    String decryptCF(String userId);
}
