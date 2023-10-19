package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorConnector;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonCancelServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeCancelServiceImpl extends CommonCancelServiceImpl implements QRCodeCancelService {


    public QRCodeCancelServiceImpl(
            @Value("${app.qrCode.expirations.cancelMinutes}") long cancelExpirationMinutes,

            TransactionInProgressRepository repository,
            RewardCalculatorConnector rewardCalculatorConnector,
            TransactionNotifierService notifierService,
            PaymentErrorNotifierService paymentErrorNotifierService,
            AuditUtilities auditUtilities) {
        super(cancelExpirationMinutes, repository, rewardCalculatorConnector, notifierService, paymentErrorNotifierService, auditUtilities);
    }

}
