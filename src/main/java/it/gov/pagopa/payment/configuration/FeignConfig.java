package it.gov.pagopa.payment.configuration;

import it.gov.pagopa.payment.connector.rest.merchant.MerchantRestClient;
import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {RewardCalculatorRestClient.class, MerchantRestClient.class})
public class FeignConfig {

}
