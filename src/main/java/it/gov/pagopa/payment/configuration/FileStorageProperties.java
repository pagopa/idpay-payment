package it.gov.pagopa.payment.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "blobstorage")
public class FileStorageProperties {

  private String storageAccountName;
  private String containerReference;
}