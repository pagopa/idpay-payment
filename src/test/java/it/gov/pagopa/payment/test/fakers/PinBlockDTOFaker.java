package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.dto.PinBlockDTO;

public class PinBlockDTOFaker {

    public static PinBlockDTO mockInstance() {
        return mockInstanceBuilder().build();
    }

    public static PinBlockDTO.PinBlockDTOBuilder mockInstanceBuilder() {

        return PinBlockDTO.builder()
                .pinBlock("1234567890123456")
                .encryptedKey("xh2ja");
    }
}
