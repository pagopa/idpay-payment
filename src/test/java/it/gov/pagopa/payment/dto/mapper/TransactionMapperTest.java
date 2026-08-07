package it.gov.pagopa.payment.dto.mapper;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;
import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;
import it.gov.pagopa.payment.dto.qrcode.TransactionCreationRequest;
import it.gov.pagopa.payment.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionMapperTest {

    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionMapper(5, 10, "https://example.com/img", "https://example.com/txt");
    }

    @Test
    void transactionCreationRequestToTransactionInitializesLifecycleRevisionIndependently() {
        TransactionCreationRequest request = TransactionCreationRequest.builder()
                .initiativeId("initiative-id")
                .idTrxAcquirer("acquirer-transaction-id")
                .amountCents(100L)
                .mcc("1234")
                .additionalProperties(Map.of("key", "value"))
                .build();
        MerchantDetailDTO merchant = MerchantDetailDTO.builder()
                .fiscalCode("merchant-fiscal-code")
                .initiativeName("initiative-name")
                .businessName("business-name")
                .vatNumber("vat-number")
                .build();

        Transaction transaction = mapper.transactionCreationRequestToTransaction(
                request, "QR_CODE", "merchant-id", "acquirer-id", merchant, "issuer-transaction-id");

        assertEquals(0L, transaction.getTransactionRevision());
        assertEquals(0L, transaction.getCounterVersion());

        transaction.setCounterVersion(3L);

        assertEquals(0L, transaction.getTransactionRevision());
        assertEquals(3L, transaction.getCounterVersion());
    }

    @Test
    void transactionBarCodeCreationRequestToTransactionInitializesLifecycleRevision() {
        TransactionBarCodeCreationRequest request = TransactionBarCodeCreationRequest.builder()
                .initiativeId("initiative-id")
                .voucherAmountCents(100L)
                .build();

        Transaction transaction = mapper.transactionBarCodeCreationRequestToTransaction(
                request,
                "BARCODE",
                "user-id",
                "initiative-name",
                Map.of("key", "value"),
                true,
                OffsetDateTime.now().plusHours(1));

        assertEquals(0L, transaction.getTransactionRevision());
        assertNull(transaction.getCounterVersion());
    }
}
