package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.barcode.TransactionBarCodeCreationRequest;

public class TransactionBarCodeCreationRequestFaker {

    private TransactionBarCodeCreationRequestFaker() {}

    public static TransactionBarCodeCreationRequest mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionBarCodeCreationRequest.TransactionBarCodeCreationRequestBuilder mockInstanceBuilder(Integer bias) {
        return TransactionBarCodeCreationRequest.builder()
                .initiativeId("INITIATIVEID%d".formatted(bias));
    }


}
