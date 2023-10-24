package it.gov.pagopa.payment.service.payment.qrcode;

import it.gov.pagopa.payment.connector.event.trx.TransactionNotifierService;
import it.gov.pagopa.payment.dto.mapper.TransactionInProgress2TransactionResponseMapper;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import it.gov.pagopa.payment.service.PaymentErrorNotifierService;
import it.gov.pagopa.payment.service.payment.common.CommonConfirmServiceImpl;
import it.gov.pagopa.payment.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QRCodeConfirmationServiceImpl extends CommonConfirmServiceImpl implements QRCodeConfirmationService {

    public QRCodeConfirmationServiceImpl(TransactionInProgressRepository repository,
                                         TransactionInProgress2TransactionResponseMapper mapper,
                                         TransactionNotifierService notifierService,
                                         PaymentErrorNotifierService paymentErrorNotifierService,
                                         AuditUtilities auditUtilities) {
        super(repository, mapper, notifierService, paymentErrorNotifierService, auditUtilities);
    }
}
