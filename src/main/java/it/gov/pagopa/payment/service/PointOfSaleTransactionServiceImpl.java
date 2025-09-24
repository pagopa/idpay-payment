package it.gov.pagopa.payment.service;

import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.repository.TransactionInProgressRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class PointOfSaleTransactionServiceImpl implements PointOfSaleTransactionService {

    private final TransactionInProgressRepository transactionInProgressRepository;
    private final PDVService pdvService;

    public PointOfSaleTransactionServiceImpl(
            TransactionInProgressRepository transactionInProgressRepository,
            PDVService pdvService) {
        this.transactionInProgressRepository = transactionInProgressRepository;
        this.pdvService = pdvService;
    }

    @Override
    public Page<TransactionInProgress> getPointOfSaleTransactions(String merchantId, String initiativeId, String pointOfSaleId, String fiscalCode, String status, String productGtin, Pageable pageable) {
        String userId = StringUtils.isNotBlank(fiscalCode) ? pdvService.encryptCF(fiscalCode) : null;
        return transactionInProgressRepository.findPageByFilter(merchantId, pointOfSaleId, initiativeId, userId, status, productGtin, pageable);
    }
}
