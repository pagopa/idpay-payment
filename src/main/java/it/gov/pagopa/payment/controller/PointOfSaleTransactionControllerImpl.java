package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.service.PointOfSaleTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PointOfSaleTransactionControllerImpl implements PointOfSaleTransactionController {

    private final PointOfSaleTransactionService pointOfSaleTransactionService;

    public PointOfSaleTransactionControllerImpl(PointOfSaleTransactionService pointOfSaleTransactionService) {
        this.pointOfSaleTransactionService = pointOfSaleTransactionService;
    }

    @Override
    public PointOfSaleTransactionsListDTO getPointOfSaleTransactions(String merchantId, String initiativeId, String pointOfSaleId, String fiscalCode, String status, Pageable pageable) {
        log.info("[GET_POINT-OF-SALE_TRANSACTIONS] Point of sale {} requested to retrieve transactions", pointOfSaleId);
        return pointOfSaleTransactionService.getPointOfSaleTransactions(merchantId, initiativeId, pointOfSaleId, fiscalCode, status, pageable);
    }
}
