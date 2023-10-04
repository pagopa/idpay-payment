package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantConnector;
import it.gov.pagopa.payment.dto.mapper.TransactionCreationRequest2TransactionInProgressMapper;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
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
  public QRCodeCreationServiceImpl(TransactionInProgress2TransactionResponseMapper transactionInProgress2TransactionResponseMapper, TransactionCreationRequest2TransactionInProgressMapper transactionCreationRequest2TransactionInProgressMapper, RewardRuleRepository rewardRuleRepository, TransactionInProgressRepository transactionInProgressRepository, TrxCodeGenUtil trxCodeGenUtil, AuditUtilities auditUtilities, MerchantConnector merchantConnector) {
    super(transactionInProgress2TransactionResponseMapper, transactionCreationRequest2TransactionInProgressMapper, rewardRuleRepository, transactionInProgressRepository, trxCodeGenUtil, auditUtilities, merchantConnector);
  }

  @Override
  public String getFlow(){
    return "QR_CODE_CREATE_TRANSACTION";
  }
}
