package it.gov.pagopa.payment.configuration;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfig {

  private final FileStorageProperties properties;

  public FileStorageConfig(FileStorageProperties properties) {
    this.properties = properties;
  }

  @Bean
  public BlobServiceClient blobServiceClient() {
    return new BlobServiceClientBuilder()
        .endpoint("https://" + properties.getStorageAccountName() + ".blob.core.windows.net")
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }

  @Bean
  public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
    return blobServiceClient.getBlobContainerClient(properties.getContainerReference());
  }
}