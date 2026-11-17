package it.gov.pagopa.payment.service.payment;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.payment.repository.TestTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test-support.enabled", havingValue = "true")
public class TestTransactionServiceImpl implements TestTransactionService {

    private final TestTransactionRepository testTransactionRepository;

    @Override
    public void updateTrxEndDate(
            String id, String initiativeId, String userId, OffsetDateTime newEndDate) {
        if (testTransactionRepository.updateTrxEndDate(id, initiativeId, userId, newEndDate) == 0) {
            throw new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "Transaction not found");
        }
        log.info("Updated transaction end date");
    }
}
