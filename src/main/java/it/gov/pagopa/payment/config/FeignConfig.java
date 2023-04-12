package it.gov.pagopa.payment.config;

import it.gov.pagopa.payment.connector.rest.reward.RewardCalculatorRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {RewardCalculatorRestClient.class})
public class FeignConfig {

}
