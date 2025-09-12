package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.payment.connector.decrypt.DecryptRest;
import it.gov.pagopa.payment.connector.encrypt.EncryptRest;
import it.gov.pagopa.payment.connector.rest.merchant.MerchantRestClient;
import it.gov.pagopa.payment.connector.rest.paymentinstrument.PaymentInstrumentRestClient;
import it.gov.pagopa.payment.connector.rest.register.RegisterRestClient;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import it.gov.pagopa.payment.connector.rest.wallet.WalletRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {RewardCalculatorRestClient.class,
        MerchantRestClient.class,
        EncryptRest.class,
        DecryptRest.class,
        WalletRestClient.class,
        PaymentInstrumentRestClient.class,
        RegisterRestClient.class})
public class FeignConfig {

}
