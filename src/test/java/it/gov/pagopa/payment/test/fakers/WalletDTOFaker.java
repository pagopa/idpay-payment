package it.gov.pagopa.payment.test.fakers;

import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;

public class WalletDTOFaker {

    public static WalletDTO mockInstance(Integer bias, String walletStatus) { return mockInstanceBuilder(bias, walletStatus).build(); }

    public static WalletDTO.WalletDTOBuilder mockInstanceBuilder (Integer bias, String walletStatus){
        return WalletDTO.builder()
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .initiativeName("INITIATIVENAME%d".formatted(bias))
                .familyId("FAMILYID%d".formatted(bias))
                .status(walletStatus);
    }

}
