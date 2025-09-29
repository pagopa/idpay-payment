package it.gov.pagopa.payment.service.pdf;


public interface PdfService {
    String create(String initiativeId, String trxCode, String userId);
}
