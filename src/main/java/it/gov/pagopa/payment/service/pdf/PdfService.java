package it.gov.pagopa.payment.service.pdf;


import it.gov.pagopa.payment.dto.ReportDTO;

public interface PdfService {
    ReportDTO create(String initiativeId, String trxCode, String userId, String username, String fiscalCode);
    ReportDTO createPreauthPdf(String initiativeId, String transactionId, String fiscalCode);
}
