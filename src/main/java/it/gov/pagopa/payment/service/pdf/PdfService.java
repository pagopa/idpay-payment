package it.gov.pagopa.payment.service.pdf;


import it.gov.pagopa.payment.dto.ReportDTO;
import it.gov.pagopa.payment.dto.ReportDTOWithTrxCode;

public interface PdfService {
    ReportDTO create(String initiativeId, String trxCode, String userId, String username, String fiscalCode);
    ReportDTOWithTrxCode createPreauthPdf(String transactionId);
}
