package it.gov.pagopa.payment.configuration;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

  @Bean
  public BlobContainerClient fileStorageClientConfiguration(
    @Value("${blobStorage.storageAccountName}") String storageAccountName,
    @Value("${blobStorage.containerReference}") String containerName) {

    return new BlobServiceClientBuilder()
        .credential(new DefaultAzureCredentialBuilder().build())
        .endpoint("https://" + storageAccountName + ".blob.core.windows.net")
        .buildClient()
        .getBlobContainerClient(containerName);
  }
}
