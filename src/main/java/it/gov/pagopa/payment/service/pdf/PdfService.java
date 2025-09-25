package it.gov.pagopa.payment.service.pdf;

public interface PdfService {
    byte[] create(String initiativeId, String trxId, String userId);
}
