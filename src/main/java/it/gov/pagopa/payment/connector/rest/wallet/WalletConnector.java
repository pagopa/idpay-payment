package it.gov.pagopa.payment.connector.rest.wallet;

import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;

public interface WalletConnector {

    WalletDTO getWallet(String initiativeId, String userId);

}
