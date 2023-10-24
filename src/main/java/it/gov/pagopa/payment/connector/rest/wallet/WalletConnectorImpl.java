package it.gov.pagopa.payment.connector.rest.wallet;

import feign.FeignException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.payment.connector.rest.wallet.dto.WalletDTO;
import it.gov.pagopa.payment.constants.PaymentConstants;
import org.springframework.http.HttpStatus;
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
                throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN,
                        PaymentConstants.ExceptionCode.USER_NOT_ONBOARDED,
                        String.format("The user is not onboarded on initiative [%s].", initiativeId));
            }

            throw new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred in the microservice wallet", e);
        }

        return walletDTO;
    }

}
