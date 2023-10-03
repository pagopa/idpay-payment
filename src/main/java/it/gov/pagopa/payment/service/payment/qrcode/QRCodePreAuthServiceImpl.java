package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.constants.PaymentConstants;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonPreAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodePreAuthServiceImpl implements QRCodePreAuthService {
  private final TransactionInProgressRepository transactionInProgressRepository;
  private final CommonPreAuthService commonPreAuthService;

  public QRCodePreAuthServiceImpl(
          TransactionInProgressRepository transactionInProgressRepository,
          CommonPreAuthService commonPreAuthService) {
    this.transactionInProgressRepository = transactionInProgressRepository;
    this.commonPreAuthService = commonPreAuthService;
  }

  @Override
  public AuthPaymentDTO relateUser(String trxCode, String userId) {
    TransactionInProgress trx = transactionInProgressRepository.findByTrxCode(trxCode.toLowerCase())
            .orElseThrow(() -> new ClientExceptionWithBody(
                    HttpStatus.NOT_FOUND,
                    PaymentConstants.ExceptionCode.TRX_NOT_FOUND_OR_EXPIRED,
                    "Cannot find transaction with trxCode [%s]".formatted(trxCode)));

    commonPreAuthService.relateUser(trx, userId);
    return commonPreAuthService.previewPayment(trx, userId);

  }

}
