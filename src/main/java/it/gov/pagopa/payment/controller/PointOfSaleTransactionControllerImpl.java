package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.dto.PointOfSaleTransactionDTO;
import it.gov.pagopa.payment.dto.PointOfSaleTransactionsListDTO;
import it.gov.pagopa.payment.dto.mapper.PointOfSaleTransactionMapper;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.service.PointOfSaleTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PointOfSaleTransactionControllerImpl implements PointOfSaleTransactionController {

    private final PointOfSaleTransactionService pointOfSaleTransactionService;
    private final PointOfSaleTransactionMapper mapper;

    public PointOfSaleTransactionControllerImpl(PointOfSaleTransactionService pointOfSaleTransactionService, PointOfSaleTransactionMapper mapper) {
        this.pointOfSaleTransactionService = pointOfSaleTransactionService;
        this.mapper = mapper;
    }

    @Override
    public PointOfSaleTransactionsListDTO getPointOfSaleTransactions(String merchantId, String initiativeId, String pointOfSaleId, String fiscalCode, String status, Pageable pageable) {
        log.info("[GET_POINT-OF-SALE_TRANSACTIONS] Point of sale {} requested to retrieve transactions", pointOfSaleId);

        Page<TransactionInProgress> page = pointOfSaleTransactionService.getPointOfSaleTransactions(
                merchantId, initiativeId, pointOfSaleId, fiscalCode, status, pageable);

        List<PointOfSaleTransactionDTO> dtos = page.getContent().stream()
                .map(tx -> mapper.toPointOfSaleTransactionDTO(tx, fiscalCode))
                .toList();

        return new PointOfSaleTransactionsListDTO(
                dtos,
                page.getNumber(),
                page.getSize(),
                (int) page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
