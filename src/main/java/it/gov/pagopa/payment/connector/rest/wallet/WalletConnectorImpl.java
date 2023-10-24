package it.gov.pagopa.payment.connector.rest.wallet;

import feign.FeignException;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.exception.custom.forbidden.UserNotOnboardedException;
import it.gov.pagopa.payment.exception.custom.servererror.WalletInvocationException;
import org.springframework.stereotype.Service;

@Service
public class WalletConnectorImpl implements WalletConnector{

    private final WalletRestClient restClient;

    public WalletConnectorImpl (WalletRestClient restClient) { this.restClient = restClient; }

    @Override
    public WalletDTO getWallet (String initiativeId, String userId){
        WalletDTO walletDTO;
        try{
            walletDTO = restClient.getWallet(initiativeId, userId);
        } catch (FeignException e){
            if (e.status() == 404) {
                throw new UserNotOnboardedException(String.format("The user is not onboarded on initiative [%s].", initiativeId));
            }

            throw new WalletInvocationException(
                "An error occurred in the microservice wallet", false, e);
        }

        return walletDTO;
    }

}
