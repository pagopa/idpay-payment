package it.gov.pagopa.payment.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(AppConfigurationProperties.RewardBatchImpact.class)
class RewardBatchImpactPropertiesTest {

    @Autowired
    private AppConfigurationProperties.RewardBatchImpact properties;

    @Test
    void eligibilityIsDisabledByDefault() {
        assertFalse(properties.getEligibility().isEnabled());
    }
}
