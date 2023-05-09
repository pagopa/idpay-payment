package it.gov.pagopa.payment.connector.event.producer.mapper;

import it.gov.pagopa.payment.connector.event.producer.dto.AuthorizationNotificationDTO;
import it.gov.pagopa.payment.dto.AuthPaymentDTO;
import it.gov.pagopa.payment.enums.SyncTrxStatus;
import it.gov.pagopa.payment.model.TransactionInProgress;
import it.gov.pagopa.payment.test.fakers.AuthPaymentDTOFaker;
import it.gov.pagopa.payment.test.fakers.TransactionInProgressFaker;
import it.gov.pagopa.payment.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationNotificationMapperTest {

    private AuthorizationNotificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AuthorizationNotificationMapper();
    }

    @Test
    void testMapper() {
        TransactionInProgress transaction = TransactionInProgressFaker.mockInstance(1,
                SyncTrxStatus.AUTHORIZED);
        transaction.setAuthDate(LocalDateTime.now());
        transaction.setUserId("USERID%d".formatted(1));
        AuthPaymentDTO authPaymentDTO = AuthPaymentDTOFaker.mockInstance(1, transaction);
        AuthorizationNotificationDTO result = mapper.map(transaction, authPaymentDTO);

        assertAll(() -> {
            assertEquals(transaction.getId(), result.getTrxId());
            assertEquals(transaction.getInitiativeId(), result.getInitiativeId());
            assertEquals(transaction.getUserId(), result.getUserId());
            assertEquals(transaction.getTrxDate(), result.getTrxDate());
            assertEquals(transaction.getMerchantId(), result.getMerchantId());
            assertEquals(transaction.getMerchantFiscalCode(), result.getMerchantFiscalCode());
            assertEquals(authPaymentDTO.getStatus(), result.getStatus());
            assertEquals(authPaymentDTO.getReward(), result.getReward());
            assertEquals(transaction.getAmountCents(), result.getAmountCents());
            assertEquals(authPaymentDTO.getRejectionReasons(), result.getRejectionReasons());
            TestUtils.checkNotNullFields(result);
        });
    }
}
