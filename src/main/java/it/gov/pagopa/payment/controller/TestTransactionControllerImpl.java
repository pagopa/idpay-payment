package it.gov.pagopa.payment.controller;

import it.gov.pagopa.payment.service.payment.TestTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public class TestTransactionControllerImpl implements TestTransactionController {

    private final TestTransactionService testTransactionService;

    @Override
    public ResponseEntity<Void> updateTrxEndDate(
            String id, String initiativeId, String userId, OffsetDateTime newEndDate) {
        testTransactionService.updateTrxEndDate(id, initiativeId, userId, newEndDate);
        return ResponseEntity.noContent().build();
    }
}
