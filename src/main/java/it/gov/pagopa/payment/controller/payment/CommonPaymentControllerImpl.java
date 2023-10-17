package it.gov.pagopa.payment.controller.payment;

import it.gov.pagopa.common.performancelogger.PerformanceLog;
import it.gov.pagopa.payment.dto.common.BaseTransactionResponseDTO;
import it.gov.pagopa.payment.dto.qrcode.SyncTrxStatusDTO;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.service.payment.common.CommonCreationServiceImpl;
import it.gov.pagopa.payment.service.payment.common.CommonStatusTransactionServiceImpl;
import it.gov.pagopa.payment.service.performancelogger.BaseTransactionResponseDTOPerfLoggerPayloadBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
public class CommonPaymentControllerImpl implements CommonPaymentController {
    private final CommonCreationServiceImpl commonCreationService;
    private final CommonStatusTransactionServiceImpl commonStatusTransactionService;

    public CommonPaymentControllerImpl(@Qualifier("CommonCreate") CommonCreationServiceImpl commonCreationService,
                                       CommonStatusTransactionServiceImpl commonStatusTransactionService) {
        this.commonCreationService = commonCreationService;
        this.commonStatusTransactionService = commonStatusTransactionService;
    }

    @Override
    @PerformanceLog(
            value = "CREATE_TRANSACTION",
            payloadBuilderBeanClass = BaseTransactionResponseDTOPerfLoggerPayloadBuilder.class)
    public BaseTransactionResponseDTO createTransaction(
            TransactionCreationRequest trxCreationRequest,
            String merchantId,
            String acquirerId,
            String idTrxIssuer) {
        log.info("[CREATE_TRANSACTION] The merchant {} through acquirer {} is creating a transaction", merchantId, acquirerId);
        return commonCreationService.createTransaction(trxCreationRequest,null,merchantId,acquirerId,idTrxIssuer);
    }

    @Override
    @PerformanceLog("GET_STATUS_TRANSACTION")
    public SyncTrxStatusDTO getStatusTransaction(String transactionId, String merchantId, String acquirerId) {
        log.info("[GET_STATUS_TRANSACTION] Merchant with {},{} requested to retrieve status of transaction{} ", merchantId, acquirerId, transactionId);
        return commonStatusTransactionService.getStatusTransaction(transactionId, merchantId, acquirerId);
    }

}
