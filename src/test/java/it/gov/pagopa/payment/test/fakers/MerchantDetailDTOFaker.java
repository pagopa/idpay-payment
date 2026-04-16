package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.merchant.dto.MerchantDetailDTO;

public class MerchantDetailDTOFaker {
    public static MerchantDetailDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static MerchantDetailDTO.MerchantDetailDTOBuilder mockInstanceBuilder(Integer bias) {
        return MerchantDetailDTO.builder()
                .initiativeName("INITIATIVENAME%d".formatted(bias))
                .businessName("BUSINESSNAME%d".formatted(bias))
                .fiscalCode("FISCALCODE%d".formatted(bias))
                .vatNumber("VAT%d".formatted(bias));
    }
}
