package it.gov.pagopa.payment.connector.rest.wallet;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.custom.InternalServerErrorException;
import it.gov.pagopa.common.web.exception.custom.NotFoundException;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
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
                throw new NotFoundException("WALLET",
                        String.format("A wallet related to the user %s with initiativeId %s was not found.", userId, initiativeId));
            }

            throw new InternalServerErrorException("INTERNAL SERVER ERROR",
                    "An error occurred in the microservice wallet", false, e);
        }

        return walletDTO;
    }

}
