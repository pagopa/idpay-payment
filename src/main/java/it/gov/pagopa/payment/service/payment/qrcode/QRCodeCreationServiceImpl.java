package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.dto.mapper.BaseTransactionResponse2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionResponse;
import it.gov.pagopa.payment.repository.RewardRuleRepository;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import it.gov.pagopa.payment.utils.TrxCodeGenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QRCodeCreationServiceImpl extends CommonCreationServiceImpl implements QRCodeCreationService {
  private final BaseTransactionResponse2TransactionResponseMapper baseTransactionResponse2TransactionResponseMapper;
  @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
  public QRCodeCreationServiceImpl(TransactionInProgress2TransactionResponseMapper transactionInProgress2BaseTransactionResponseMapper, TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper, RewardRuleRepository rewardRuleRepository, TransactionInProgressRepository transactionInProgressRepository, TrxCodeGenUtil trxCodeGenUtil, AuditUtilities auditUtilities, MerchantConnector merchantConnector, BaseTransactionResponse2TransactionResponseMapper baseTransactionResponse2TransactionResponseMapper) {
    super(transactionInProgress2BaseTransactionResponseMapper, transactionCreationRequest2TransactionInProgressMapper, rewardRuleRepository, transactionInProgressRepository, trxCodeGenUtil, auditUtilities, merchantConnector);
    this.baseTransactionResponse2TransactionResponseMapper = baseTransactionResponse2TransactionResponseMapper;
  }

  public TransactionResponse createQRCodeTransaction(TransactionCreationRequest trxCreationRequest, String channel, String merchantId, String acquirerId, String idTrxIssuer) {
    return super.createTransaction(trxCreationRequest, channel, merchantId, acquirerId, idTrxIssuer);
  }

  @Override
  public String getFlow(){
    return "QR_CODE_CREATE_TRANSACTION";
  }
}
